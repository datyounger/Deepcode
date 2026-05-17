package com.deepcode.controller;

import com.deepcode.dao.UserDAO;
import com.deepcode.model.User;
import com.deepcode.service.CrawlerService;
import com.deepcode.service.CodeforcesAPI;
import com.deepcode.service.VJudgeAPI;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.text.Font;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class UserController {
    private final VBox view;
    private final TableView<User> table;
    private final UserDAO userDAO;
    private final CrawlerService crawlerService;
    private final CodeforcesAPI cfAPI;
    private final VJudgeAPI vjAPI;
    private final ObservableList<User> userList;
    private final TextArea logArea;

    public UserController() {
        this.crawlerService = new CrawlerService();
        this.userDAO = new UserDAO();
        this.cfAPI = new CodeforcesAPI();
        this.vjAPI = new VJudgeAPI();
        this.userList = FXCollections.observableArrayList();
        
        view = new VBox(20);
        view.setPadding(new Insets(20));

        Label title = new Label("Quản lý Nick (Codeforces / VJudge)");
        title.setFont(Font.font("System", 24));

        // Form
        HBox form = new HBox(10);
        TextField txtUsername = new TextField();
        txtUsername.setPromptText("Nhập username...");
        ComboBox<String> cbPlatform = new ComboBox<>(FXCollections.observableArrayList("codeforces", "vjudge"));
        cbPlatform.setValue("codeforces");
        Button btnAdd = new Button("Thêm Nick");
        
        form.getChildren().addAll(new Label("Nick:"), txtUsername, new Label("Nền tảng:"), cbPlatform, btnAdd);

        // Table
        table = new TableView<>();
        
        TableColumn<User, String> colUsername = new TableColumn<>("Username");
        colUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
        colUsername.setPrefWidth(200);

        TableColumn<User, String> colPlatform = new TableColumn<>("Nền tảng");
        colPlatform.setCellValueFactory(new PropertyValueFactory<>("platform"));
        colPlatform.setPrefWidth(150);

        TableColumn<User, LocalDateTime> colAdded = new TableColumn<>("Ngày thêm");
        colAdded.setCellValueFactory(new PropertyValueFactory<>("addedDate"));
        colAdded.setPrefWidth(200);
        colAdded.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setText(null);
                else setText(item.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            }
        });

        TableColumn<User, LocalDateTime> colCrawled = new TableColumn<>("Lần crawl cuối");
        colCrawled.setCellValueFactory(new PropertyValueFactory<>("lastCrawled"));
        colCrawled.setPrefWidth(200);
        colCrawled.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setText("Chưa crawl");
                else setText(item.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            }
        });

        table.getColumns().addAll(colUsername, colPlatform, colAdded, colCrawled);
        table.setItems(userList);
        VBox.setVgrow(table, Priority.ALWAYS);

        // Actions
        HBox actions = new HBox(10);
        Button btnDelete = new Button("Xóa Nick");
        Button btnCrawl = new Button("Crawl Code ngay");
        Button btnCrawlAll = new Button("Crawl tất cả");
        actions.getChildren().addAll(btnDelete, btnCrawl, btnCrawlAll);

        // Log Area
        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setPrefHeight(150);
        logArea.setStyle("-fx-font-family: monospace;");

        view.getChildren().addAll(title, form, table, actions, new Label("Log:"), logArea);

        // Event Handlers
        btnAdd.setOnAction(e -> {
            String username = txtUsername.getText().trim();
            String platform = cbPlatform.getValue();
            if (username.isEmpty()) {
                showAlert("Lỗi", "Vui lòng nhập username!");
                return;
            }
            
            // Validate via API
            log("Đang kiểm tra username " + username + " trên " + platform + "...");
            new Thread(() -> {
                boolean valid = false;
                if ("codeforces".equals(platform)) {
                    valid = cfAPI.validateHandle(username);
                } else if ("vjudge".equals(platform)) {
                    valid = vjAPI.validateUsername(username);
                }

                if (valid) {
                    Platform.runLater(() -> {
                        try {
                            User u = new User(username, platform);
                            userDAO.addUser(u);
                            refreshData();
                            txtUsername.clear();
                            log("✅ Đã thêm nick: " + username + " (" + platform + ")");
                        } catch (Exception ex) {
                            showAlert("Lỗi CSDL", "Có thể nick này đã tồn tại.");
                            log("❌ Lỗi lưu CSDL: " + ex.getMessage());
                        }
                    });
                } else {
                    Platform.runLater(() -> {
                        showAlert("Lỗi API", "Không tìm thấy user '" + username + "' trên " + platform + "!");
                        log("❌ Không tìm thấy: " + username);
                    });
                }
            }).start();
        });

        btnDelete.setOnAction(e -> {
            User selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) {
                try {
                    userDAO.deleteUser(selected.getId());
                    refreshData();
                    log("Đã xóa nick: " + selected.getUsername());
                } catch (Exception ex) {
                    showAlert("Lỗi", "Không thể xóa nick: " + ex.getMessage());
                }
            }
        });

        btnCrawl.setOnAction(e -> {
            User selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) {
                btnCrawl.setDisable(true);
                new Thread(() -> {
                    try {
                        crawlerService.crawlUser(selected, this::log);
                        Platform.runLater(this::refreshData);
                    } catch (Exception ex) {
                        log("❌ Lỗi: " + ex.getMessage());
                    } finally {
                        Platform.runLater(() -> btnCrawl.setDisable(false));
                    }
                }).start();
            }
        });

        btnCrawlAll.setOnAction(e -> {
            btnCrawlAll.setDisable(true);
            new Thread(() -> {
                try {
                    crawlerService.crawlAllUsers(this::log);
                    Platform.runLater(this::refreshData);
                } catch (Exception ex) {
                    log("❌ Lỗi: " + ex.getMessage());
                } finally {
                    Platform.runLater(() -> btnCrawlAll.setDisable(false));
                }
            }).start();
        });
    }

    public Node getView() {
        return view;
    }

    public void refreshData() {
        try {
            userList.setAll(userDAO.getAllUsers());
        } catch (Exception e) {
            System.err.println("Error loading users: " + e.getMessage());
        }
    }

    private void log(String message) {
        Platform.runLater(() -> {
            logArea.appendText(message + "\n");
            logArea.positionCaret(logArea.getLength());
        });
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
