package com.deepcode.model;

import java.time.LocalDateTime;

/**
 * Represents aggregated evaluation for a user.
 */
public class UserEvaluation {
    private int id;
    private int userId;
    private double dsScore;           // Data structure proficiency (0-100)
    private double algoScore;         // Algorithm proficiency (0-100)
    private double aiUsageRate;       // AI usage percentage (0-100)
    private int totalSubmissions;
    private int analyzedSubmissions;
    private String evaluationSummary;
    private LocalDateTime evaluatedAt;

    // For display
    private String username;
    private String platform;

    public UserEvaluation() {}

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public double getDsScore() { return dsScore; }
    public void setDsScore(double dsScore) { this.dsScore = dsScore; }

    public double getAlgoScore() { return algoScore; }
    public void setAlgoScore(double algoScore) { this.algoScore = algoScore; }

    public double getAiUsageRate() { return aiUsageRate; }
    public void setAiUsageRate(double aiUsageRate) { this.aiUsageRate = aiUsageRate; }

    public int getTotalSubmissions() { return totalSubmissions; }
    public void setTotalSubmissions(int totalSubmissions) { this.totalSubmissions = totalSubmissions; }

    public int getAnalyzedSubmissions() { return analyzedSubmissions; }
    public void setAnalyzedSubmissions(int analyzedSubmissions) { this.analyzedSubmissions = analyzedSubmissions; }

    public String getEvaluationSummary() { return evaluationSummary; }
    public void setEvaluationSummary(String evaluationSummary) { this.evaluationSummary = evaluationSummary; }

    public LocalDateTime getEvaluatedAt() { return evaluatedAt; }
    public void setEvaluatedAt(LocalDateTime evaluatedAt) { this.evaluatedAt = evaluatedAt; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }

    public String getDsLevel() {
        if (dsScore >= 80) return "⭐ Xuất sắc";
        if (dsScore >= 60) return "🟢 Tốt";
        if (dsScore >= 40) return "🟡 Trung bình";
        if (dsScore >= 20) return "🟠 Yếu";
        return "🔴 Rất yếu";
    }

    public String getAlgoLevel() {
        if (algoScore >= 80) return "⭐ Xuất sắc";
        if (algoScore >= 60) return "🟢 Tốt";
        if (algoScore >= 40) return "🟡 Trung bình";
        if (algoScore >= 20) return "🟠 Yếu";
        return "🔴 Rất yếu";
    }

    public String getAiUsageLevel() {
        if (aiUsageRate <= 10) return "🟢 Rất thấp";
        if (aiUsageRate <= 25) return "🟡 Thấp";
        if (aiUsageRate <= 50) return "🟠 Trung bình";
        if (aiUsageRate <= 75) return "🔴 Cao";
        return "⛔ Rất cao";
    }
}
