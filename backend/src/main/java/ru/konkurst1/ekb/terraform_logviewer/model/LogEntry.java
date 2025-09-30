package ru.konkurst1.ekb.terraform_logviewer.model;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.Instant;
@Getter
@Entity
public class LogEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT")
    private String rawMessage;

    private Instant timestamp;

    @Enumerated(EnumType.STRING)
    private LogLevel level;

    private String section;

    @Column(columnDefinition = "TEXT")
    private String message;

    private Boolean hasJson;

    // Новые поля для обработки ошибок
    private Boolean parsingError = false;
    private String parsingErrorMessage;
    private Integer lineNumber;
    private String logFileId; // ID файла для группировки

    // constructors
    public LogEntry() {}

    public LogEntry(String rawMessage, Instant timestamp, LogLevel level,
                    String section, String message, Boolean hasJson,
                    Boolean parsingError, String parsingErrorMessage, Integer lineNumber) {
        this.rawMessage = rawMessage;
        this.timestamp = timestamp;
        this.level = level;
        this.section = section;
        this.message = message;
        this.hasJson = hasJson;
        this.parsingError = parsingError;
        this.parsingErrorMessage = parsingErrorMessage;
        this.lineNumber = lineNumber;
    }
}