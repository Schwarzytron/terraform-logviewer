package ru.konkurst1.ekb.terraform_logviewer.dto;

import lombok.Getter;
import lombok.Setter;
import ru.konkurst1.ekb.terraform_logviewer.model.LogLevel;

import java.time.Instant;

@Getter
@Setter
public class SearchFilters {
    private String logFileId;
    private String tfResourceType;
    private Instant timestampFrom;
    private Instant timestampTo;
    private LogLevel level;
    private String section;
    private String tfReqId;
    private Boolean onlyUnread = false;
    private String freeText;
    private Integer page = 0;
    private Integer size = 50;
}