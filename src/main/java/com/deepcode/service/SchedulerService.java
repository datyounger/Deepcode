package com.deepcode.service;

import com.deepcode.dao.UserDAO;
import com.deepcode.model.User;
import com.deepcode.util.Config;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.*;

/**
 * Scheduler service that runs periodic crawling tasks.
 * Crawls new submissions at configurable intervals.
 */
public class SchedulerService {
    private ScheduledExecutorService scheduler;
    private final CrawlerService crawlerService;
    private volatile boolean active = false;
    private volatile String lastStatus = "Chưa khởi động";

    public SchedulerService() {
        this.crawlerService = new CrawlerService();
    }

    public void start() {
        if (scheduler != null && !scheduler.isShutdown()) {
            return;
        }

        scheduler = Executors.newScheduledThreadPool(1, r -> {
            Thread t = new Thread(r, "DeepCode-Scheduler");
            t.setDaemon(true);
            return t;
        });

        int intervalHours = Config.getInstance().getCrawlIntervalHours();

        // Schedule periodic crawling
        scheduler.scheduleAtFixedRate(this::runCrawlTask, 
            1, // Initial delay: 1 minute
            intervalHours * 60L, // Period in minutes
            TimeUnit.MINUTES);

        active = true;
        lastStatus = "✅ Scheduler đang chạy (Crawl mỗi " + intervalHours + " giờ)";
        System.out.println(lastStatus);
    }

    public void stop() {
        if (scheduler != null) {
            scheduler.shutdown();
            try {
                scheduler.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
            }
        }
        active = false;
        lastStatus = "⏹ Scheduler đã dừng";
    }

    private void runCrawlTask() {
        try {
            UserDAO userDAO = new UserDAO();
            List<User> users = userDAO.getAllUsers();
            int intervalHours = Config.getInstance().getCrawlIntervalHours();

            for (User user : users) {
                // Check if user needs crawling
                if (user.getLastCrawled() != null) {
                    long hoursSinceLast = ChronoUnit.HOURS.between(user.getLastCrawled(), LocalDateTime.now());
                    if (hoursSinceLast < intervalHours) {
                        continue; // Skip - recently crawled
                    }
                }

                try {
                    lastStatus = "🔄 Đang crawl " + user.getDisplayName() + "...";
                    crawlerService.crawlUser(user, msg -> {
                        lastStatus = msg;
                        System.out.println("[Scheduler] " + msg);
                    });
                } catch (Exception e) {
                    lastStatus = "❌ Lỗi crawl " + user.getUsername() + ": " + e.getMessage();
                    System.err.println("[Scheduler] " + lastStatus);
                }
            }

            lastStatus = "✅ Crawl định kỳ hoàn tất lúc " + LocalDateTime.now().toString().substring(0, 19);
            
        } catch (Exception e) {
            lastStatus = "❌ Scheduler error: " + e.getMessage();
            System.err.println("[Scheduler] " + lastStatus);
        }
    }

    public boolean isActive() {
        return active;
    }

    public String getLastStatus() {
        return lastStatus;
    }

    public CrawlerService getCrawlerService() {
        return crawlerService;
    }
}
