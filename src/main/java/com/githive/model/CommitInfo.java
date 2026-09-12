package com.githive.model;

import java.util.List;

public record CommitInfo(
        String shortHash,
        String fullHash,
        String message,
        String author,
        String date,
        List<String> parentHashes
) {}
