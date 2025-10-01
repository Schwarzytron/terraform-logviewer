package ru.konkurst1.ekb.terraform_logviewer.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.Instant;

@Getter
@Setter
@Document(indexName = "terraform_logs")
public class LogEntry {
    @Id
    private String id;

    @Field(type = FieldType.Date)
    private Instant timestamp;

    @Field(type = FieldType.Keyword)
    private String level;

    @Field(type = FieldType.Text)
    private String message;

    @Field(type = FieldType.Keyword)
    private String logFileId;

    @Field(type = FieldType.Keyword)
    private String section; // "plan", "apply", "other"

    // Terraform-specific fields
    @Field(type = FieldType.Keyword)
    private String tfResourceType;

    @Field(type = FieldType.Keyword)
    private String tfReqId;

    @Field(type = FieldType.Keyword)
    private String requestType; // "request", "response"

    @Field(type = FieldType.Keyword)
    private String module;

    @Field(type = FieldType.Keyword)
    private String tfProviderAddr;

    // Raw JSON для полного доступа к данным
    @Field(type = FieldType.Object, enabled = true)
    private Object rawJson;

    @Field(type = FieldType.Integer)
    private Integer lineNumber;

    @Field(type = FieldType.Long)
    private Long requestDurationMs;

    @Field(type = FieldType.Keyword)
    private String requestStatus; // "success", "failed", "warning"

    @Field(type = FieldType.Keyword)
    private String operationType; // "create", "update", "delete", "read"

    @Field(type = FieldType.Integer)
    private Integer severityScore;

    @Field(type = FieldType.Boolean)
    private Boolean isPartOfRequestChain;

    @Field(type = FieldType.Boolean)
    private Boolean isRead = false;
}