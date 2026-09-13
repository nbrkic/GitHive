package com.githive;

import atlantafx.base.theme.NordDark;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.net.ServerSocket;

public class MainApp extends Application {
    private static final int LOCK_PORT = 54321;
    private static ServerSocket lockSocket;

    @Override
    public void start(Stage stage) throws Exception {
        if (!acquireLock()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("GitHive");
            alert.setHeaderText("Already running");
            alert.setContentText("GitHive is already open.");
            alert.showAndWait();
            Platform.exit();
            return;
        }

        Application.setUserAgentStylesheet(new NordDark().getUserAgentStylesheet());
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/githive/views/main.fxml"));
        Scene scene = new Scene(loader.load(), 1200, 750);
        stage.setTitle("GitHive");
        stage.setScene(scene);
        stage.setOnCloseRequest(e -> releaseLock());
        stage.show();
    }

    private boolean acquireLock() {
        try {
            lockSocket = new ServerSocket(LOCK_PORT);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void releaseLock() {
        try {
            if (lockSocket != null) lockSocket.close();
        } catch (Exception ignored) {}
    }
}