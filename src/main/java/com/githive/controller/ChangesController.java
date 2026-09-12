package com.githive.controller;

import com.githive.service.GitService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import org.eclipse.jgit.api.Status;

import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;


public class ChangesController implements Initializable {
    @FXML private ListView<String> unstagedList;
    @FXML private ListView<String> stagedList;
    @FXML private TextField commitMsg;

    private GitService gitService;

    public void setGitService(GitService gitService){
        this.gitService = gitService;
        refresh();
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {}

    public void refresh(){
        try{
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
        }catch (Exception e){
            unstagedList.setItems(FXCollections.observableArrayList("Error: " + e.getMessage()));
        }
    }

    @FXML
    private void handleStage(){
        String selected = unstagedList.getSelectionModel().getSelectedItem();
        if(selected == null) return;
        try{
            gitService.stageFile(selected.substring(3));
            refresh();
        }catch (Exception e){
            unstagedList.setItems(FXCollections.observableArrayList("Error: " + e.getMessage()));
        }
    }

    @FXML
    private void handleCommit(){
        String msg = commitMsg.getText().trim();
        if(msg.isEmpty() || stagedList.getItems().isEmpty()) return;
        try{
            gitService.commit(msg);
            commitMsg.clear();
            refresh();
        }catch (Exception e){
            commitMsg.setText("Error: " + e.getMessage());
        }
    }
}
