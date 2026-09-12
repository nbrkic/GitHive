package com.githive.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class RecentReposService {
    private static final Path STORAGE = Path.of(System.getProperty("user.home"), ".githive_recent");
    private static final int MAX = 10;

    public void add(String path) throws IOException{
        List<String> repos = load();
        repos.remove(path);
        repos.add(0, path);
        if(repos.size() > MAX) repos = repos.subList(0, MAX);
        Files.writeString(STORAGE, String.join("\n", repos));
    }

    public List<String> load(){
        if(!Files.exists(STORAGE)) return new ArrayList<>();
        try{
            List<String> lines = new ArrayList<>(Files.readAllLines(STORAGE));
            lines.removeIf(String::isBlank);
            return lines;
        }catch (IOException e){
            return new ArrayList<>();
        }
    }
}
