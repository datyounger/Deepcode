package com.deepcode.dao;

import com.deepcode.model.AnalysisResult;
import com.deepcode.model.UserEvaluation;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for AnalysisResult and UserEvaluation entities.
 */
public class AnalysisDAO {
    private final Connection conn;
    private final Gson gson = new Gson();

    public AnalysisDAO() {
        this.conn = DatabaseManager.getInstance().getConnection();
    }

    // ==================== Analysis Results ====================

    public int addAnalysisResult(AnalysisResult result) throws SQLException {
        String sql = """
            INSERT INTO analysis_results 
            (submission_id, algorithms, data_structures, ai_probability, ai_indicators, complexity, analysis_summary, analyzed_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, result.getSubmissionId());
            ps.setString(2, gson.toJson(result.getAlgorithms()));
            ps.setString(3, gson.toJson(result.getDataStructures()));
            ps.setDouble(4, result.getAiProbability());
            ps.setString(5, result.getAiIndicators());
            ps.setString(6, result.getComplexity());
            ps.setString(7, result.getAnalysisSummary());
            ps.setString(8, LocalDateTime.now().toString());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    result.setId(id);
                    return id;
                }
            }
        }
        return -1;
    }

    public AnalysisResult getAnalysisForSubmission(int submissionId) throws SQLException {
        String sql = "SELECT * FROM analysis_results WHERE submission_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, submissionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapAnalysisResult(rs);
            }
        }
        return null;
    }

    public List<AnalysisResult> getAnalysisResultsByUserId(int userId) throws SQLException {
        List<AnalysisResult> results = new ArrayList<>();
        String sql = """
            SELECT ar.*, s.problem_name, s.language FROM analysis_results ar
            JOIN submissions s ON ar.submission_id = s.id
            WHERE s.user_id = ?
            ORDER BY ar.analyzed_at DESC
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    AnalysisResult ar = mapAnalysisResult(rs);
                    try {
                        ar.setProblemName(rs.getString("problem_name"));
                        ar.setLanguage(rs.getString("language"));
                    } catch (SQLException ignored) {}
                    results.add(ar);
                }
            }
        }
        return results;
    }

    public int getAnalyzedCount(int userId) throws SQLException {
        String sql = """
            SELECT COUNT(*) FROM analysis_results ar
            JOIN submissions s ON ar.submission_id = s.id
            WHERE s.user_id = ?
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return 0;
    }

    private AnalysisResult mapAnalysisResult(ResultSet rs) throws SQLException {
        AnalysisResult r = new AnalysisResult();
        r.setId(rs.getInt("id"));
        r.setSubmissionId(rs.getInt("submission_id"));
        r.setAiProbability(rs.getDouble("ai_probability"));
        r.setAiIndicators(rs.getString("ai_indicators"));
        r.setComplexity(rs.getString("complexity"));
        r.setAnalysisSummary(rs.getString("analysis_summary"));

        String algos = rs.getString("algorithms");
        if (algos != null) {
            r.setAlgorithms(gson.fromJson(algos, new TypeToken<List<String>>(){}.getType()));
        } else {
            r.setAlgorithms(new ArrayList<>());
        }

        String ds = rs.getString("data_structures");
        if (ds != null) {
            r.setDataStructures(gson.fromJson(ds, new TypeToken<List<String>>(){}.getType()));
        } else {
            r.setDataStructures(new ArrayList<>());
        }

        String analyzedAt = rs.getString("analyzed_at");
        if (analyzedAt != null) {
            try { r.setAnalyzedAt(LocalDateTime.parse(analyzedAt)); } catch (Exception ignored) {}
        }

        return r;
    }

    // ==================== User Evaluations ====================

    public int saveEvaluation(UserEvaluation eval) throws SQLException {
        // Delete old evaluation for this user
        String delSql = "DELETE FROM user_evaluations WHERE user_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(delSql)) {
            ps.setInt(1, eval.getUserId());
            ps.executeUpdate();
        }

        String sql = """
            INSERT INTO user_evaluations 
            (user_id, ds_score, algo_score, ai_usage_rate, total_submissions, analyzed_submissions, evaluation_summary, evaluated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, eval.getUserId());
            ps.setDouble(2, eval.getDsScore());
            ps.setDouble(3, eval.getAlgoScore());
            ps.setDouble(4, eval.getAiUsageRate());
            ps.setInt(5, eval.getTotalSubmissions());
            ps.setInt(6, eval.getAnalyzedSubmissions());
            ps.setString(7, eval.getEvaluationSummary());
            ps.setString(8, LocalDateTime.now().toString());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return -1;
    }

    public UserEvaluation getEvaluation(int userId) throws SQLException {
        String sql = """
            SELECT ue.*, u.username, u.platform FROM user_evaluations ue
            JOIN users u ON ue.user_id = u.id
            WHERE ue.user_id = ? ORDER BY ue.evaluated_at DESC LIMIT 1
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapEvaluation(rs);
            }
        }
        return null;
    }

    public List<UserEvaluation> getAllEvaluations() throws SQLException {
        List<UserEvaluation> evals = new ArrayList<>();
        String sql = """
            SELECT ue.*, u.username, u.platform FROM user_evaluations ue
            JOIN users u ON ue.user_id = u.id
            ORDER BY ue.evaluated_at DESC
        """;
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                evals.add(mapEvaluation(rs));
            }
        }
        return evals;
    }

    private UserEvaluation mapEvaluation(ResultSet rs) throws SQLException {
        UserEvaluation e = new UserEvaluation();
        e.setId(rs.getInt("id"));
        e.setUserId(rs.getInt("user_id"));
        e.setDsScore(rs.getDouble("ds_score"));
        e.setAlgoScore(rs.getDouble("algo_score"));
        e.setAiUsageRate(rs.getDouble("ai_usage_rate"));
        e.setTotalSubmissions(rs.getInt("total_submissions"));
        e.setAnalyzedSubmissions(rs.getInt("analyzed_submissions"));
        e.setEvaluationSummary(rs.getString("evaluation_summary"));

        try {
            e.setUsername(rs.getString("username"));
            e.setPlatform(rs.getString("platform"));
        } catch (SQLException ignored) {}

        String evaluatedAt = rs.getString("evaluated_at");
        if (evaluatedAt != null) {
            try { e.setEvaluatedAt(LocalDateTime.parse(evaluatedAt)); } catch (Exception ignored) {}
        }

        return e;
    }
}
