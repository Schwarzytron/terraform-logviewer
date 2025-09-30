package ru.konkurst1.ekb.terraform_logviewer.service;

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
    
    @Autowired
    private LogEntryRepository logEntryRepository;
    
    public void saveEntries(List<LogEntry> entries) {
        logEntryRepository.saveAll(entries);
    }
    
    public Page<LogEntry> findEntries(String logFileId, int page, int size,
                                      LogLevel level, String section, Boolean hasErrors) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("lineNumber").ascending());
        
        // Здесь будет сложный query с фильтрами
        // Пока простой вариант:
        if (hasErrors != null && hasErrors) {
            return logEntryRepository.findByLogFileIdAndParsingError(logFileId, true, pageable);
        }
        
        return logEntryRepository.findByLogFileId(logFileId, pageable);
    }
    
    public List<LogEntry> searchEntries(String logFileId, String query, int page, int size) {
        // Реализация поиска - можно через Elasticsearch или LIKE запросы
        return logEntryRepository.findByLogFileIdAndMessageContainingIgnoreCase(
            logFileId, query, PageRequest.of(page, size)
        ).getContent();
    }
    
    public List<LogFileInfo> getLogFiles() {
        // Группируем по logFileId и возвращаем информацию
        return logEntryRepository.findLogFileInfo();
    }
}