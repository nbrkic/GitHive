package com.githive.service;

import com.githive.model.CommitInfo;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.EmptyTreeIterator;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.dircache.DirCache;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class GitService {
    private Git git;
    private File repoDir;
    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    private UsernamePasswordCredentialsProvider credentials;

    public void open(File dir) throws IOException {
        if (git != null) {
            git.close();
        }
        git = Git.open(dir);
        this.repoDir = dir;
    }

    public boolean isLoaded() {
        return git != null;
    }

    public File getRepoDir() {
        return repoDir;
    }

    public List<String> getBranches() throws GitAPIException {
        List<String> names = new ArrayList<>();
        for (Ref ref : git.branchList().call()) {
            names.add(ref.getName().replace("refs/heads/", ""));
        }
        return names;
    }

    public List<CommitInfo> getCommits(int limit) throws GitAPIException {
        List<CommitInfo> result = new ArrayList<>();
        try {
            for (RevCommit c : git.log().setMaxCount(limit).call()) {
                List<String> parents = new ArrayList<>();
                for (RevCommit p : c.getParents()) parents.add(p.getName());
                result.add(new CommitInfo(c.abbreviate(7).name(), c.getName(), c.getShortMessage(), c.getAuthorIdent().getName(), DATE_FMT.format(new Date((long) c.getCommitTime() * 1000)), parents));
            }
        } catch (org.eclipse.jgit.api.errors.NoHeadException ignored) {
            // empty repo, no commits yet
        }
        return result;
    }


    public List<String> getChangedFiles(String fullHash) throws Exception {
        Repository repo = git.getRepository();
        try (RevWalk walk = new RevWalk(repo)) {
            RevCommit commit = walk.parseCommit(ObjectId.fromString(fullHash));
            AbstractTreeIterator oldTree = commit.getParentCount() > 0 ? treeParser(repo, walk.parseCommit(commit.getParent(0).getId())) : new EmptyTreeIterator();
            AbstractTreeIterator newTree = treeParser(repo, commit);

            try (DiffFormatter df = new DiffFormatter(null)) {
                df.setRepository(repo);
                List<String> files = new ArrayList<>();
                for (DiffEntry e : df.scan(oldTree, newTree)) {
                    String path = e.getChangeType() == DiffEntry.ChangeType.DELETE ? e.getOldPath() : e.getNewPath();
                    files.add(e.getChangeType().name().charAt(0) + "  " + path);
                }
                return files;
            }
        }
    }

    public String getFileDiff(String fullHash, String fileEntry) throws Exception {
        String path = fileEntry.length() > 3 ? fileEntry.substring(3) : fileEntry;
        Repository repo = git.getRepository();
        try (RevWalk walk = new RevWalk(repo);
             ByteArrayOutputStream out = new ByteArrayOutputStream();
             DiffFormatter df = new DiffFormatter(out)) {

            RevCommit commit = walk.parseCommit(ObjectId.fromString(fullHash));
            AbstractTreeIterator oldTree = commit.getParentCount() > 0 ? treeParser(repo, walk.parseCommit(commit.getParent(0).getId())) : new EmptyTreeIterator();
            AbstractTreeIterator newTree = treeParser(repo, commit);

            df.setRepository(repo);
            df.setContext(3);
            for (DiffEntry e : df.scan(oldTree, newTree)) {
                String entryPath = e.getChangeType() == DiffEntry.ChangeType.DELETE ? e.getOldPath() : e.getNewPath();
                if (entryPath.equals(path)) {
                    df.format(e);
                    df.flush();
                    return out.toString(StandardCharsets.UTF_8);
                }
            }
        }
        return "";
    }

    public Status getStatus() throws GitAPIException{
        return git.status().call();
    }

    public void stageFile(String path) throws GitAPIException{
        git.add().addFilepattern(path).call();
    }

    public void commit(String message) throws GitAPIException{
        git.commit().setMessage(message).call();
    }

    public void setCredentials(String username, String token){
        this.credentials = new UsernamePasswordCredentialsProvider(username, token);
    }

    public void push() throws GitAPIException{
        git.push().setCredentialsProvider(credentials).call();
    }

    public void pull() throws GitAPIException{
        git.pull().setCredentialsProvider(credentials).call();
    }

    public void checkoutBranch(String name) throws GitAPIException{
        git.checkout().setName(name).call();
    }

    public void createBranch(String name) throws GitAPIException {
        git.branchCreate().setName(name).call();
    }

    public void deleteBranch(String name) throws GitAPIException {
        git.branchDelete().setBranchNames(name).setForce(true).call();
    }

    public void stashSave() throws GitAPIException{
        git.stashCreate().call();
    }

    public void stashPop() throws  GitAPIException{
        git.stashApply().call();
        git.stashDrop().call();
    }

    public List<String> stashList() throws GitAPIException{
        List<String> result = new ArrayList<>();
        int i = 0;
        for(RevCommit c : git.stashList().call()){
            result.add("stash@{" + i++ + "} " + c.getShortMessage());
        }
        return result;
    }

    private AbstractTreeIterator treeParser(Repository repo, RevCommit commit) throws Exception {
        try (RevWalk walk = new RevWalk(repo);
             ObjectReader reader = repo.newObjectReader()) {
            RevTree tree = walk.parseTree(commit.getTree().getId());
            CanonicalTreeParser parse = new CanonicalTreeParser();
            parse.reset(reader, tree.getId());
            return parse;
        }
    }

    public MergeResult merge(String branchName) throws Exception{
        ObjectId branchId = git.getRepository().resolve(branchName);
        return git.merge().include(branchId).call();
    }

    public List<String> getTags() throws GitAPIException{
        List<String> names = new ArrayList<>();
        for(Ref ref : git.tagList().call()){
            names.add(ref.getName().replace("refs/tags/", ""));
        }
        return names;
    }

    public void createTag(String name) throws GitAPIException{
        git.tag().setName(name).call();
    }

    public void deleteTag(String name) throws GitAPIException{
        git.tagDelete().setTags(name).call();
    }

    public void initRepo(File directory) throws GitAPIException, IOException{
        if(git != null) git.close();
        git = Git.init().setDirectory(directory).call();
        this.repoDir = directory;
    }

    public void cloneRepo(String url, File directory) throws GitAPIException{
        if(git != null) git.close();
        git = Git.cloneRepository().setURI(url).setDirectory(directory).call();
        this.repoDir = directory;
    }

    public String getConfigName(){
        return git.getRepository().getConfig().getString("user", null, "name");
    }

    public String getConfigEmail(){
        return git.getRepository().getConfig().getString("user", null, "email");
    }

    public void setConfig(String name, String email) throws IOException{
        StoredConfig config = git.getRepository().getConfig();
        config.setString("user", null, "name", name);
        config.setString("user", null, "email", email);
        config.save();
    }

    public void unstageFile(String path) throws GitAPIException{
        git.reset().addPath(path).call();
    }

    public void discardChanges(String path) throws Exception{
        git.checkout().addPath(path).call();
    }

    public void fetch() throws GitAPIException{
        git.fetch().setCredentialsProvider(credentials).call();
    }

    public void addToGitIgnore(List<String> paths) throws IOException{
        File gitignore = new File(git.getRepository().getWorkTree(), ".gitignore");
        List<String> existing = gitignore.exists() ? new ArrayList<>(Files.readAllLines(gitignore.toPath())) : new ArrayList<>();
        for(String path : paths){
            if(!existing.contains(path)) existing.add(path);
        }
        Files.writeString(gitignore.toPath(), String.join("\n", existing) + "\n");
    }

    public String getLastCommitMessage(){
        try{
            return git.log().setMaxCount(1).call().iterator().next().getFullMessage();
        }catch (Exception e){
            return "";
        }
    }

    public void amendCommit(String message) throws GitAPIException{
        git.commit().setAmend(true).setMessage(message).call();
    }
}