package ru.konkurst1.ekb.terraform_logviewer.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.DateFormat;

import java.time.Instant;

@Getter
@Setter
@Document(indexName = "log_entries")
public class LogEntry {
    @Id
    private String id;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String rawMessage;

    @Field(type = FieldType.Date, format = DateFormat.date_time)
    private Instant timestamp;

    @Field(type = FieldType.Keyword)
    private LogLevel level;

    @Field(type = FieldType.Keyword)
    private String section;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String message;

    @Field(type = FieldType.Boolean)
    private Boolean hasJson;

    // Fields for error handling
    @Field(type = FieldType.Boolean)
    private Boolean parsingError = false;

    @Field(type = FieldType.Text)
    private String parsingErrorMessage;

    @Field(type = FieldType.Integer)
    private Integer lineNumber;

    @Field(type = FieldType.Keyword)
    private String logFileId;

    @Field(type = FieldType.Keyword)
    private String tfResourceType;

    @Field(type = FieldType.Keyword)
    private String tfReqId;

    @Field(type = FieldType.Boolean)
    private Boolean isRead = false;

    @Field(type = FieldType.Keyword)
    private String requestType; // "request", "response", or null

    @Field(type = FieldType.Text, analyzer = "standard")
    private String jsonBody;

    public LogEntry() {}

    public LogEntry(String id, String rawMessage, Instant timestamp, LogLevel level,
                    String section, String message, Boolean hasJson,
                    Boolean parsingError, String parsingErrorMessage,
                    Integer lineNumber, String tfResourceType, String tfReqId,
                    String requestType, String jsonBody, String logFileId) {
        this.id = id;
        this.rawMessage = rawMessage;
        this.timestamp = timestamp;
        this.level = level;
        this.section = section;
        this.message = message;
        this.hasJson = hasJson;
        this.parsingError = parsingError;
        this.parsingErrorMessage = parsingErrorMessage;
        this.lineNumber = lineNumber;
        this.tfResourceType = tfResourceType;
        this.tfReqId = tfReqId;
        this.requestType = requestType;
        this.jsonBody = jsonBody;
        this.logFileId = logFileId;
        this.isRead = false;
    }

    // Constructor without ID for new entries (ID will be generated)
    public LogEntry(String rawMessage, Instant timestamp, LogLevel level,
                    String section, String message, Boolean hasJson,
                    Boolean parsingError, String parsingErrorMessage,
                    Integer lineNumber, String tfResourceType, String tfReqId,
                    String requestType, String jsonBody, String logFileId) {
        this.rawMessage = rawMessage;
        this.timestamp = timestamp;
        this.level = level;
        this.section = section;
        this.message = message;
        this.hasJson = hasJson;
        this.parsingError = parsingError;
        this.parsingErrorMessage = parsingErrorMessage;
        this.lineNumber = lineNumber;
        this.tfResourceType = tfResourceType;
        this.tfReqId = tfReqId;
        this.requestType = requestType;
        this.jsonBody = jsonBody;
        this.logFileId = logFileId;
        this.isRead = false;
    }
}