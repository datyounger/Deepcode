package com.deepcode.controller;

import com.deepcode.service.SchedulerService;
import com.deepcode.service.CrawlerService;
import com.deepcode.dao.UserDAO;
import com.deepcode.model.User;
import com.deepcode.util.Config;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import java.util.List;

public class SettingsController {
    private final VBox view;
    private final Config config = Config.getInstance();
    private final SchedulerService scheduler;
    private final CrawlerService crawlerService;
    private final TextArea logArea;

    public SettingsController(SchedulerService scheduler, CrawlerService crawlerService) {
        this.scheduler = scheduler;
        this.crawlerService = crawlerService;
        view = new VBox(20);
        view.setPadding(new Insets(20));
        
        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setPrefHeight(150);
        logArea.setStyle("-fx-font-family: monospace; -fx-font-size: 12px;");
        
        Label title = new Label("Cài đặt Hệ thống");
        title.setFont(Font.font("System", 24));

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        
        // API Key
        TextField txtApiKey = new TextField(config.getGeminiApiKey());
        txtApiKey.setPrefWidth(400);
        grid.add(new Label("Google Gemini API Key:"), 0, 0);
        grid.add(txtApiKey, 1, 0);

        // Crawl Interval
        TextField txtInterval = new TextField(String.valueOf(config.getCrawlIntervalHours()));
        grid.add(new Label("Crawl định kỳ (giờ):"), 0, 1);
        grid.add(txtInterval, 1, 1);

        // Max submissions per crawl
        TextField txtMaxSubs = new TextField(String.valueOf(config.getMaxSubmissions()));
        grid.add(new Label("Giới hạn crawl (số bài):"), 0, 2);
        grid.add(txtMaxSubs, 1, 2);
        
        // Gemini Model
        TextField txtModel = new TextField(config.getGeminiModel());
        grid.add(new Label("Gemini Model:"), 0, 3);
        grid.add(txtModel, 1, 3);

        Button btnSave = new Button("Lưu Cài đặt");
        btnSave.setStyle("-fx-background-color: #3498db; -fx-text-fill: white;");
        
        // Nút crawl thủ công tất cả users
        Button btnCrawlAll = new Button("Crawl tất cả ngay");
        btnCrawlAll.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-size: 14px;");
        
        Label lblMsg = new Label();
        lblMsg.setStyle("-fx-text-fill: #27ae60;");
        
        btnSave.setOnAction(e -> {
            try {
                config.setGeminiApiKey(txtApiKey.getText().trim());
                config.set("crawl.interval.hours", txtInterval.getText().trim());
                config.set("crawl.max.submissions", txtMaxSubs.getText().trim());
                config.set("gemini.model", txtModel.getText().trim());
                config.saveConfig();
                
                // Restart scheduler to apply new interval
                scheduler.stop();
                scheduler.start();
                
                lblMsg.setText("Đã lưu thành công! Scheduler đã được khởi động lại.");
            } catch (Exception ex) {
                lblMsg.setText("Lỗi: " + ex.getMessage());
                lblMsg.setStyle("-fx-text-fill: #e74c3c;");
            }
        });
        
        btnCrawlAll.setOnAction(e -> {
            btnCrawlAll.setDisable(true);
            log("=== Bắt đầu crawl thủ công ===");
            new Thread(() -> {
                try {
                    UserDAO userDAO = new UserDAO();
                    List<User> users = userDAO.getAllUsers();
                    if (users.isEmpty()) {
                        Platform.runLater(() -> log("Không có user nào trong hệ thống!"));
                        Platform.runLater(() -> btnCrawlAll.setDisable(false));
                        return;
                    }
                    log("Tìm thấy " + users.size() + " user(s)");
                    int successCount = 0;
                    for (User user : users) {
                        try {
                            log("--- Đang crawl: " + user.getUsername() + " ---");
                            crawlerService.crawlUser(user, msg -> {
                                Platform.runLater(() -> log(msg));
                            });
                            successCount++;
                        } catch (Exception ex) {
                            Platform.runLater(() -> log("Lỗi crawl " + user.getUsername() + ": " + ex.getMessage()));
                        }
                    }
                    log("=== Hoàn tất! Đã crawl " + successCount + "/" + users.size() + " user(s) ===");
                } catch (Exception ex) {
                    Platform.runLater(() -> log("Lỗi: " + ex.getMessage()));
                } finally {
                    Platform.runLater(() -> btnCrawlAll.setDisable(false));
                }
            }).start();
        });
        
        VBox helpBox = new VBox(10);
        helpBox.setPadding(new Insets(20, 0, 0, 0));
        helpBox.getChildren().addAll(
            new Label("💡 Hướng dẫn lấy Gemini API Key:"),
            new Label("1. Truy cập https://aistudio.google.com/app/apikey"),
            new Label("2. Đăng nhập bằng tài khoản Google"),
            new Label("3. Bấm 'Create API key'"),
            new Label("4. Copy và dán vào ô bên trên")
        );
        helpBox.setStyle("-fx-text-fill: #7f8c8d;");
        
        view.getChildren().addAll(title, grid, btnSave, lblMsg, new Separator(), 
            btnCrawlAll, new Label("Log crawl:"), logArea, new Separator(), helpBox);
    }
    
    private void log(String message) {
        logArea.appendText("[" + java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")) + "] " + message + "\n");
        logArea.positionCaret(logArea.getLength());
    }

    public Node getView() {
        return view;
    }
}
