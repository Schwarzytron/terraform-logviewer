package ru.konkurst1.ekb.terraform_logviewer.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ru.konkurst1.ekb.terraform_logviewer.model.LogEntry;
import ru.konkurst1.ekb.terraform_logviewer.model.LogLevel;

public interface LogEntryRepositoryCustom {
    Page<LogEntry> findByLogFileIdAndFilters(String logFileId, LogLevel level, String section, Boolean hasErrors, Pageable pageable);
}