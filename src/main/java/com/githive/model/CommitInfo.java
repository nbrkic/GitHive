package com.githive.model;

public record CommitInfo(
        String shortHash,
        String fullHash,
        String message,
        String author,
        String date
) {}
