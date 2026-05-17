package com.deepcode.dao;

import com.deepcode.model.Submission;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Submission entity.
 */
public class SubmissionDAO {
    private final Connection conn;

    public SubmissionDAO() {
        this.conn = DatabaseManager.getInstance().getConnection();
    }

    public int addSubmission(Submission submission) throws SQLException {
        String sql = """
            INSERT OR IGNORE INTO submissions 
            (user_id, submission_id, problem_name, problem_id, language, verdict, source_code, submission_time, crawled_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, submission.getUserId());
            ps.setString(2, submission.getSubmissionId());
            ps.setString(3, submission.getProblemName());
            ps.setString(4, submission.getProblemId());
            ps.setString(5, submission.getLanguage());
            ps.setString(6, submission.getVerdict());
            ps.setString(7, submission.getSourceCode());
            ps.setString(8, submission.getSubmissionTime() != null ? submission.getSubmissionTime().toString() : null);
            ps.setString(9, LocalDateTime.now().toString());
            
            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        int id = rs.getInt(1);
                        submission.setId(id);
                        return id;
                    }
                }
            }
        }
        return -1;
    }

    public List<Submission> getSubmissionsByUserId(int userId) throws SQLException {
        List<Submission> submissions = new ArrayList<>();
        String sql = "SELECT s.*, u.username, u.platform FROM submissions s JOIN users u ON s.user_id = u.id WHERE s.user_id = ? ORDER BY s.submission_time DESC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    submissions.add(mapSubmission(rs));
                }
            }
        }
        return submissions;
    }

    public List<Submission> getUnanalyzedSubmissions(int userId) throws SQLException {
        List<Submission> submissions = new ArrayList<>();
        String sql = """
            SELECT s.*, u.username, u.platform FROM submissions s 
            JOIN users u ON s.user_id = u.id
            WHERE s.user_id = ? AND s.source_code IS NOT NULL AND s.source_code != ''
            AND s.id NOT IN (SELECT submission_id FROM analysis_results)
            ORDER BY s.submission_time DESC
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    submissions.add(mapSubmission(rs));
                }
            }
        }
        return submissions;
    }

    public List<Submission> getAllUnanalyzedSubmissions() throws SQLException {
        List<Submission> submissions = new ArrayList<>();
        String sql = """
            SELECT s.*, u.username, u.platform FROM submissions s 
            JOIN users u ON s.user_id = u.id
            WHERE s.source_code IS NOT NULL AND s.source_code != ''
            AND s.id NOT IN (SELECT submission_id FROM analysis_results)
            ORDER BY s.submission_time DESC
        """;
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                submissions.add(mapSubmission(rs));
            }
        }
        return submissions;
    }

    public int getSubmissionCount(int userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM submissions WHERE user_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return 0;
    }

    public int getSubmissionWithCodeCount(int userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM submissions WHERE user_id = ? AND source_code IS NOT NULL AND source_code != ''";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return 0;
    }

    public boolean submissionExists(int userId, String submissionId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM submissions WHERE user_id = ? AND submission_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, submissionId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    public Submission getSubmissionById(int id) throws SQLException {
        String sql = "SELECT s.*, u.username, u.platform FROM submissions s JOIN users u ON s.user_id = u.id WHERE s.id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapSubmission(rs);
            }
        }
        return null;
    }

    private Submission mapSubmission(ResultSet rs) throws SQLException {
        Submission s = new Submission();
        s.setId(rs.getInt("id"));
        s.setUserId(rs.getInt("user_id"));
        s.setSubmissionId(rs.getString("submission_id"));
        s.setProblemName(rs.getString("problem_name"));
        s.setProblemId(rs.getString("problem_id"));
        s.setLanguage(rs.getString("language"));
        s.setVerdict(rs.getString("verdict"));
        s.setSourceCode(rs.getString("source_code"));

        String subTime = rs.getString("submission_time");
        if (subTime != null && !subTime.isEmpty()) {
            try { s.setSubmissionTime(LocalDateTime.parse(subTime)); } catch (Exception ignored) {}
        }

        String crawledAt = rs.getString("crawled_at");
        if (crawledAt != null && !crawledAt.isEmpty()) {
            try { s.setCrawledAt(LocalDateTime.parse(crawledAt)); } catch (Exception ignored) {}
        }

        try {
            s.setUsername(rs.getString("username"));
            s.setPlatform(rs.getString("platform"));
        } catch (SQLException ignored) {}

        return s;
    }
}
