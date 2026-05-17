package com.deepcode.service;

import com.deepcode.model.Submission;
import com.deepcode.model.User;
import com.google.gson.*;

import java.net.URI;
import java.net.http.*;
import java.time.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Service to crawl submissions from VJudge.
 * VJudge provides submission data via its API endpoints.
 */
public class VJudgeAPI {
    private static final String API_BASE = "https://vjudge.net/";
    private final HttpClient httpClient;

    public VJudgeAPI() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    /**
     * Fetch recent submissions for a VJudge user.
     */
    public List<Submission> fetchSubmissions(User user, int maxCount) throws Exception {
        List<Submission> submissions = new ArrayList<>();
        
        // VJudge uses a different API endpoint
        String url = API_BASE + "status/data/?un=" + user.getUsername() + "&num=" + maxCount;
        System.out.println("📡 Crawling VJudge: " + url);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 200) {
            throw new RuntimeException("VJudge API error: HTTP " + response.statusCode());
        }

        String body = response.body();
        JsonObject json = JsonParser.parseString(body).getAsJsonObject();
        
        if (!json.has("data")) {
            System.out.println("⚠ No submissions found for VJudge user: " + user.getUsername());
            return submissions;
        }

        JsonArray data = json.getAsJsonArray("data");
        
        for (JsonElement elem : data) {
            try {
                JsonArray row = elem.getAsJsonArray();
                // VJudge data format: [runId, username, OJ, probNum, result, time, memory, length, language, submitTime, ...]
                if (row.size() < 10) continue;

                Submission submission = new Submission();
                submission.setUserId(user.getId());
                submission.setSubmissionId(String.valueOf(row.get(0).getAsLong()));

                String oj = row.get(2).getAsString();       // Original judge (CF, SPOJ, etc.)
                String probNum = row.get(3).getAsString();   // Problem number
                int result = row.get(4).getAsInt();           // Result code
                String language = row.get(8).getAsString();   // Language

                submission.setProblemId(oj + "-" + probNum);
                submission.setProblemName(oj + " " + probNum);
                submission.setLanguage(language);
                
                // VJudge result codes
                submission.setVerdict(mapVJudgeVerdict(result));

                // Submit time (Unix timestamp in ms)
                long submitTime = row.get(9).getAsLong();
                submission.setSubmissionTime(
                    LocalDateTime.ofInstant(Instant.ofEpochMilli(submitTime), ZoneId.systemDefault())
                );

                submissions.add(submission);
            } catch (Exception e) {
                System.err.println("⚠ Error parsing VJudge submission: " + e.getMessage());
            }
        }

        System.out.println("✅ Fetched " + submissions.size() + " submissions from VJudge for " + user.getUsername());
        return submissions;
    }

    /**
     * Fetch source code for a VJudge submission.
     */
    public String fetchSourceCode(String submissionId) {
        try {
            String url = API_BASE + "solution/data/" + submissionId;
            System.out.println("📄 Fetching VJudge source code: " + url);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                String body = response.body();
                JsonObject json = JsonParser.parseString(body).getAsJsonObject();
                if (json.has("code")) {
                    return json.get("code").getAsString();
                }
            }
            
            Thread.sleep(300); // Rate limiting
        } catch (Exception e) {
            System.err.println("⚠ Error fetching VJudge source: " + e.getMessage());
        }
        return null;
    }

    /**
     * Validate if a VJudge username exists.
     */
    public boolean validateUsername(String username) {
        try {
            String url = API_BASE + "user/checkUsername?username=" + username;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "Mozilla/5.0")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            // If username exists, the endpoint returns specific response
            return response.statusCode() == 200;
        } catch (Exception e) {
            System.err.println("Error validating VJudge username: " + e.getMessage());
        }
        return false;
    }

    private String mapVJudgeVerdict(int code) {
        return switch (code) {
            case 0 -> "OK";
            case 1 -> "WRONG_ANSWER";
            case 2 -> "TIME_LIMIT_EXCEEDED";
            case 3 -> "MEMORY_LIMIT_EXCEEDED";
            case 4 -> "OUTPUT_LIMIT_EXCEEDED";
            case 5 -> "RUNTIME_ERROR";
            case 6 -> "COMPILATION_ERROR";
            case 7 -> "PRESENTATION_ERROR";
            default -> "UNKNOWN";
        };
    }
}
