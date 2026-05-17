package com.deepcode.service;

import com.deepcode.model.Submission;
import com.deepcode.model.User;
import com.deepcode.util.Config;
import com.google.gson.*;

import java.net.URI;
import java.net.http.*;
import java.time.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Service to crawl submissions from Codeforces API.
 * API Doc: https://codeforces.com/apiHelp
 */
public class CodeforcesAPI {
    private static final String API_BASE = "https://codeforces.com/api/";
    private final HttpClient httpClient;

    private static final int MAX_RETRIES = 3;
    private static final long[] RETRY_DELAYS_MS = {10000, 30000, 60000}; // 10s, 30s, 60s

    public CodeforcesAPI() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    /**
     * Fetch recent submissions for a Codeforces user.
     * Returns list of Submission objects with metadata.
     * Source code is fetched separately from the contest page.
     */
    public List<Submission> fetchSubmissions(User user, int maxCount) throws Exception {
        List<Submission> submissions = new ArrayList<>();
        
        String url = API_BASE + "user.status?handle=" + user.getUsername() + "&from=1&count=" + maxCount;
        System.out.println("📡 Crawling Codeforces: " + url);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36")
                .header("Accept", "application/json, text/plain, */*")
                .header("Accept-Language", "en-US,en;q=0.9")
                .GET()
                .build();

        HttpResponse<String> response = null;
        for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                break;
            } else if (response.statusCode() == 403 && attempt < MAX_RETRIES) {
                long delay = RETRY_DELAYS_MS[attempt];
                System.out.println("⏳ Codeforces tra ve 403. Doi " + (delay/1000) + "s roi thu lai (lan " + (attempt+2) + "/" + (MAX_RETRIES+1) + ")...");
                Thread.sleep(delay);
            } else if (response.statusCode() != 200) {
                throw new RuntimeException("Codeforces API error: HTTP " + response.statusCode() + " (da thu " + (attempt+1) + " lan)");
            }
        }

        if (response == null || response.statusCode() != 200) {
            throw new RuntimeException("Codeforces API error: HTTP " + (response != null ? response.statusCode() : "null") + " - IP bi chan tam thoi. Vui long doi 5-10 phut.");
        }

        JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
        String status = json.get("status").getAsString();
        
        if (!"OK".equals(status)) {
            String comment = json.has("comment") ? json.get("comment").getAsString() : "Unknown error";
            throw new RuntimeException("Codeforces API error: " + comment);
        }

        JsonArray results = json.getAsJsonArray("result");
        
        for (JsonElement elem : results) {
            try {
                JsonObject sub = elem.getAsJsonObject();
                Submission submission = new Submission();
                submission.setUserId(user.getId());
                submission.setSubmissionId(String.valueOf(sub.get("id").getAsLong()));
                
                // Problem info
                JsonObject problem = sub.getAsJsonObject("problem");
                String contestId = problem.has("contestId") ? problem.get("contestId").getAsString() : "";
                String index = problem.has("index") ? problem.get("index").getAsString() : "";
                String problemName = problem.has("name") ? problem.get("name").getAsString() : "Unknown";
                
                submission.setProblemId(contestId + index);
                submission.setProblemName(problemName);
                submission.setVerdict(sub.has("verdict") ? sub.get("verdict").getAsString() : "TESTING");

                // Programming language
                submission.setLanguage(sub.has("programmingLanguage") ? sub.get("programmingLanguage").getAsString() : "Unknown");
                
                // Submission time
                long creationTime = sub.get("creationTimeSeconds").getAsLong();
                submission.setSubmissionTime(
                    LocalDateTime.ofInstant(Instant.ofEpochSecond(creationTime), ZoneId.systemDefault())
                );

                submissions.add(submission);
            } catch (Exception e) {
                System.err.println("⚠ Error parsing submission: " + e.getMessage());
            }
        }

        System.out.println("✅ Fetched " + submissions.size() + " submissions from Codeforces for " + user.getUsername());
        return submissions;
    }

    /**
     * Fetch source code for a Codeforces submission.
     * Uses the contest submission page to get the source code.
     */
    public String fetchSourceCode(String submissionId, String problemId) {
        try {
            // Extract contest ID from problem ID (e.g., "1234A" -> "1234")
            String contestId = problemId.replaceAll("[^0-9]", "");
            if (contestId.isEmpty()) return null;

            String url = "https://codeforces.com/contest/" + contestId + "/submission/" + submissionId;
            System.out.println("📄 Fetching source code: " + url);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                String body = response.body();
                // Look for source code in the HTML
                // The source code is usually in a <pre> tag with id "program-source-text"
                int startIdx = body.indexOf("id=\"program-source-text\"");
                if (startIdx == -1) {
                    // Try alternative format
                    startIdx = body.indexOf("source-code");
                }
                
                if (startIdx != -1) {
                    // Find the next <pre> tag content
                    int preStart = body.indexOf(">", startIdx);
                    if (preStart != -1) {
                        preStart += 1;
                        int preEnd = body.indexOf("</pre>", preStart);
                        if (preEnd != -1) {
                            String code = body.substring(preStart, preEnd);
                            // Decode HTML entities
                            code = code.replace("&lt;", "<")
                                       .replace("&gt;", ">")
                                       .replace("&amp;", "&")
                                       .replace("&quot;", "\"")
                                       .replace("&#39;", "'")
                                       .replace("&nbsp;", " ");
                            return code.trim();
                        }
                    }
                }
            }
            
            // Rate limiting - wait longer to avoid 403
            Thread.sleep(2000);
            
        } catch (Exception e) {
            System.err.println("⚠ Error fetching source code for submission " + submissionId + ": " + e.getMessage());
        }
        return null;
    }

    /**
     * Validate if a Codeforces handle exists.
     */
    public boolean validateHandle(String handle) {
        try {
            String url = API_BASE + "user.info?handles=" + handle;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36")
                    .header("Accept", "application/json, text/plain, */*")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                return "OK".equals(json.get("status").getAsString());
            }
        } catch (Exception e) {
            System.err.println("Error validating handle: " + e.getMessage());
        }
        return false;
    }
}
