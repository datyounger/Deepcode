package com.deepcode;

import com.deepcode.dao.DatabaseManager;
import java.sql.*;

public class CheckDB {
    public static void main(String[] args) {
        try {
            DatabaseManager.getInstance().initialize();
            Connection conn = DatabaseManager.getInstance().getConnection();
            
            System.out.println("--- USERS ---");
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT id, username FROM users")) {
                while (rs.next()) {
                    System.out.println("User ID: " + rs.getInt("id") + ", Username: " + rs.getString("username"));
                }
            }

            System.out.println("\n--- SUBMISSIONS (Top 10) ---");
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT id, user_id, submission_id, problem_name, (source_code IS NOT NULL AND source_code != '') AS has_code FROM submissions LIMIT 10")) {
                while (rs.next()) {
                    System.out.println("ID: " + rs.getInt("id") + ", UserID: " + rs.getInt("user_id") + ", Prob: " + rs.getString("problem_name") + ", HasCode: " + rs.getInt("has_code"));
                }
            }

            System.out.println("\n--- ANALYSIS RESULTS COUNT ---");
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM analysis_results")) {
                if (rs.next()) System.out.println("Analysis Results: " + rs.getInt(1));
            }

            DatabaseManager.getInstance().close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
