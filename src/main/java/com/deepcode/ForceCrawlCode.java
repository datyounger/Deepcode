package com.deepcode;

import com.deepcode.dao.DatabaseManager;
import com.deepcode.dao.SubmissionDAO;
import com.deepcode.dao.UserDAO;
import com.deepcode.model.Submission;
import com.deepcode.model.User;
import java.util.List;

public class ForceCrawlCode {
    public static void main(String[] args) {
        try {
            System.out.println("🚀 Đang khởi tạo Database...");
            DatabaseManager.getInstance().initialize();

            UserDAO userDAO = new UserDAO();
            SubmissionDAO submissionDAO = new SubmissionDAO();
            com.deepcode.service.CodeforcesAPI cfApi = new com.deepcode.service.CodeforcesAPI();

            List<User> users = userDAO.getAllUsers();
            for (User user : users) {
                if (!"codeforces".equals(user.getPlatform())) continue;
                
                System.out.println("\n👤 User: " + user.getUsername());
                List<Submission> subs = submissionDAO.getSubmissionsByUserId(user.getId());
                
                int updated = 0;
                for (Submission s : subs) {
                    if (s.getSourceCode() == null || s.getSourceCode().trim().isEmpty()) {
                        System.out.println("📄 Lấy source cho: " + s.getProblemName() + " (" + s.getSubmissionId() + ")");
                        String code = cfApi.fetchSourceCode(s.getSubmissionId(), s.getProblemId());
                        if (code != null) {
                            // Update manual update logic here since DAO might not have direct updateSourceCode
                            updateCode(s.getId(), code);
                            updated++;
                            Thread.sleep(1000); // Tránh bị block
                        }
                    }
                    if (updated >= 5) break; // Chỉ lấy 5 cái mỗi user để test cho nhanh
                }
                System.out.println("✅ Đã cập nhật " + updated + " submissions.");
            }

            DatabaseManager.getInstance().close();
            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void updateCode(int id, String code) throws Exception {
        String sql = "UPDATE submissions SET source_code = ? WHERE id = ?";
        try (java.sql.PreparedStatement ps = DatabaseManager.getInstance().getConnection().prepareStatement(sql)) {
            ps.setString(1, code);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }
}
