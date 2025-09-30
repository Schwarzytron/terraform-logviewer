package ru.konkurst1.ekb.terraform_logviewer.model;

public record LogEntry(
    String timestamp,
    String level,
    String message,
    String resource,
    String terraformVersion
) {}