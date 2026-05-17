package com.deepcode.controller;

import com.deepcode.service.SchedulerService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

public class MainController {
    private final Stage stage;
    private final SchedulerService schedulerService;
    
    private BorderPane rootPane;
    private VBox sidebar;
    private StackPane contentArea;
    
    // Sub-controllers
    private UserController userController;
    private AnalysisController analysisController;
    private ReportController reportController;
    private SettingsController settingsController;

    public MainController(Stage stage, SchedulerService schedulerService) {
        this.stage = stage;
        this.schedulerService = schedulerService;
        
        this.userController = new UserController();
        this.analysisController = new AnalysisController();
        this.reportController = new ReportController();
        this.settingsController = new SettingsController(schedulerService, new com.deepcode.service.CrawlerService());
    }

    public Scene createScene() {
        rootPane = new BorderPane();
        
        createSidebar();
        
        contentArea = new StackPane();
        contentArea.setPadding(new Insets(20));
        rootPane.setCenter(contentArea);
        
        // Show default view
        showUserManagement();
        
        Scene scene = new Scene(rootPane);
        scene.getStylesheets().add(getClass().getResource("/styles.css") != null ? 
            getClass().getResource("/styles.css").toExternalForm() : "");
            
        return scene;
    }

    private void createSidebar() {
        sidebar = new VBox(10);
        sidebar.setPadding(new Insets(20));
        sidebar.setStyle("-fx-background-color: #2c3e50;");
        sidebar.setPrefWidth(250);

        Label titleLabel = new Label("DeepCode");
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 24px; -fx-font-weight: bold;");
        titleLabel.setMaxWidth(Double.MAX_VALUE);
        titleLabel.setAlignment(Pos.CENTER);
        
        VBox.setMargin(titleLabel, new Insets(0, 0, 30, 0));

        Button btnUsers = createMenuButton("👥 Quản lý Nick", e -> showUserManagement());
        Button btnAnalysis = createMenuButton("🔬 Phân tích Code", e -> showAnalysis());
        Button btnReport = createMenuButton("📊 Báo cáo & Đánh giá", e -> showReport());
        Button btnSettings = createMenuButton("⚙ Cài đặt", e -> showSettings());

        sidebar.getChildren().addAll(titleLabel, btnUsers, btnAnalysis, btnReport, btnSettings);
        
        // Status area at bottom
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        
        Label lblStatus = new Label("Trạng thái Scheduler:\n" + schedulerService.getLastStatus());
        lblStatus.setStyle("-fx-text-fill: #bdc3c7; -fx-font-size: 12px;");
        lblStatus.setWrapText(true);
        
        // Update status periodically
        Thread statusThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(2000);
                    javafx.application.Platform.runLater(() -> 
                        lblStatus.setText("Trạng thái Scheduler:\n" + schedulerService.getLastStatus())
                    );
                } catch (InterruptedException ex) {
                    break;
                }
            }
        });
        statusThread.setDaemon(true);
        statusThread.start();
        
        sidebar.getChildren().addAll(spacer, lblStatus);
        
        rootPane.setLeft(sidebar);
    }

    private Button createMenuButton(String text, javafx.event.EventHandler<javafx.event.ActionEvent> handler) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 16px; -fx-alignment: center-left; -fx-padding: 10 20;");
        btn.setOnAction(handler);
        
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #34495e; -fx-text-fill: white; -fx-font-size: 16px; -fx-alignment: center-left; -fx-padding: 10 20;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 16px; -fx-alignment: center-left; -fx-padding: 10 20;"));
        
        return btn;
    }

    private void showUserManagement() {
        contentArea.getChildren().setAll(userController.getView());
        userController.refreshData();
    }

    private void showAnalysis() {
        contentArea.getChildren().setAll(analysisController.getView());
        analysisController.refreshData();
    }

    private void showReport() {
        contentArea.getChildren().setAll(reportController.getView());
        reportController.refreshData();
    }
    
    private void showSettings() {
        contentArea.getChildren().setAll(settingsController.getView());
    }
}
