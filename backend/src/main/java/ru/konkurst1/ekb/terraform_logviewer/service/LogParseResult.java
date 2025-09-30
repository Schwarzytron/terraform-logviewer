package ru.konkurst1.ekb.terraform_logviewer.service;

import lombok.Getter;
import ru.konkurst1.ekb.terraform_logviewer.model.LogEntry;

import java.util.List;
@Getter
public class LogParseResult {
    private final List<LogEntry> entries;
    private final List<ParsingError> errors;
    
    public LogParseResult(List<LogEntry> entries, List<ParsingError> errors) {
        this.entries = entries;
        this.errors = errors;
    }
}