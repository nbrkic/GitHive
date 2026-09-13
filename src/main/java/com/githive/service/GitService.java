package com.githive.service;

import com.githive.model.CommitInfo;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
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
        for (RevCommit c : git.log().setMaxCount(limit).call()) {
            List<String> parents = new ArrayList<>();
            for (RevCommit p : c.getParents()) parents.add(p.getName());
            result.add(new CommitInfo(c.abbreviate(7).name(), c.getName(), c.getShortMessage(), c.getAuthorIdent().getName(), DATE_FMT.format(new Date((long) c.getCommitTime() * 1000)), parents));
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

    private AbstractTreeIterator treeParser(Repository repo, RevCommit commit) throws Exception {
        try (RevWalk walk = new RevWalk(repo);
             ObjectReader reader = repo.newObjectReader()) {
            RevTree tree = walk.parseTree(commit.getTree().getId());
            CanonicalTreeParser parse = new CanonicalTreeParser();
            parse.reset(reader, tree.getId());
            return parse;
        }
    }
}