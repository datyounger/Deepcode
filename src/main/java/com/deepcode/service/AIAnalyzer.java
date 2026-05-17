package com.deepcode.service;

import com.deepcode.dao.AnalysisDAO;
import com.deepcode.dao.SubmissionDAO;
import com.deepcode.model.*;
import com.deepcode.util.Config;
import com.google.gson.*;

import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
import java.util.function.Consumer;

/**
 * AI-powered code analyzer using Google Gemini API.
 * Analyzes source code for:
 * - Data structures used
 * - Algorithms used
 * - AI-generated code probability
 * - Code complexity
 */
public class AIAnalyzer {
    private final HttpClient httpClient;
    private final AnalysisDAO analysisDAO;
    private final SubmissionDAO submissionDAO;
    private final Gson gson = new Gson();

    public AIAnalyzer() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(60))
                .build();
        this.analysisDAO = new AnalysisDAO();
        this.submissionDAO = new SubmissionDAO();
    }

    /**
     * Analyze a single submission using Gemini AI.
     */
    public AnalysisResult analyzeSubmission(Submission submission) throws Exception {
        String apiKey = Config.getInstance().getGeminiApiKey();
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new RuntimeException("Gemini API key chưa được cấu hình! Vào Cài đặt để nhập API key.");
        }

        String sourceCode = submission.getSourceCode();
        if (sourceCode == null || sourceCode.trim().isEmpty()) {
            throw new RuntimeException("Submission không có source code.");
        }

        // Truncate very long code to stay within token limits
        if (sourceCode.length() > 8000) {
            sourceCode = sourceCode.substring(0, 8000) + "\n// ... (truncated)";
        }

        String prompt = buildAnalysisPrompt(sourceCode, submission.getLanguage(), submission.getProblemName());
        String response = callGeminiAPI(prompt, apiKey);
        
        AnalysisResult result = parseAIResponse(response);
        result.setSubmissionId(submission.getId());

        // Save to database
        analysisDAO.addAnalysisResult(result);
        
        return result;
    }

    /**
     * Analyze all unanalyzed submissions for a user.
     */
    public int analyzeUserSubmissions(User user, Consumer<String> progressCallback) throws Exception {
        List<Submission> unanalyzed = submissionDAO.getUnanalyzedSubmissions(user.getId());
        int analyzed = 0;

        progressCallback.accept("🔬 Bắt đầu phân tích " + unanalyzed.size() + " submissions cho " + user.getDisplayName());

        for (int i = 0; i < unanalyzed.size(); i++) {
            Submission sub = unanalyzed.get(i);
            progressCallback.accept("🤖 [" + (i + 1) + "/" + unanalyzed.size() + "] Phân tích: " + sub.getProblemName());
            
            try {
                analyzeSubmission(sub);
                analyzed++;
                
                // Rate limiting for Gemini API (15 req/min for free tier)
                Thread.sleep(4500); // ~13 requests per minute to be safe
                
            } catch (Exception e) {
                progressCallback.accept("⚠ Lỗi phân tích " + sub.getProblemName() + ": " + e.getMessage());
                Thread.sleep(2000);
            }
        }

        progressCallback.accept("✅ Phân tích xong " + analyzed + "/" + unanalyzed.size() + " submissions.");
        return analyzed;
    }

    /**
     * Generate user evaluation based on analysis results.
     */
    public UserEvaluation evaluateUser(User user) throws Exception {
        List<AnalysisResult> results = analysisDAO.getAnalysisResultsByUserId(user.getId());
        int totalSubs = submissionDAO.getSubmissionCount(user.getId());
        
        if (results.isEmpty()) {
            throw new RuntimeException("Chưa có kết quả phân tích nào cho user " + user.getUsername());
        }

        UserEvaluation eval = new UserEvaluation();
        eval.setUserId(user.getId());
        eval.setTotalSubmissions(totalSubs);
        eval.setAnalyzedSubmissions(results.size());

        // Calculate Data Structure score
        Map<String, Integer> dsFrequency = new HashMap<>();
        Map<String, Integer> algoFrequency = new HashMap<>();
        double totalAiProb = 0;

        for (AnalysisResult r : results) {
            if (r.getDataStructures() != null) {
                for (String ds : r.getDataStructures()) {
                    dsFrequency.merge(ds, 1, Integer::sum);
                }
            }
            if (r.getAlgorithms() != null) {
                for (String algo : r.getAlgorithms()) {
                    algoFrequency.merge(algo, 1, Integer::sum);
                }
            }
            totalAiProb += r.getAiProbability();
        }

        // DS Score: based on variety and count
        double dsScore = calculateDSScore(dsFrequency, results.size());
        double algoScore = calculateAlgoScore(algoFrequency, results.size());
        double aiUsageRate = (totalAiProb / results.size()) * 100;

        eval.setDsScore(dsScore);
        eval.setAlgoScore(algoScore);
        eval.setAiUsageRate(aiUsageRate);

        // Generate summary
        StringBuilder summary = new StringBuilder();
        summary.append("📊 Đánh giá ").append(user.getUsername()).append(" (").append(user.getPlatform()).append(")\n\n");
        summary.append("📈 Tổng submissions: ").append(totalSubs).append("\n");
        summary.append("🔬 Đã phân tích: ").append(results.size()).append("\n\n");
        
        summary.append("🏗️ CTDL đã sử dụng:\n");
        dsFrequency.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .forEach(e -> summary.append("  • ").append(e.getKey()).append(": ").append(e.getValue()).append(" lần\n"));
        
        summary.append("\n⚡ Thuật toán đã sử dụng:\n");
        algoFrequency.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .forEach(e -> summary.append("  • ").append(e.getKey()).append(": ").append(e.getValue()).append(" lần\n"));
        
        summary.append("\n🎯 Điểm CTDL: ").append(String.format("%.1f", dsScore)).append("/100\n");
        summary.append("🎯 Điểm Thuật toán: ").append(String.format("%.1f", algoScore)).append("/100\n");
        summary.append("🤖 Mức sử dụng AI: ").append(String.format("%.1f", aiUsageRate)).append("%\n");

        eval.setEvaluationSummary(summary.toString());

        // Save evaluation
        analysisDAO.saveEvaluation(eval);

        return eval;
    }

    private double calculateDSScore(Map<String, Integer> dsFrequency, int totalAnalyzed) {
        if (dsFrequency.isEmpty()) return 0;
        
        // Define DS categories and weights
        Map<String, Double> dsWeights = new HashMap<>();
        dsWeights.put("Array", 1.0);
        dsWeights.put("String", 1.0);
        dsWeights.put("Stack", 2.0);
        dsWeights.put("Queue", 2.0);
        dsWeights.put("LinkedList", 2.0);
        dsWeights.put("HashMap", 2.5);
        dsWeights.put("HashSet", 2.5);
        dsWeights.put("TreeMap", 3.0);
        dsWeights.put("TreeSet", 3.0);
        dsWeights.put("Priority Queue", 3.0);
        dsWeights.put("Heap", 3.0);
        dsWeights.put("Graph", 4.0);
        dsWeights.put("Tree", 3.5);
        dsWeights.put("Binary Tree", 3.5);
        dsWeights.put("Trie", 4.0);
        dsWeights.put("Segment Tree", 5.0);
        dsWeights.put("Fenwick Tree", 5.0);
        dsWeights.put("Disjoint Set", 4.5);
        dsWeights.put("Sparse Table", 5.0);

        double weightedScore = 0;
        double maxPossible = 0;
        
        for (Map.Entry<String, Integer> entry : dsFrequency.entrySet()) {
            double weight = dsWeights.getOrDefault(entry.getKey(), 2.0);
            weightedScore += weight * Math.min(entry.getValue(), 5); // Cap at 5 uses each
        }
        
        // Variety bonus
        double varietyBonus = Math.min(dsFrequency.size() * 5, 30);
        
        // Scale to 0-100
        double rawScore = (weightedScore / Math.max(totalAnalyzed, 1)) * 20 + varietyBonus;
        return Math.min(100, rawScore);
    }

    private double calculateAlgoScore(Map<String, Integer> algoFrequency, int totalAnalyzed) {
        if (algoFrequency.isEmpty()) return 0;

        Map<String, Double> algoWeights = new HashMap<>();
        algoWeights.put("Brute Force", 1.0);
        algoWeights.put("Sorting", 1.5);
        algoWeights.put("Greedy", 2.0);
        algoWeights.put("Binary Search", 2.5);
        algoWeights.put("Two Pointers", 2.5);
        algoWeights.put("Sliding Window", 3.0);
        algoWeights.put("BFS", 3.0);
        algoWeights.put("DFS", 3.0);
        algoWeights.put("Dynamic Programming", 4.0);
        algoWeights.put("Divide and Conquer", 3.5);
        algoWeights.put("Backtracking", 3.5);
        algoWeights.put("Dijkstra", 4.0);
        algoWeights.put("Floyd-Warshall", 4.0);
        algoWeights.put("Bellman-Ford", 4.0);
        algoWeights.put("Kruskal", 4.0);
        algoWeights.put("Topological Sort", 3.5);
        algoWeights.put("KMP", 4.5);
        algoWeights.put("Network Flow", 5.0);
        algoWeights.put("Convex Hull", 5.0);
        algoWeights.put("FFT", 5.0);
        algoWeights.put("Math", 2.0);
        algoWeights.put("Number Theory", 3.5);
        algoWeights.put("Combinatorics", 3.5);

        double weightedScore = 0;
        
        for (Map.Entry<String, Integer> entry : algoFrequency.entrySet()) {
            double weight = algoWeights.getOrDefault(entry.getKey(), 2.0);
            weightedScore += weight * Math.min(entry.getValue(), 5);
        }
        
        double varietyBonus = Math.min(algoFrequency.size() * 5, 30);
        double rawScore = (weightedScore / Math.max(totalAnalyzed, 1)) * 20 + varietyBonus;
        return Math.min(100, rawScore);
    }

    private String buildAnalysisPrompt(String sourceCode, String language, String problemName) {
        return """
            Analyze the following competitive programming code submission. 
            Problem: %s
            Language: %s
            
            Source Code:
            ```
            %s
            ```
            
            Please provide your analysis in the following JSON format (and ONLY JSON, no other text):
            {
                "algorithms": ["list of algorithms used, e.g., Dynamic Programming, BFS, Binary Search, Greedy, etc."],
                "data_structures": ["list of data structures used, e.g., Array, Stack, Queue, HashMap, Graph, Segment Tree, etc."],
                "ai_probability": 0.0 to 1.0 (probability this code was generated by AI),
                "ai_indicators": "explanation of AI indicators found or not found",
                "complexity": "Time: O(...), Space: O(...)",
                "summary": "Brief summary of what the code does and the approach used (in Vietnamese)"
            }
            
            For ai_probability, consider these factors:
            - Overly verbose/explanatory variable names → higher AI probability
            - Perfect code structure with no debugging artifacts → higher AI probability  
            - Unusual comment patterns or explanatory comments → higher AI probability
            - Template-like code structure → higher AI probability
            - Competitive programming shortcuts/macros → lower AI probability
            - Concise, efficient style typical of CP contestants → lower AI probability
            - Minor inconsistencies or personal coding style → lower AI probability
            
            IMPORTANT: Return ONLY valid JSON, no markdown formatting, no code blocks.
            """.formatted(problemName != null ? problemName : "Unknown", 
                         language != null ? language : "Unknown", 
                         sourceCode);
    }

    private String callGeminiAPI(String prompt, String apiKey) throws Exception {
        String model = Config.getInstance().getGeminiModel();
        String apiUrl = Config.getInstance().getGeminiApiUrl() + model + ":generateContent?key=" + apiKey;

        JsonObject requestBody = new JsonObject();
        JsonArray contents = new JsonArray();
        JsonObject content = new JsonObject();
        JsonArray parts = new JsonArray();
        JsonObject part = new JsonObject();
        part.addProperty("text", prompt);
        parts.add(part);
        content.add("parts", parts);
        contents.add(content);
        requestBody.add("contents", contents);

        // Generation config
        JsonObject genConfig = new JsonObject();
        genConfig.addProperty("temperature", 0.3);
        genConfig.addProperty("maxOutputTokens", 2048);
        requestBody.add("generationConfig", genConfig);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(requestBody)))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            String errorMsg = "Gemini API error: HTTP " + response.statusCode();
            try {
                JsonObject errorJson = JsonParser.parseString(response.body()).getAsJsonObject();
                if (errorJson.has("error")) {
                    errorMsg += " - " + errorJson.getAsJsonObject("error").get("message").getAsString();
                }
            } catch (Exception ignored) {}
            throw new RuntimeException(errorMsg);
        }

        // Parse Gemini response
        JsonObject responseJson = JsonParser.parseString(response.body()).getAsJsonObject();
        JsonArray candidates = responseJson.getAsJsonArray("candidates");
        if (candidates != null && candidates.size() > 0) {
            JsonObject candidate = candidates.get(0).getAsJsonObject();
            JsonObject contentObj = candidate.getAsJsonObject("content");
            JsonArray partsArr = contentObj.getAsJsonArray("parts");
            if (partsArr != null && partsArr.size() > 0) {
                return partsArr.get(0).getAsJsonObject().get("text").getAsString();
            }
        }

        throw new RuntimeException("Empty response from Gemini API");
    }

    private AnalysisResult parseAIResponse(String response) {
        AnalysisResult result = new AnalysisResult();
        
        try {
            // Clean up response - remove markdown code blocks if present
            String cleaned = response.trim();
            if (cleaned.startsWith("```json")) {
                cleaned = cleaned.substring(7);
            } else if (cleaned.startsWith("```")) {
                cleaned = cleaned.substring(3);
            }
            if (cleaned.endsWith("```")) {
                cleaned = cleaned.substring(0, cleaned.length() - 3);
            }
            cleaned = cleaned.trim();

            JsonObject json = JsonParser.parseString(cleaned).getAsJsonObject();

            // Parse algorithms
            if (json.has("algorithms")) {
                List<String> algorithms = new ArrayList<>();
                for (JsonElement e : json.getAsJsonArray("algorithms")) {
                    algorithms.add(e.getAsString());
                }
                result.setAlgorithms(algorithms);
            } else {
                result.setAlgorithms(new ArrayList<>());
            }

            // Parse data structures
            if (json.has("data_structures")) {
                List<String> ds = new ArrayList<>();
                for (JsonElement e : json.getAsJsonArray("data_structures")) {
                    ds.add(e.getAsString());
                }
                result.setDataStructures(ds);
            } else {
                result.setDataStructures(new ArrayList<>());
            }

            // Parse AI probability
            result.setAiProbability(json.has("ai_probability") ? json.get("ai_probability").getAsDouble() : 0.0);
            
            // Parse AI indicators
            result.setAiIndicators(json.has("ai_indicators") ? json.get("ai_indicators").getAsString() : "");
            
            // Parse complexity
            result.setComplexity(json.has("complexity") ? json.get("complexity").getAsString() : "N/A");
            
            // Parse summary
            result.setAnalysisSummary(json.has("summary") ? json.get("summary").getAsString() : "");

        } catch (Exception e) {
            System.err.println("⚠ Error parsing AI response: " + e.getMessage());
            // Fallback: store raw response
            result.setAlgorithms(new ArrayList<>());
            result.setDataStructures(new ArrayList<>());
            result.setAiProbability(0.0);
            result.setAiIndicators("Parse error");
            result.setComplexity("N/A");
            result.setAnalysisSummary("Không thể phân tích response: " + response);
        }

        return result;
    }
}
