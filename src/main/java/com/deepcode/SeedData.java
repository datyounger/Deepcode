package com.deepcode;

import com.deepcode.dao.DatabaseManager;
import com.deepcode.dao.UserDAO;
import com.deepcode.model.User;
import com.deepcode.service.CrawlerService;

public class SeedData {
    public static void main(String[] args) {
        try {
            System.out.println("🚀 Đang khởi tạo Database...");
            DatabaseManager.getInstance().initialize();

            UserDAO userDAO = new UserDAO();
            CrawlerService crawlerService = new CrawlerService();

            String[] testUsers = {"tourist", "Benq", "jiangly"};

            for (String username : testUsers) {
                // Kiểm tra xem user đã tồn tại chưa
                User user = userDAO.getUserByUsernameAndPlatform(username, "codeforces");
                if (user == null) {
                    System.out.println("Thêm user mới: " + username);
                    user = new User(username, "codeforces");
                    int id = userDAO.addUser(user);
                    user.setId(id);
                } else {
                    System.out.println("User " + username + " đã có trong DB.");
                }

                // Crawl data
                System.out.println("Bắt đầu crawl data cho " + username + "...");
                crawlerService.crawlUser(user, System.out::println);
            }

            System.out.println("✅ Hoàn tất việc lấy data!");
            DatabaseManager.getInstance().close();
            System.exit(0);

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
