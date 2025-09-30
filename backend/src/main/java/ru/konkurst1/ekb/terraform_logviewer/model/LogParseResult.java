package ru.konkurst1.ekb.terraform_logviewer.model;

import ru.konkurst1.ekb.terraform_logviewer.service.ParsingError;

import java.util.List;

public record LogParseResult(List<LogEntry> entries, List<ParsingError> errors) {
}