package com.githive.controller;

import com.githive.model.CommitInfo;
import com.githive.model.GraphRow;
import com.githive.service.GitService;
import com.githive.service.GraphLayoutService;
import com.githive.service.RecentReposService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.DirectoryChooser;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.eclipse.jgit.api.ResetCommand;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Optional;
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
    @FXML private ListView<String> tagList;
    @FXML private Button refreshBtn;
    @FXML private Button changesBtn;
    @FXML private Button pullBtn;
    @FXML private Button pushBtn;
    @FXML private Button configBtn;
    @FXML private Button newBranchBtn;
    @FXML private Button newTagBtn;
    @FXML private MenuButton stashMenu;
    @FXML private Button fetchBtn;

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

        refreshRecentMenu();
        graphCol.setCellFactory(col -> new GraphCell());

        ContextMenu branchMenu = new ContextMenu();
        MenuItem checkoutItem = new MenuItem("Checkout");
        MenuItem deleteItem = new MenuItem("Delete");

        checkoutItem.setOnAction(e -> {
            String branch = branchList.getSelectionModel().getSelectedItem();
            if(branch == null) return;
            try{
                gitService.checkoutBranch(branch);
                statusLabel.setText("Switched to: " + branch);
                handleRefresh();
            }catch (Exception ex){
                statusLabel.setText("Error: " + ex.getMessage());
            }
        });

        deleteItem.setOnAction(e -> {
            String branch = branchList.getSelectionModel().getSelectedItem();
            if (branch == null) return;
            try {
                gitService.deleteBranch(branch);
                branchList.setItems(FXCollections.observableArrayList(gitService.getBranches()));
                statusLabel.setText("Deleted branch: " + branch);
            } catch (Exception ex) {
                statusLabel.setText("Error: " + ex.getMessage());
            }
        });

        MenuItem mergeItem = new MenuItem("Merge into current");
        mergeItem.setOnAction(e -> {
            String branch = branchList.getSelectionModel().getSelectedItem();
            if (branch == null) return;
            try {
                gitService.merge(branch);
                handleRefresh();
                statusLabel.setText("Merged: " + branch);
            } catch (Exception ex) {
                statusLabel.setText("Error: " + ex.getMessage());
            }
        });

        branchMenu.getItems().addAll(checkoutItem, deleteItem, mergeItem);

        branchList.setContextMenu(branchMenu);

        ContextMenu tagMenu = new ContextMenu();
        MenuItem deleteTagItem = new MenuItem("Delete Tag");
        deleteTagItem.setOnAction(e -> {
            String tag = tagList.getSelectionModel().getSelectedItem();
            if(tag == null) return;
            try{
                gitService.deleteTag(tag);
                tagList.setItems(FXCollections.observableArrayList(gitService.getTags()));
                statusLabel.setText("Deleted tag: " + tag);
            }catch (Exception ex){
                statusLabel.setText("Error: " + ex.getMessage());
            }
        });
        tagMenu.getItems().add(deleteTagItem);
        tagList.setContextMenu(tagMenu);

        ContextMenu commitMenu = new ContextMenu();
        MenuItem softReset = new MenuItem("Reset → Soft");
        MenuItem mixedReset = new MenuItem("Reset → Mixed");
        MenuItem hardReset = new MenuItem("Reset → Hard");

        softReset.setOnAction(e -> {
            CommitInfo commit = commitTable.getSelectionModel().getSelectedItem();
            if (commit == null) return;
            try {
                gitService.reset(commit.fullHash(), ResetCommand.ResetType.SOFT);
                handleRefresh();
                statusLabel.setText("Soft reset to: " + commit.shortHash());
            } catch (Exception ex) {
                statusLabel.setText("Error: " + ex.getMessage());
            }
        });

        mixedReset.setOnAction(e -> {
            CommitInfo commit = commitTable.getSelectionModel().getSelectedItem();
            if (commit == null) return;
            try {
                gitService.reset(commit.fullHash(), ResetCommand.ResetType.MIXED);
                handleRefresh();
                statusLabel.setText("Mixed reset to: " + commit.shortHash());
            } catch (Exception ex) {
                statusLabel.setText("Error: " + ex.getMessage());
            }
        });

        hardReset.setOnAction(e -> {
            CommitInfo commit = commitTable.getSelectionModel().getSelectedItem();
            if (commit == null) return;
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Hard Reset");
            confirm.setHeaderText("Hard reset će trajno obrisati sve promjene.");
            confirm.setContentText("Jesi li siguran?");
            confirm.showAndWait().ifPresent(btn -> {
                if (btn == ButtonType.OK) {
                    try {
                        gitService.reset(commit.fullHash(), ResetCommand.ResetType.HARD);
                        handleRefresh();
                        statusLabel.setText("Hard reset to: " + commit.shortHash());
                    } catch (Exception ex) {
                        statusLabel.setText("Error: " + ex.getMessage());
                    }
                }
            });
        });

        MenuItem revertItem = new MenuItem("Revert Commit");
        revertItem.setOnAction(e -> {
            CommitInfo commit = commitTable.getSelectionModel().getSelectedItem();
            if (commit == null) return;
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Revert Commit");
            confirm.setHeaderText("Napraviće se novi commit koji poništava: " + commit.shortHash());
            confirm.setContentText("Nastavi?");
            confirm.showAndWait().ifPresent(btn -> {
                if (btn == ButtonType.OK) {
                    try {
                        gitService.revert(commit.fullHash());
                        handleRefresh();
                        statusLabel.setText("Reverted: " + commit.shortHash());
                    } catch (Exception ex) {
                        statusLabel.setText("Error: " + ex.getMessage());
                    }
                }
            });
        });
        commitMenu.getItems().addAll(softReset, mixedReset, hardReset, revertItem);
        commitTable.setContextMenu(commitMenu);

        setRepoLoaded(false);
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
            setRepoLoaded(true);
            try {
                tagList.setItems(FXCollections.observableArrayList(gitService.getTags()));
            } catch (Exception ignored) {}

            try{
                recentRepos.add(dir.getAbsolutePath());
                refreshRecentMenu();
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

   @FXML
   private void handleShowChanges(){
        try{
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/githive/views/changes.fxml"));
            Scene scene = new Scene(loader.load(), 700, 500);
            ChangesController ctrl = loader.getController();
            ctrl.setGitService(gitService);
            ctrl.setOnCommitSuccess(msg -> {
                handleRefresh();
                statusLabel.setText("Commited: " + msg);
            });
            Stage stage = new Stage();
            stage.setTitle("Changes");
            stage.setScene(scene);
            stage.show();
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

   @FXML
    private void handlePull(){
        if(!gitService.isLoaded()) return;
        askCredentialsAndRun(false);
   }

   @FXML
    private void handlePush(){
        if(!gitService.isLoaded()) return;
        askCredentialsAndRun(true);
   }

   private void askCredentialsAndRun(boolean isPush){
        TextInputDialog userDialog = new TextInputDialog();
        userDialog.setTitle(isPush ? "Push" : "Pull");
        userDialog.setHeaderText("GitHub username:");
        userDialog.setContentText("Username:");
        Optional<String> username = userDialog.showAndWait();
        if(username.isEmpty()) return;

        TextInputDialog tokenDialog = new TextInputDialog();
        tokenDialog.setTitle(isPush ? "Push" : "Pull");
        tokenDialog.setHeaderText("Personal Access Token (PAT):");
        tokenDialog.setContentText("Token:");
        Optional<String> token = tokenDialog.showAndWait();
        if(token.isEmpty()) return;

        gitService.setCredentials(username.get(), token.get());
        try{
            if (isPush) {
                gitService.push();
                handleRefresh();
                statusLabel.setText("Push successful.");
            }
            else{
                gitService.pull();
                statusLabel.setText("Pull successful");
            }
        }catch(Exception e){
            statusLabel.setText("Error: " + e.getMessage());
        }
   }

   @FXML
    private void handleNewBranch(){
        if(!gitService.isLoaded()) return;
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("New Branch");
        dialog.setHeaderText("Enter branch name:");
        dialog.setContentText("Name:");
        dialog.showAndWait().ifPresent(name -> {
            try{
                gitService.createBranch(name);
                branchList.setItems(FXCollections.observableArrayList(gitService.getBranches()));
                statusLabel.setText("Branch created: " + name);
            }catch (Exception e){
                statusLabel.setText("Error: " + e.getMessage());
            }
        });
   }

   @FXML
    private void handleStashSave(){
        if(!gitService.isLoaded()) return;
        try{
            gitService.stashSave();
            statusLabel.setText("Changes stashed.");
        }catch (Exception e){
            statusLabel.setText("Error: " + e.getMessage());
        }
   }

   @FXML
   private void handleStashPop() {
       if (!gitService.isLoaded()) return;
       try {
           gitService.stashPop();
           statusLabel.setText("Stash applied.");
       } catch (Exception e) {
           statusLabel.setText("Error: " + e.getMessage());
       }
   }

   @FXML
    private void handleStashList(){
        if(!gitService.isLoaded()) return;
        try{
            List<String> stashes= gitService.stashList();
            String msg = stashes.isEmpty() ? "No stashes." : String.join("\n", stashes);
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Stash List");
            alert.setHeaderText(null);
            alert.setContentText(msg);
            alert.showAndWait();
        }catch (Exception e){
            statusLabel.setText("Error: " + e.getMessage());
        }
   }

   @FXML
    private void handleNewTag(){
        if(!gitService.isLoaded()) return;
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("New Tag");
        dialog.setHeaderText("Tag current commit:");
        dialog.setContentText("Tag name");
        dialog.showAndWait().ifPresent(name -> {
            try{
                gitService.createTag(name);
                tagList.setItems(FXCollections.observableArrayList(gitService.getTags()));
                statusLabel.setText("Tag created: " + name);
            }catch (Exception e){
                statusLabel.setText("Error: " + e.getMessage());
            }
        });
   }

   @FXML
    private void handleInitRepo(){
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select folder for new repository");
        File dir  = chooser.showDialog(openRepoBtn.getScene().getWindow());
        if(dir == null) return;
        try{
            gitService.initRepo(dir);
            loadRepository(dir);
            statusLabel.setText("Initialized new repo: " + dir.getName());
        }catch (Exception e){
            statusLabel.setText("Error: " + e.getMessage());
        }
   }

   @FXML
    private void handleCloneRepo(){
        TextInputDialog urlDialog = new TextInputDialog();
        urlDialog.setTitle("Clone repository");
        urlDialog.setHeaderText("Enter repository URL:");
        urlDialog.setContentText("URL:");
        Optional<String> url = urlDialog.showAndWait();
        if(url.isEmpty()) return;

        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select destination folder");
        File dir = chooser.showDialog(openRepoBtn.getScene().getWindow());
        if(dir == null) return;

        statusLabel.setText("Cloning");

       Task<Void> task = new Task<Void>() {
           @Override
           protected Void call() throws Exception {
               gitService.cloneRepo(url.get(), dir);
               return null;
           }
       };
       task.setOnSucceeded(e -> {
           loadRepository(dir);
           statusLabel.setText("Cloned: " + dir.getName());
       });
       task.setOnFailed(e -> {
           statusLabel.setText("Error: " + task.getException().getMessage());
       });
       new Thread(task).start();
   }

    @FXML
    private void handleConfig() {
        if (!gitService.isLoaded()) {
            statusLabel.setText("No repository loaded.");
            return;
        }
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Git Config");
        dialog.setHeaderText("Local repository config:");

        javafx.scene.control.TextField nameField = new javafx.scene.control.TextField(gitService.getConfigName());
        javafx.scene.control.TextField emailField = new javafx.scene.control.TextField(gitService.getConfigEmail());

        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.add(new Label("Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Email:"), 0, 1);
        grid.add(emailField, 1, 1);
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    gitService.setConfig(nameField.getText(), emailField.getText());
                    statusLabel.setText("Config saved.");
                } catch (IOException e) {
                    statusLabel.setText("Error: " + e.getMessage());
                }
            }
        });
    }

    private void refreshRecentMenu(){
        List<String> recent = recentRepos.load();
        recentMenu.getItems().clear();
        if(recent.isEmpty()){
            recentMenu.getItems().add(new MenuItem("No recent repositories"));
            return;
        }
        for(String path : recent){
            Label label = new Label(path);
            label.setMinWidth(220);
            Button removeBtn = new Button("X");
            removeBtn.setStyle("-fx-font-size: 10; -fx-padding: 0 5; -fx-cursor: hand;");
            removeBtn.setOnAction(e -> {
                e.consume();
                try{
                    recentRepos.remove(path);
                }catch (Exception ignored) {}
                refreshRecentMenu();
            });
            HBox box = new HBox(10, label, removeBtn);
            box.setAlignment(Pos.CENTER_LEFT);
            MenuItem item = new MenuItem();
            item.setGraphic(box);
            item.setOnAction(e -> loadRepository(new File(path)));
            recentMenu.getItems().add(item);
        }
    }

    private void setRepoLoaded(boolean loaded) {
        refreshBtn.setDisable(!loaded);
        changesBtn.setDisable(!loaded);
        pullBtn.setDisable(!loaded);
        pushBtn.setDisable(!loaded);
        configBtn.setDisable(!loaded);
        newBranchBtn.setDisable(!loaded);
        newTagBtn.setDisable(!loaded);
        stashMenu.setDisable(!loaded);
        fetchBtn.setDisable(!loaded);
    }

    @FXML private void handleFetch(){
        if(!gitService.isLoaded()) return;
        askCredentialsAndFetch();
    }

    private void askCredentialsAndFetch() {
        TextInputDialog userDialog = new TextInputDialog();
        userDialog.setTitle("Fetch");
        userDialog.setHeaderText("GitHub username:");
        userDialog.setContentText("Username:");
        Optional<String> username = userDialog.showAndWait();
        if (username.isEmpty()) return;

        TextInputDialog tokenDialog = new TextInputDialog();
        tokenDialog.setTitle("Fetch");
        tokenDialog.setHeaderText("Personal Access Token (PAT):");
        tokenDialog.setContentText("Token:");
        Optional<String> token = tokenDialog.showAndWait();
        if (token.isEmpty()) return;

        gitService.setCredentials(username.get(), token.get());
        statusLabel.setText("Fetching...");

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                gitService.fetch();
                return null;
            }
        };
        task.setOnSucceeded(e -> statusLabel.setText("Fetch successful."));
        task.setOnFailed(e -> statusLabel.setText("Error: " + task.getException().getMessage()));
        new Thread(task).start();
    }

}
