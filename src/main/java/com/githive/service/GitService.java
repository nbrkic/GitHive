package com.githive.service;

import com.githive.model.CommitInfo;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;

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

    public void open(File dir) throws IOException {
        if (git != null) git.close();
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
            result.add(new CommitInfo(
                    c.abbreviate(7).name(),
                    c.getName(),
                    c.getShortMessage(),
                    c.getAuthorIdent().getName(),
                    DATE_FMT.format(new Date((long) c.getCommitTime() * 1000))
            ));
        }
        return result;
    }
}
