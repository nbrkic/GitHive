package com.githive.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

public class CredentialsService {
    private static final Path STORAGE = Path.of(System.getProperty("user.home"), ".githive_credentials");

    public void save(String username, String token) throws IOException {
        String encoded = Base64.getEncoder().encodeToString((username + ":" + token).getBytes());
        Files.writeString(STORAGE, encoded);
    }

    public String[] load() {
        try {
            if (!Files.exists(STORAGE)) return null;
            String decoded = new String(Base64.getDecoder().decode(Files.readString(STORAGE).trim()));
            String[] parts = decoded.split(":", 2);
            return parts.length == 2 ? parts : null;
        } catch (Exception e) {
            return null;
        }
    }

    public void clear() throws IOException {
        Files.deleteIfExists(STORAGE);
    }
}
