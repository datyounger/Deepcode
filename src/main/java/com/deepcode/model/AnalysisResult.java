package com.deepcode.model;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Represents the AI analysis result for a code submission.
 */
public class AnalysisResult {
    private int id;
    private int submissionId;
    private List<String> algorithms;
    private List<String> dataStructures;
    private double aiProbability;     // 0.0 to 1.0
    private String aiIndicators;
    private String complexity;
    private String analysisSummary;
    private LocalDateTime analyzedAt;

    // For display
    private String problemName;
    private String language;

    public AnalysisResult() {}

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getSubmissionId() { return submissionId; }
    public void setSubmissionId(int submissionId) { this.submissionId = submissionId; }

    public List<String> getAlgorithms() { return algorithms; }
    public void setAlgorithms(List<String> algorithms) { this.algorithms = algorithms; }

    public List<String> getDataStructures() { return dataStructures; }
    public void setDataStructures(List<String> dataStructures) { this.dataStructures = dataStructures; }

    public double getAiProbability() { return aiProbability; }
    public void setAiProbability(double aiProbability) { this.aiProbability = aiProbability; }

    public String getAiIndicators() { return aiIndicators; }
    public void setAiIndicators(String aiIndicators) { this.aiIndicators = aiIndicators; }

    public String getComplexity() { return complexity; }
    public void setComplexity(String complexity) { this.complexity = complexity; }

    public String getAnalysisSummary() { return analysisSummary; }
    public void setAnalysisSummary(String analysisSummary) { this.analysisSummary = analysisSummary; }

    public LocalDateTime getAnalyzedAt() { return analyzedAt; }
    public void setAnalyzedAt(LocalDateTime analyzedAt) { this.analyzedAt = analyzedAt; }

    public String getProblemName() { return problemName; }
    public void setProblemName(String problemName) { this.problemName = problemName; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public String getAlgorithmsDisplay() {
        return algorithms == null || algorithms.isEmpty() ? "N/A" : String.join(", ", algorithms);
    }

    public String getDataStructuresDisplay() {
        return dataStructures == null || dataStructures.isEmpty() ? "N/A" : String.join(", ", dataStructures);
    }

    public String getAiProbabilityDisplay() {
        int percent = (int)(aiProbability * 100);
        if (percent <= 20) return "🟢 " + percent + "% (Rất thấp)";
        if (percent <= 40) return "🟡 " + percent + "% (Thấp)";
        if (percent <= 60) return "🟠 " + percent + "% (Trung bình)";
        if (percent <= 80) return "🔴 " + percent + "% (Cao)";
        return "⛔ " + percent + "% (Rất cao)";
    }
}
