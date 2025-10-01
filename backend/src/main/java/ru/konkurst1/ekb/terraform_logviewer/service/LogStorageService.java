package ru.konkurst1.ekb.terraform_logviewer.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import ru.konkurst1.ekb.terraform_logviewer.dto.LogFileInfo;
import ru.konkurst1.ekb.terraform_logviewer.model.LogEntry;
import ru.konkurst1.ekb.terraform_logviewer.model.LogLevel;
import ru.konkurst1.ekb.terraform_logviewer.repository.LogEntryRepository;

import java.util.List;

@Service
public class LogStorageService {
    private static final Logger logger = LoggerFactory.getLogger(LogStorageService.class);

    @Autowired
    private LogEntryRepository logEntryRepository;

    public void saveEntries(List<LogEntry> entries) {
        logger.info("Saving {} log entries to Elasticsearch", entries.size());
        logEntryRepository.saveAll(entries);
        logger.info("Successfully saved {} entries", entries.size());
    }

    public Page<LogEntry> findEntries(String logFileId, int page, int size,
                                      LogLevel level, String section, Boolean hasErrors) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").ascending());

        return logEntryRepository.findByLogFileIdAndFilters(logFileId, level, section, hasErrors, pageable);
    }

    public Long getUnreadCount(String logFileId) {
        return logEntryRepository.countByLogFileIdAndIsRead(logFileId, false);
    }

    public Page<LogEntry> searchEntries(String logFileId, String query, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").ascending());
        return logEntryRepository.findByLogFileIdAndMessageContainingIgnoreCase(logFileId, query, pageable);
    }

    public List<LogFileInfo> getLogFiles() {
        return logEntryRepository.findLogFileInfo();
    }

    public List<String> getDistinctResourceTypes() {
        return logEntryRepository.findDistinctTfResourceTypes();
    }

    public List<String> getDistinctRequestIds() {
        return logEntryRepository.findDistinctTfReqIds();
    }
}