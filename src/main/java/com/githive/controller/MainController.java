package com.githive.controller;

import com.githive.model.CommitInfo;
import com.githive.model.GraphRow;
import com.githive.service.GitService;
import com.githive.service.GraphLayoutService;
import com.githive.service.RecentReposService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class MainController implements Initializable {
    @FXML private Button openRepoBtn;
    @FXML private ListView<String> branchList;
    @FXML private TableView<CommitInfo> commitTable;
    @FXML private TableColumn<CommitInfo, String> hashCol;
    @FXML private TableColumn<CommitInfo, String> messageCol;
    @FXML private TableColumn<CommitInfo, String> authorCol;
    @FXML private TableColumn<CommitInfo, String> dateCol;
@FXML private Label statusLabel;
    @FXML private ListView<String> fileList;
    @FXML private TextArea  diffView;
    @FXML private MenuButton recentMenu;
    @FXML private TableColumn<CommitInfo, GraphRow> graphCol;

    private final GitService gitService = new GitService();
    private CommitInfo selectedCommit;
    private final RecentReposService recentRepos = new RecentReposService();
    private final GraphLayoutService graphLayout = new GraphLayoutService();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        hashCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().shortHash()));
        messageCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().message()));
        authorCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().author()));
        dateCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().date()));

        commitTable.getSelectionModel().selectedItemProperty().addListener((obs, old, commit) -> {
            if(commit != null) onCommitSelected(commit);
        });

        fileList.getSelectionModel().selectedItemProperty().addListener((obs, old, file) -> {
            if(file != null && selectedCommit != null) onFileSelected(file);
        });

        List<String> recent = recentRepos.load();
        if(!recent.isEmpty()){
            recentMenu.getItems().clear();
            for(String path : recent){
                MenuItem item = new MenuItem(path);
                item.setOnAction(e -> loadRepository(new File(path)));
                recentMenu.getItems().add(item);
            }
        }
        graphCol.setCellFactory(col -> new GraphCell());
    }

    @FXML
    private void handleOpenRepo(){
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Open Git Repository");
        File dir = chooser.showDialog(openRepoBtn.getScene().getWindow());
        if(dir != null) loadRepository(dir);
    }

    @FXML
    private void handleRefresh(){
        if(gitService.isLoaded()) loadRepository(gitService.getRepoDir());
    }

    private void loadRepository(File dir){
        try{
            gitService.open(dir);
            branchList.setItems(FXCollections.observableArrayList(gitService.getBranches()));
            List<CommitInfo> commits = gitService.getCommits(200);
            List<GraphRow> graphRows = graphLayout.compute(commits);
            ObservableList<CommitInfo> items = FXCollections.observableArrayList(commits);
            commitTable.setItems(items);
            graphCol.setCellValueFactory(d -> {
                int index = items.indexOf(d.getValue());
                return new javafx.beans.property.SimpleObjectProperty<>(index >= 0 ? graphRows.get(index) : null);
            });
            statusLabel.setText("Loaded: " + dir.getName());
            try{
                recentRepos.add(dir.getAbsolutePath());
            }catch (Exception ignored){}
        }catch (Exception e){
            statusLabel.setText("Error " + e.getMessage());
        }
    }

   private void onCommitSelected(CommitInfo commit){
        selectedCommit = commit;
        diffView.clear();
        try{
            fileList.setItems(FXCollections.observableArrayList(gitService.getChangedFiles(commit.fullHash())));
        }catch (Exception e){
            statusLabel.setText("Error: " + e.getMessage());
        }
   }

   private void onFileSelected(String fileEntry){
        try{
            String diff = gitService.getFileDiff(selectedCommit.fullHash(), fileEntry);
            diffView.setText(diff);
            diffView.setScrollTop(0);
        }catch (Exception e){
            statusLabel.setText("Error: " + e.getMessage());
        }
   }
}
