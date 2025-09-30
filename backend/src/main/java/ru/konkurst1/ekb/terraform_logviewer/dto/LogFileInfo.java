package ru.konkurst1.ekb.terraform_logviewer.dto;

import lombok.Getter;

import java.time.Instant;

@Getter
public class LogFileInfo {
    private String id;
    private String filename;
    private Instant uploadTime;
    private Integer entryCount;
    private Integer errorCount;
    public LogFileInfo(String id, String filename, Instant uploadTime, 
                      Integer entryCount, Integer errorCount) {
        this.id = id;
        this.filename = filename;
        this.uploadTime = uploadTime;
        this.entryCount = entryCount;
        this.errorCount = errorCount;
    }

}