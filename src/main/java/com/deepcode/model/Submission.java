package com.deepcode.model;

import java.time.LocalDateTime;

/**
 * Represents a code submission from Codeforces or VJudge.
 */
public class Submission {
    private int id;
    private int userId;
    private String submissionId;
    private String problemName;
    private String problemId;
    private String language;
    private String verdict;
    private String sourceCode;
    private LocalDateTime submissionTime;
    private LocalDateTime crawledAt;

    // For display purposes
    private String username;
    private String platform;

    public Submission() {}

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getSubmissionId() { return submissionId; }
    public void setSubmissionId(String submissionId) { this.submissionId = submissionId; }

    public String getProblemName() { return problemName; }
    public void setProblemName(String problemName) { this.problemName = problemName; }

    public String getProblemId() { return problemId; }
    public void setProblemId(String problemId) { this.problemId = problemId; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public String getVerdict() { return verdict; }
    public void setVerdict(String verdict) { this.verdict = verdict; }

    public String getSourceCode() { return sourceCode; }
    public void setSourceCode(String sourceCode) { this.sourceCode = sourceCode; }

    public LocalDateTime getSubmissionTime() { return submissionTime; }
    public void setSubmissionTime(LocalDateTime submissionTime) { this.submissionTime = submissionTime; }

    public LocalDateTime getCrawledAt() { return crawledAt; }
    public void setCrawledAt(LocalDateTime crawledAt) { this.crawledAt = crawledAt; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }

    public String getVerdictDisplay() {
        if (verdict == null) return "Unknown";
        return switch (verdict) {
            case "OK" -> "✅ Accepted";
            case "WRONG_ANSWER" -> "❌ Wrong Answer";
            case "TIME_LIMIT_EXCEEDED" -> "⏱ TLE";
            case "MEMORY_LIMIT_EXCEEDED" -> "💾 MLE";
            case "RUNTIME_ERROR" -> "💥 Runtime Error";
            case "COMPILATION_ERROR" -> "🔧 Compilation Error";
            default -> verdict;
        };
    }
}
