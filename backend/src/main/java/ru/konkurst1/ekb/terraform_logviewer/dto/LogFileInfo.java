package ru.konkurst1.ekb.terraform_logviewer.dto;

import java.time.Instant;

public record LogFileInfo(String id, Instant uploadTime, Long entryCount, Long errorCount) {
}