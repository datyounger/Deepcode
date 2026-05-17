package com.deepcode.controller;

import com.deepcode.dao.AnalysisDAO;
import com.deepcode.dao.SubmissionDAO;
import com.deepcode.dao.UserDAO;
import com.deepcode.model.AnalysisResult;
import com.deepcode.model.Submission;
import com.deepcode.model.User;
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

public class AnalysisController {
    private final VBox view;
    private final ComboBox<User> cbUser;
    private final TableView<Submission> tableUnanalyzed;
    private final TableView<AnalysisResult> tableResults;
    private final TextArea logArea;
    
    private final UserDAO userDAO = new UserDAO();
    private final SubmissionDAO subDAO = new SubmissionDAO();
    private final AnalysisDAO analysisDAO = new AnalysisDAO();
    private final AIAnalyzer aiAnalyzer = new AIAnalyzer();

    private final ObservableList<Submission> unanalyzedList = FXCollections.observableArrayList();
    private final ObservableList<AnalysisResult> resultsList = FXCollections.observableArrayList();

    public AnalysisController() {
        view = new VBox(20);
        view.setPadding(new Insets(20));

        Label title = new Label("Phân Tích Code bằng AI");
        title.setFont(Font.font("System", 24));

        // User Selection
        HBox filterBox = new HBox(10);
        cbUser = new ComboBox<>();
        cbUser.setPromptText("Chọn User");
        Button btnAnalyze = new Button("Phân tích AI cho User này");
        btnAnalyze.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white;");
        filterBox.getChildren().addAll(new Label("User:"), cbUser, btnAnalyze);

        // Split Pane for layout
        SplitPane splitPane = new SplitPane();
        VBox.setVgrow(splitPane, Priority.ALWAYS);

        // Left Side: Unanalyzed Submissions
        VBox leftBox = new VBox(10);
        Label lblUnanalyzed = new Label("Submissions chưa phân tích:");
        lblUnanalyzed.setFont(Font.font(16));
        tableUnanalyzed = createUnanalyzedTable();
        leftBox.getChildren().addAll(lblUnanalyzed, tableUnanalyzed);

        // Right Side: Analysis Results
        VBox rightBox = new VBox(10);
        Label lblResults = new Label("Kết quả phân tích gần đây:");
        lblResults.setFont(Font.font(16));
        tableResults = createResultsTable();
        rightBox.getChildren().addAll(lblResults, tableResults);

        splitPane.getItems().addAll(leftBox, rightBox);
        splitPane.setDividerPositions(0.4);

        // Log Area
        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setPrefHeight(120);
        logArea.setStyle("-fx-font-family: monospace;");

        view.getChildren().addAll(title, filterBox, splitPane, new Label("Log:"), logArea);

        // Event Handlers
        cbUser.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) loadUserData(newV);
        });

        btnAnalyze.setOnAction(e -> {
            User user = cbUser.getValue();
            if (user == null) {
                showAlert("Vui lòng chọn user!");
                return;
            }

            btnAnalyze.setDisable(true);
            new Thread(() -> {
                try {
                    aiAnalyzer.analyzeUserSubmissions(user, this::log);
                    Platform.runLater(() -> loadUserData(user));
                } catch (Exception ex) {
                    log("❌ Lỗi: " + ex.getMessage());
                } finally {
                    Platform.runLater(() -> btnAnalyze.setDisable(false));
                }
            }).start();
        });

        // Double click on result to see details
        tableResults.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && tableResults.getSelectionModel().getSelectedItem() != null) {
                showResultDetails(tableResults.getSelectionModel().getSelectedItem());
            }
        });
    }

    private TableView<Submission> createUnanalyzedTable() {
        TableView<Submission> table = new TableView<>();
        
        TableColumn<Submission, String> colProb = new TableColumn<>("Bài toán");
        colProb.setCellValueFactory(new PropertyValueFactory<>("problemName"));
        
        TableColumn<Submission, String> colLang = new TableColumn<>("Ngôn ngữ");
        colLang.setCellValueFactory(new PropertyValueFactory<>("language"));

        table.getColumns().addAll(colProb, colLang);
        table.setItems(unanalyzedList);
        VBox.setVgrow(table, Priority.ALWAYS);
        return table;
    }

    private TableView<AnalysisResult> createResultsTable() {
        TableView<AnalysisResult> table = new TableView<>();
        
        TableColumn<AnalysisResult, String> colProb = new TableColumn<>("Bài toán");
        colProb.setCellValueFactory(new PropertyValueFactory<>("problemName"));
        colProb.setPrefWidth(150);

        TableColumn<AnalysisResult, String> colAi = new TableColumn<>("Mức độ AI");
        colAi.setCellValueFactory(new PropertyValueFactory<>("aiProbabilityDisplay"));
        colAi.setPrefWidth(150);

        TableColumn<AnalysisResult, String> colAlgo = new TableColumn<>("Thuật toán");
        colAlgo.setCellValueFactory(new PropertyValueFactory<>("algorithmsDisplay"));
        colAlgo.setPrefWidth(150);

        TableColumn<AnalysisResult, String> colDs = new TableColumn<>("Cấu trúc DL");
        colDs.setCellValueFactory(new PropertyValueFactory<>("dataStructuresDisplay"));
        colDs.setPrefWidth(150);

        table.getColumns().addAll(colProb, colAi, colAlgo, colDs);
        table.setItems(resultsList);
        VBox.setVgrow(table, Priority.ALWAYS);
        return table;
    }

    private void showResultDetails(AnalysisResult result) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Chi tiết phân tích: " + result.getProblemName());
        alert.setHeaderText("Kết quả phân tích AI");
        
        TextArea area = new TextArea();
        area.setEditable(false);
        area.setWrapText(true);
        area.setPrefSize(600, 400);
        
        StringBuilder sb = new StringBuilder();
        sb.append("Bài toán: ").append(result.getProblemName()).append("\n");
        sb.append("Độ phức tạp: ").append(result.getComplexity()).append("\n");
        sb.append("Tỷ lệ AI code: ").append(result.getAiProbabilityDisplay()).append("\n");
        sb.append("Dấu hiệu AI:\n").append(result.getAiIndicators()).append("\n\n");
        sb.append("Tóm tắt:\n").append(result.getAnalysisSummary()).append("\n");
        
        area.setText(sb.toString());
        alert.getDialogPane().setContent(area);
        alert.showAndWait();
    }

    public Node getView() {
        return view;
    }

    public void refreshData() {
        try {
            User current = cbUser.getValue();
            List<User> users = userDAO.getAllUsers();
            cbUser.setItems(FXCollections.observableArrayList(users));
            
            if (current != null) {
                // Try to keep selection
                users.stream().filter(u -> u.getId() == current.getId()).findFirst().ifPresent(cbUser::setValue);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadUserData(User user) {
        try {
            unanalyzedList.setAll(subDAO.getUnanalyzedSubmissions(user.getId()));
            resultsList.setAll(analysisDAO.getAnalysisResultsByUserId(user.getId()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void log(String msg) {
        Platform.runLater(() -> {
            logArea.appendText(msg + "\n");
            logArea.positionCaret(logArea.getLength());
        });
    }

    private void showAlert(String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.show();
    }
}
