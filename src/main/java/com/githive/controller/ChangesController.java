package com.githive.controller;

import com.githive.service.GitService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.eclipse.jgit.api.Status;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Consumer;

public class ChangesController implements Initializable {
    @FXML private ListView<String> unstagedList;
    @FXML private ListView<String> stagedList;
    @FXML private TextField commitMsg;
    @FXML private CheckBox amendCheck;

    private GitService gitService;
    private Consumer<String> onCommitSuccess;

    public void setGitService(GitService gitService) {
        this.gitService = gitService;
        refresh();
    }

    public void setOnCommitSuccess(Consumer<String> callback) {
        this.onCommitSuccess = callback;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        unstagedList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        stagedList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        commitMsg.setOnAction(e -> handleCommit());
    }

    public void refresh() {
        try {
            Status status = gitService.getStatus();
            ArrayList<String> unstaged = new ArrayList<>();
            status.getModified().forEach(f -> unstaged.add("M  " + f));
            status.getUntracked().forEach(f -> unstaged.add("?  " + f));
            status.getMissing().forEach(f -> unstaged.add("D  " + f));
            unstagedList.setItems(FXCollections.observableArrayList(unstaged));

            ArrayList<String> staged = new ArrayList<>();
            status.getAdded().forEach(f -> staged.add("A  " + f));
            status.getChanged().forEach(f -> staged.add("M  " + f));
            status.getRemoved().forEach(f -> staged.add("D  " + f));
            stagedList.setItems(FXCollections.observableArrayList(staged));
        } catch (Exception e) {
            unstagedList.setItems(FXCollections.observableArrayList("Error: " + e.getMessage()));
        }
    }

    @FXML
    private void handleStage() {
        List<String> selected = new ArrayList<>(unstagedList.getSelectionModel().getSelectedItems());
        if (selected.isEmpty()) return;
        try {
            for (String item : selected) gitService.stageFile(item.substring(3));
            refresh();
        } catch (Exception e) {
            unstagedList.setItems(FXCollections.observableArrayList("Error: " + e.getMessage()));
        }
    }

    @FXML
    private void handleStageAll() {
        List<String> all = new ArrayList<>(unstagedList.getItems());
        if (all.isEmpty()) return;
        try {
            for (String item : all) gitService.stageFile(item.substring(3));
            refresh();
        } catch (Exception e) {
            unstagedList.setItems(FXCollections.observableArrayList("Error: " + e.getMessage()));
        }
    }

    @FXML
    private void handleCommit() {
        String msg = commitMsg.getText().trim();
        if (msg.isEmpty()) {
            commitMsg.setStyle("-fx-border-color: #e05252; -fx-border-width: 2;");
            return;
        }
        if (stagedList.getItems().isEmpty() && !amendCheck.isSelected()) return;
        try {
            if (amendCheck.isSelected()) {
                if (gitService.isLastCommitPushed()) {
                    Alert warning = new Alert(Alert.AlertType.CONFIRMATION);
                    warning.setTitle("Upozorenje");
                    warning.setHeaderText("Ovaj commit je već pushovan na remote.");
                    warning.setContentText("Amend će promijeniti historiju i sljedeći push će zahtijevati force push. Nastavi?");
                    if (warning.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;
                }
                gitService.amendCommit(msg);
            } else {
                gitService.commit(msg);
            }
            if (onCommitSuccess != null) onCommitSuccess.accept(msg);
            ((Stage) commitMsg.getScene().getWindow()).close();
        } catch (Exception e) {
            commitMsg.setText("Error: " + e.getMessage());
        }
    }

    @FXML
    private void handleUnstage(){
        List<String> selected = new ArrayList<>(stagedList.getSelectionModel().getSelectedItems());
        if(selected.isEmpty()) return;
        try{
            for(String item : selected) gitService.unstageFile(item.substring(3));
            refresh();
        }catch (Exception e){
            stagedList.setItems(FXCollections.observableArrayList("Error: " + e.getMessage()));
        }
    }

    @FXML
    private void handleDiscard(){
        List<String> selected = new ArrayList<>(unstagedList.getSelectionModel().getSelectedItems());
        if (selected.isEmpty()) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Discard Changes");
        confirm.setHeaderText("Ovo će trajno odbaciti izmjene u odabranim fajlovima.");
        confirm.setContentText("Jesi li siguran?");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                for (String item : selected) {
                    if (item.startsWith("?")) continue; // untracked se ne može discardovati
                    try {
                        gitService.discardChanges(item.substring(3));
                    } catch (Exception e) {
                        unstagedList.setItems(FXCollections.observableArrayList("Error: " + e.getMessage()));
                        return;
                    }
                }
                refresh();
            }
        });
    }

    @FXML
    private void handleIgnore(){
        List<String> seleceted = new ArrayList<>(unstagedList.getSelectionModel().getSelectedItems());
        if(seleceted.isEmpty()) return;
        List<String> paths = seleceted.stream().map(item -> item.substring(3)).toList();
        try{
            gitService.addToGitIgnore(paths);
            refresh();
        }catch (Exception e){
            unstagedList.setItems(FXCollections.observableArrayList("Error: " + e.getMessage()));
        }
    }

    @FXML
    private void handleAmendToggle(){
        if(amendCheck.isSelected()){
            commitMsg.setText(gitService.getLastCommitMessage().trim());
        }
        else{
            commitMsg.clear();
        }
    }
}