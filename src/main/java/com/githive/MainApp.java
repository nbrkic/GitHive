package com.githive;

import atlantafx.base.theme.NordDark;
import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

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
            alert.getDialogPane().getStylesheets().add(getClass().getResource("/com/githive/css/app.css").toExternalForm());
            alert.getDialogPane().setGraphic(null);
            alert.showAndWait();
            Platform.exit();
            return;
        }

        showSplash(() -> {
            try {
                Application.setUserAgentStylesheet(new NordDark().getUserAgentStylesheet());
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/githive/views/main.fxml"));
                Scene scene = new Scene(loader.load(), 1440, 860);
                scene.getStylesheets().add(getClass().getResource("/com/githive/css/app.css").toExternalForm());
                stage.setTitle("GitHive");
                stage.setScene(scene);
                stage.setOnCloseRequest(e -> {
                    releaseLock();
                    Platform.exit();
                });
                stage.show();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
    }

    private void showSplash(Runnable onFinished) {
        Stage splash = new Stage();
        splash.initStyle(StageStyle.UNDECORATED);

        Label title = new Label("GitHive");
        title.setStyle("-fx-font-size: 38; -fx-font-weight: bold; -fx-text-fill: #58a6ff;");
        Label sub = new Label("Git GUI Client");
        sub.setStyle("-fx-font-size: 13; -fx-text-fill: #8b949e;");

        VBox box = new VBox(8, title, sub);
        box.setAlignment(Pos.CENTER);
        box.setStyle("-fx-background-color: #0d1117; -fx-padding: 40; -fx-border-color: #30363d; -fx-border-width: 1;");

        splash.setScene(new Scene(box, 320, 160));
        splash.show();

        PauseTransition pause = new PauseTransition(Duration.seconds(1.8));
        pause.setOnFinished(e -> {
            splash.close();
            onFinished.run();
        });
        pause.play();
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