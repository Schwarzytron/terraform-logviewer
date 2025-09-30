package ru.konkurst1.ekb.terraform_logviewer.service;

import org.springframework.stereotype.Service;
import ru.konkurst1.ekb.terraform_logviewer.model.LogEntry;
import ru.konkurst1.ekb.terraform_logviewer.model.LogLevel;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class LogParserService {
    
    // Паттерны для определения секций
    private final Pattern PLAN_START = Pattern.compile(".*Terraform will perform.*|.*terraform plan.*", Pattern.CASE_INSENSITIVE);
    private final Pattern APPLY_START = Pattern.compile(".*terraform apply.*|.*Applying.*", Pattern.CASE_INSENSITIVE);
    private final Pattern SECTION_END = Pattern.compile(".*Apply complete!.*|.*Plan:.*");
    
    // Форматы временных меток
    private final Pattern[] TIMESTAMP_PATTERNS = {
        Pattern.compile("(\\d{4}-\\d{2}-\\d{2}[T\\s]\\d{2}:\\d{2}:\\d{2})"),
        Pattern.compile("(\\d{2}:\\d{2}:\\d{2})"),
        Pattern.compile("\\[(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2})\\]")
    };
    
    // Ключевые слова для уровней
    private final Map<LogLevel, List<String>> LEVEL_KEYWORDS = Map.of(
        LogLevel.ERROR, Arrays.asList("error", "failed", "failure", "exception"),
        LogLevel.WARN, Arrays.asList("warn", "warning", "deprecated"),
        LogLevel.INFO, Arrays.asList("info", "applying", "creating", "destroying"),
        LogLevel.DEBUG, Arrays.asList("debug", "trace", "verbose")
    );
    
    private String currentSection = "other";
    private Instant lastTimestamp = Instant.now();
    
    public List<LogEntry> parseLogs(List<String> rawLines) {
        List<LogEntry> entries = new ArrayList<>();
        List<String> context = new ArrayList<>();
        
        for (String line : rawLines) {
            if (line.trim().isEmpty()) continue;
            
            // Определяем секцию
            currentSection = detectSection(line, context);
            
            // Извлекаем timestamp
            Instant timestamp = extractTimestamp(line);
            if (timestamp == null) {
                timestamp = lastTimestamp;
            } else {
                lastTimestamp = timestamp;
            }
            
            // Определяем уровень
            LogLevel level = detectLogLevel(line);
            
            // Проверяем на наличие JSON
            boolean hasJson = line.contains("{") && line.contains("}");
            
            LogEntry entry = new LogEntry(
                timestamp, level, currentSection, line,
                    extractMessage(line), hasJson
            );
            
            entries.add(entry);
            context.add(line);
            
            // Сохраняем контекст (последние 5 строк)
            if (context.size() > 5) {
                context.removeFirst();
            }
        }
        
        return entries;
    }
    
    private String detectSection(String line, List<String> context) {
        if (PLAN_START.matcher(line).matches()) {
            return "plan";
        } else if (APPLY_START.matcher(line).matches()) {
            return "apply";
        } else if (SECTION_END.matcher(line).matches()) {
            return "other";
        }
        return currentSection;
    }
    
    private Instant extractTimestamp(String line) {
        for (Pattern pattern : TIMESTAMP_PATTERNS) {
            Matcher matcher = pattern.matcher(line);
            if (matcher.find()) {
                String timestampStr = matcher.group(1);
                try {
                    // Пробуем разные парсеры для timestamp
                    return parseTimestamp(timestampStr);
                } catch (Exception e) {
                    // Логируем и продолжаем
                }
            }
        }
        return null;
    }
    
    static public Instant parseTimestamp(String timestampStr) {
        try {
            return Instant.parse(timestampStr.replace(" ", "T") + "Z");
        } catch (Exception e) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                LocalDateTime localDateTime = LocalDateTime.parse(timestampStr, formatter);
                return localDateTime.atZone(ZoneId.systemDefault()).toInstant();
            } catch (Exception ex) {
                return Instant.now();
            }
        }
    }
    
    private LogLevel detectLogLevel(String line) {
        String lowerLine = line.toLowerCase();
        
        for (Map.Entry<LogLevel, List<String>> entry : LEVEL_KEYWORDS.entrySet()) {
            for (String keyword : entry.getValue()) {
                if (lowerLine.contains(keyword)) {
                    return entry.getKey();
                }
            }
        }
        
        return LogLevel.INFO; // По умолчанию
    }
    
    private String extractMessage(String line) {
        // Убираем timestamp из сообщения если есть
        for (Pattern pattern : TIMESTAMP_PATTERNS) {
            Matcher matcher = pattern.matcher(line);
            if (matcher.find()) {
                return line.replace(matcher.group(0), "").trim();
            }
        }
        return line.trim();
    }
}