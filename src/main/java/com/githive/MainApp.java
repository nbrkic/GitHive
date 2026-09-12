package com.githive;

import atlantafx.base.theme.NordDark;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        Application.setUserAgentStylesheet(new NordDark().getUserAgentStylesheet());

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/githive/views/main.fxml"));
        Scene scene = new Scene(loader.load(), 1200, 750);
        scene.getStylesheets().add(getClass().getResource("/com/githive/css/githive.css").toExternalForm());
        stage.setTitle("GitHive");
        stage.setScene(scene);
        stage.show();
    }
}
