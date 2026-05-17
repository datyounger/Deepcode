package com.deepcode.service;

import com.deepcode.dao.*;
import com.deepcode.model.*;
import com.deepcode.util.Config;

import java.util.List;
import java.util.function.Consumer;

/**
 * Service that orchestrates crawling from Codeforces and VJudge.
 * Handles deduplication and source code fetching.
 */
public class CrawlerService {
    private final CodeforcesAPI codeforcesAPI;
    private final VJudgeAPI vjudgeAPI;
    private final SubmissionDAO submissionDAO;
    private final UserDAO userDAO;
    private volatile boolean running = false;

    public CrawlerService() {
        this.codeforcesAPI = new CodeforcesAPI();
        this.vjudgeAPI = new VJudgeAPI();
        this.submissionDAO = new SubmissionDAO();
        this.userDAO = new UserDAO();
    }

    /**
     * Crawl submissions for a specific user.
     * @param user The user to crawl
     * @param progressCallback Callback for progress updates
     * @return Number of new submissions crawled
     */
    public int crawlUser(User user, Consumer<String> progressCallback) throws Exception {
        running = true;
        int newCount = 0;
        int maxSubs = Config.getInstance().getMaxSubmissions();

        try {
            progressCallback.accept("🔄 Bắt đầu crawl " + user.getDisplayName() + "...");

            List<Submission> submissions;
            
            if ("codeforces".equals(user.getPlatform())) {
                submissions = codeforcesAPI.fetchSubmissions(user, maxSubs);
            } else if ("vjudge".equals(user.getPlatform())) {
                submissions = vjudgeAPI.fetchSubmissions(user, maxSubs);
            } else {
                throw new RuntimeException("Unknown platform: " + user.getPlatform());
            }

            progressCallback.accept("📥 Tìm thấy " + submissions.size() + " submissions. Đang lưu...");

            for (int i = 0; i < submissions.size() && running; i++) {
                Submission sub = submissions.get(i);
                
                // Check if already exists
                if (submissionDAO.submissionExists(user.getId(), sub.getSubmissionId())) {
                    continue;
                }

                // Try to fetch source code
                progressCallback.accept("📄 [" + (i + 1) + "/" + submissions.size() + "] Lấy source code: " + sub.getProblemName());
                
                String sourceCode = null;
                try {
                    if ("codeforces".equals(user.getPlatform())) {
                        sourceCode = codeforcesAPI.fetchSourceCode(sub.getSubmissionId(), sub.getProblemId());
                    } else {
                        sourceCode = vjudgeAPI.fetchSourceCode(sub.getSubmissionId());
                    }
                } catch (Exception e) {
                    System.err.println("⚠ Could not fetch source for " + sub.getSubmissionId() + ": " + e.getMessage());
                }

                sub.setSourceCode(sourceCode);
                
                int id = submissionDAO.addSubmission(sub);
                if (id > 0) {
                    newCount++;
                }

                // Rate limiting
                Thread.sleep(300);
            }

            // Update last crawled timestamp
            userDAO.updateLastCrawled(user.getId());
            
            progressCallback.accept("✅ Hoàn tất! " + newCount + " submissions mới được crawl cho " + user.getDisplayName());
            
        } finally {
            running = false;
        }

        return newCount;
    }

    /**
     * Crawl all users in the system.
     */
    public int crawlAllUsers(Consumer<String> progressCallback) throws Exception {
        List<User> users = userDAO.getAllUsers();
        int totalNew = 0;

        for (User user : users) {
            if (!running) break;
            try {
                int newSubs = crawlUser(user, progressCallback);
                totalNew += newSubs;
            } catch (Exception e) {
                progressCallback.accept("❌ Lỗi crawl " + user.getDisplayName() + ": " + e.getMessage());
            }
        }

        return totalNew;
    }

    public void stop() {
        running = false;
    }

    public boolean isRunning() {
        return running;
    }
}
