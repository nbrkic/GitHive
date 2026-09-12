package com.githive.controller;

import com.githive.model.CommitInfo;
import com.githive.service.GitService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

public class MainController implements Initializable {

    @FXML private Button openRepoBtn;
    @FXML private ListView<String> branchList;
    @FXML private TableView<CommitInfo> commitTable;
    @FXML private TableColumn<CommitInfo, String> hashCol;
    @FXML private TableColumn<CommitInfo, String> messageCol;
    @FXML private TableColumn<CommitInfo, String> authorCol;
    @FXML private TableColumn<CommitInfo, String> dateCol;
    @FXML private TextArea commitDetails;
    @FXML private Label statusLabel;

    private final GitService gitService = new GitService();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        hashCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().shortHash()));
        messageCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().message()));
        authorCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().author()));
        dateCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().date()));

        commitTable.getSelectionModel().selectedItemProperty().addListener((obs, old, commit) -> {
            if (commit != null) showCommitDetails(commit);
        });
    }

    @FXML
    private void handleOpenRepo() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Open Git Repository");
        File dir = chooser.showDialog(openRepoBtn.getScene().getWindow());
        if (dir != null) loadRepository(dir);
    }

    @FXML
    private void handleRefresh() {
        if (gitService.isLoaded()) loadRepository(gitService.getRepoDir());
    }

    private void loadRepository(File dir) {
        try {
            gitService.open(dir);
            branchList.setItems(FXCollections.observableArrayList(gitService.getBranches()));
            commitTable.setItems(FXCollections.observableArrayList(gitService.getCommits(100)));
            statusLabel.setText("Loaded: " + dir.getAbsolutePath());
        } catch (Exception e) {
            statusLabel.setText("Error: " + e.getMessage());
        }
    }

    private void showCommitDetails(CommitInfo commit) {
        commitDetails.setText(
            "Hash:    " + commit.fullHash() + "\n" +
            "Author:  " + commit.author() + "\n" +
            "Date:    " + commit.date() + "\n\n" +
            commit.message()
        );
    }
}
