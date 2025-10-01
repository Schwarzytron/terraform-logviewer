package ru.konkurst1.ekb.terraform_logviewer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.konkurst1.ekb.terraform_logviewer.model.LogEntry;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class LogParserService {
    private static final Logger logger = LoggerFactory.getLogger(LogParserService.class);

    @Autowired
    private SectionDetectionService sectionDetectionService;
    @Autowired
    private TerraformContextEnricher contextEnricher;
    public List<LogEntry> parseAndEnrichLogs(List<String> rawLines, String logFileId) {
        // 1. Базовый парсинг JSON
        List<LogEntry> entries = parseJsonLogs(rawLines, logFileId);

        // 2. Обогащение бизнес-логикой (секции)
        entries = sectionDetectionService.detectSections(entries);

        // 3. Дополнительное обогащение (группировка по tf_req_id и т.д.)
        entries = contextEnricher.enrichWithTerraformContext(entries);

        return entries;
    }

    private List<LogEntry> parseJsonLogs(List<String> rawLines, String logFileId) {
        return rawLines.stream()
                .map(line -> parseSingleJsonLine(line, logFileId))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private LogEntry parseSingleJsonLine(String rawLine, String logFileId) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode json = mapper.readTree(rawLine);

            LogEntry entry = new LogEntry();
            entry.setRawJson(mapper.readValue(rawLine, Object.class));

            // Извлекаем базовые поля
            extractBasicFields(json, entry);

            // Извлекаем Terraform-специфичные поля
            extractTerraformFields(json, entry);

            return entry;

        } catch (Exception e) {
            logger.warn("Failed to parse JSON line: {}", e.getMessage());
            return null;
        }
    }

    private void extractBasicFields(JsonNode json, LogEntry entry) {
        // Обязательные поля из JSON логов Terraform
        if (json.has("@timestamp")) {
            try {
                String timestampStr = json.get("@timestamp").asText();
                // Конвертируем формат "2025-09-09T15:47:33.319437+03:00" в Instant
                timestampStr = timestampStr.replace("+03:00", "Z");
                entry.setTimestamp(Instant.parse(timestampStr));
            } catch (Exception e) {
                logger.warn("Failed to parse timestamp: {}", json.get("@timestamp").asText());
                entry.setTimestamp(Instant.now());
            }
        }

        if (json.has("@level")) {
            String levelStr = json.get("@level").asText().toUpperCase();
            try {
                entry.setLevel(levelStr); // Сохраняем как строку "INFO", "DEBUG" и т.д.
            } catch (Exception e) {
                entry.setLevel("INFO"); // fallback
            }
        }

        if (json.has("@message")) {
            entry.setMessage(json.get("@message").asText());
        }
    }

    private void extractTerraformFields(JsonNode json, LogEntry entry) {
        // Terraform-specific поля для группировки и фильтрации
        if (json.has("tf_resource_type")) {
            entry.setTfResourceType(json.get("tf_resource_type").asText());
        }

        if (json.has("tf_req_id")) {
            entry.setTfReqId(json.get("tf_req_id").asText());
        }

        if (json.has("tf_rpc")) {
            entry.setRequestType("request");
        } else if (json.has("tf_proto_version")) {
            entry.setRequestType("response");
        }

        // Дополнительные поля которые могут быть полезны
        if (json.has("@module")) {
            entry.setModule(json.get("@module").asText());
        }

        if (json.has("tf_provider_addr")) {
            entry.setTfProviderAddr(json.get("tf_provider_addr").asText());
        }
    }
}