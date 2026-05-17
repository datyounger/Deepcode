package com.deepcode;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.application.Platform;

import com.deepcode.controller.MainController;
import com.deepcode.dao.DatabaseManager;
import com.deepcode.service.SchedulerService;

/**
 * DeepCode - Hệ thống phân tích code Competitive Programming
 * Main Application Entry Point
 */
public class App extends Application {

    private MainController mainController;
    private SchedulerService schedulerService;

    @Override
    public void start(Stage primaryStage) {
        // Initialize database
        DatabaseManager.getInstance().initialize();

        // Initialize scheduler
        schedulerService = new SchedulerService();

        // Create main controller
        mainController = new MainController(primaryStage, schedulerService);

        // Build and show UI
        Scene scene = mainController.createScene();
        
        primaryStage.setTitle("DeepCode v1.2.0 - Hệ Thống Phân Tích Code CP");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(1200);
        primaryStage.setMinHeight(800);
        primaryStage.setWidth(1400);
        primaryStage.setHeight(900);
        primaryStage.show();

        // Start scheduler
        schedulerService.start();

        // Cleanup on close
        primaryStage.setOnCloseRequest(e -> {
            schedulerService.stop();
            DatabaseManager.getInstance().close();
            Platform.exit();
            System.exit(0);
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}
