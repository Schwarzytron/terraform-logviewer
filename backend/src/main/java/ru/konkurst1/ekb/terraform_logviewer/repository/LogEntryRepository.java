package ru.konkurst1.ekb.terraform_logviewer.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.konkurst1.ekb.terraform_logviewer.dto.LogFileInfo;
import ru.konkurst1.ekb.terraform_logviewer.model.LogEntry;

import java.util.List;

@Repository
public interface LogEntryRepository extends JpaRepository<LogEntry, Long> {
    
    Page<LogEntry> findByLogFileId(String logFileId, Pageable pageable);
    
    Page<LogEntry> findByLogFileIdAndParsingError(String logFileId, Boolean parsingError, Pageable pageable);
    
    Page<LogEntry> findByLogFileIdAndMessageContainingIgnoreCase(String logFileId, String query, Pageable pageable);
    
    @Query("SELECT new ru.konkursteleb.terraformlogviewer.dto.LogFileInfo(" +
           "l.logFileId, MAX(l.rawMessage), MAX(l.timestamp), COUNT(l), SUM(CASE WHEN l.parsingError = true THEN 1 ELSE 0 END)) " +
           "FROM LogEntry l GROUP BY l.logFileId")
    List<LogFileInfo> findLogFileInfo();
}