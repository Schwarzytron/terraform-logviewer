package ru.konkurst1.ekb.terraform_logviewer.service;

import org.springframework.stereotype.Service;
import ru.konkurst1.ekb.terraform_logviewer.model.LogEntry;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TerraformContextEnricher {
    
    public List<LogEntry> enrichWithTerraformContext(List<LogEntry> entries) {
        // 1. Группировка запросов-ответов по tf_req_id
        Map<String, List<LogEntry>> requests = groupByRequestId(entries);
        
        // 2. Вычисление длительности запросов
        calculateRequestDurations(requests);
        
        // 3. Определение статуса операций
        determineOperationStatus(requests);
        
        // 4. Обогащение дополнительными метаданными
        return enrichWithMetadata(entries, requests);
    }
    
    private Map<String, List<LogEntry>> groupByRequestId(List<LogEntry> entries) {
        return entries.stream()
            .filter(entry -> entry.getTfReqId() != null && !entry.getTfReqId().isEmpty())
            .collect(Collectors.groupingBy(LogEntry::getTfReqId));
    }
    
    private void calculateRequestDurations(Map<String, List<LogEntry>> requests) {
        for (Map.Entry<String, List<LogEntry>> requestEntry : requests.entrySet()) {
            List<LogEntry> requestChain = requestEntry.getValue();
            
            if (requestChain.size() > 1) {
                // Сортируем по времени
                requestChain.sort(Comparator.comparing(LogEntry::getTimestamp));
                
                Instant startTime = requestChain.get(0).getTimestamp();
                Instant endTime = requestChain.get(requestChain.size() - 1).getTimestamp();
                long durationMs = Duration.between(startTime, endTime).toMillis();
                
                // Добавляем длительность ко всем записям цепочки
                requestChain.forEach(entry -> {
                    // Можно сохранить в отдельное поле или в rawJson
                    if (entry.getRawJson() instanceof Map) {
                        ((Map<String, Object>) entry.getRawJson()).put("request_duration_ms", durationMs);
                    }
                });
            }
        }
    }
    
    private void determineOperationStatus(Map<String, List<LogEntry>> requests) {
        for (Map.Entry<String, List<LogEntry>> requestEntry : requests.entrySet()) {
            List<LogEntry> requestChain = requestEntry.getValue();
            
            // Определяем статус по наличию ошибок в цепочке
            boolean hasErrors = requestChain.stream()
                .anyMatch(entry -> "ERROR".equals(entry.getLevel()));
                
            boolean hasWarnings = requestChain.stream()
                .anyMatch(entry -> "WARN".equals(entry.getLevel()));
            
            String status = hasErrors ? "failed" : hasWarnings ? "warning" : "success";
            
            // Добавляем статус ко всем записям цепочки
            requestChain.forEach(entry -> {
                if (entry.getRawJson() instanceof Map) {
                    ((Map<String, Object>) entry.getRawJson()).put("request_status", status);
                }
            });
        }
    }
    
    private List<LogEntry> enrichWithMetadata(List<LogEntry> entries, Map<String, List<LogEntry>> requests) {
        // Дополнительное обогащение
        for (LogEntry entry : entries) {
            // Определяем тип операции на основе resource_type
            if (entry.getTfResourceType() != null) {
                String operationType = determineOperationType(entry.getTfResourceType(), entry.getMessage());
                if (entry.getRawJson() instanceof Map) {
                    ((Map<String, Object>) entry.getRawJson()).put("operation_type", operationType);
                }
            }
            
            // Вычисляем "важность" записи на основе уровня и контекста
            int severity = calculateSeverity(entry);
            if (entry.getRawJson() instanceof Map) {
                ((Map<String, Object>) entry.getRawJson()).put("severity_score", severity);
            }
        }
        
        return entries;
    }
    
    private String determineOperationType(String resourceType, String message) {
        if (message != null) {
            if (message.toLowerCase().contains("creating") || message.contains("Create")) {
                return "create";
            } else if (message.toLowerCase().contains("updating") || message.contains("Update")) {
                return "update";
            } else if (message.toLowerCase().contains("destroying") || message.contains("Destroy")) {
                return "delete";
            } else if (message.toLowerCase().contains("reading") || message.contains("Read")) {
                return "read";
            }
        }
        return "unknown";
    }
    
    private int calculateSeverity(LogEntry entry) {
        int score = 0;
        
        // Базовый score по уровню логирования
        switch (entry.getLevel()) {
            case "ERROR": score += 10; break;
            case "WARN": score += 5; break;
            case "INFO": score += 1; break;
            case "DEBUG": score += 0; break;
        }
        
        // Дополнительные факторы
        if (entry.getTfResourceType() != null && entry.getTfResourceType().contains("compute")) {
            score += 2; // Compute ресурсы обычно более критичны
        }
        
        if (entry.getMessage() != null) {
            if (entry.getMessage().toLowerCase().contains("failed") || 
                entry.getMessage().toLowerCase().contains("error")) {
                score += 3;
            }
        }
        
        return score;
    }
}