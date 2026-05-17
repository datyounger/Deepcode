package com.deepcode.controller;

import com.deepcode.dao.AnalysisDAO;
import com.deepcode.dao.UserDAO;
import com.deepcode.model.User;
import com.deepcode.model.UserEvaluation;
import com.deepcode.service.AIAnalyzer;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.text.Font;

import java.util.List;

public class ReportController {
    private final VBox view;
    private final TableView<UserEvaluation> table;
    private final TextArea reportArea;
    private final AnalysisDAO analysisDAO = new AnalysisDAO();
    private final UserDAO userDAO = new UserDAO();
    private final AIAnalyzer aiAnalyzer = new AIAnalyzer();
    private final ObservableList<UserEvaluation> evalList = FXCollections.observableArrayList();

    public ReportController() {
        view = new VBox(20);
        view.setPadding(new Insets(20));

        Label title = new Label("Báo cáo & Đánh giá Năng lực");
        title.setFont(Font.font("System", 24));

        HBox actions = new HBox(10);
        Button btnRefresh = new Button("Làm mới danh sách");
        Button btnEvaluateAll = new Button("Đánh giá lại tất cả User");
        actions.getChildren().addAll(btnRefresh, btnEvaluateAll);

        // SplitPane for Table and Report text
        SplitPane splitPane = new SplitPane();
        VBox.setVgrow(splitPane, Priority.ALWAYS);

        // Table
        table = new TableView<>();
        setupTableColumns();
        table.setItems(evalList);

        // Details text area
        reportArea = new TextArea();
        reportArea.setEditable(false);
        reportArea.setWrapText(true);
        reportArea.setStyle("-fx-font-size: 14px;");

        splitPane.getItems().addAll(table, reportArea);
        splitPane.setDividerPositions(0.6);

        view.getChildren().addAll(title, actions, splitPane);

        // Handlers
        btnRefresh.setOnAction(e -> refreshData());

        btnEvaluateAll.setOnAction(e -> {
            btnEvaluateAll.setDisable(true);
            new Thread(() -> {
                try {
                    List<User> users = userDAO.getAllUsers();
                    for (User u : users) {
                        try {
                            aiAnalyzer.evaluateUser(u);
                        } catch (Exception ex) {
                            System.err.println("Cannot evaluate " + u.getUsername() + ": " + ex.getMessage());
                        }
                    }
                    Platform.runLater(this::refreshData);
                } catch (Exception ex) {
                    ex.printStackTrace();
                } finally {
                    Platform.runLater(() -> btnEvaluateAll.setDisable(false));
                }
            }).start();
        });

        table.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) {
                reportArea.setText(newV.getEvaluationSummary());
            }
        });
    }

    private void setupTableColumns() {
        TableColumn<UserEvaluation, String> colUser = new TableColumn<>("Username");
        colUser.setCellValueFactory(new PropertyValueFactory<>("username"));

        TableColumn<UserEvaluation, String> colPlat = new TableColumn<>("Nền tảng");
        colPlat.setCellValueFactory(new PropertyValueFactory<>("platform"));

        TableColumn<UserEvaluation, String> colDs = new TableColumn<>("Trình độ CTDL");
        colDs.setCellValueFactory(new PropertyValueFactory<>("dsLevel"));

        TableColumn<UserEvaluation, String> colAlgo = new TableColumn<>("Trình độ Thuật toán");
        colAlgo.setCellValueFactory(new PropertyValueFactory<>("algoLevel"));

        TableColumn<UserEvaluation, String> colAi = new TableColumn<>("Mức dùng AI");
        colAi.setCellValueFactory(new PropertyValueFactory<>("aiUsageLevel"));

        TableColumn<UserEvaluation, Integer> colTotal = new TableColumn<>("Đã phân tích");
        colTotal.setCellValueFactory(new PropertyValueFactory<>("analyzedSubmissions"));

        table.getColumns().addAll(colUser, colPlat, colDs, colAlgo, colAi, colTotal);
    }

    public Node getView() {
        return view;
    }

    public void refreshData() {
        try {
            evalList.setAll(analysisDAO.getAllEvaluations());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
