package ru.konkurst1.ekb.terraform_logviewer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.konkurst1.ekb.terraform_logviewer.model.LogEntry;
import ru.konkurst1.ekb.terraform_logviewer.model.LogLevel;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class LogParserService {

    // Паттерны для определения секций
    private final Pattern PLAN_START = Pattern.compile(".*Terraform will perform.*|.*terraform plan.*", Pattern.CASE_INSENSITIVE);
    private final Pattern APPLY_START = Pattern.compile(".*terraform apply.*|.*Applying.*", Pattern.CASE_INSENSITIVE);
    private final Pattern SECTION_END = Pattern.compile(".*Apply complete!.*|.*Plan:.*");

    private static final Pattern JSON_PATTERN = Pattern.compile("\\{.*\\}");

    // Форматы временных меток
    private final Pattern[] TIMESTAMP_PATTERNS = {
        Pattern.compile("(\\d{4}-\\d{2}-\\d{2}[T\\s]\\d{2}:\\d{2}:\\d{2})"),
        Pattern.compile("(\\d{2}:\\d{2}:\\d{2})"),
        Pattern.compile("\\[(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2})]"),
        Pattern.compile("(\\d{4}-\\d{2}-\\d{2}[T\\s]\\d{2}:\\d{2}:\\d{2})|(\\d{2}:\\d{2}:\\d{2})")
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

    public LogParseResult parseLogs(List<String> rawLines, String logFileId) {
        List<LogEntry> entries = new ArrayList<>();
        List<ParsingError> errors = new ArrayList<>();

        String currentSection = "other";
        Instant lastValidTimestamp = Instant.now();
        int lineNumber = 0;

        for (String rawLine : rawLines) {
            lineNumber++;
            try {
                LogEntry entry = parseSingleLine(rawLine, lineNumber, currentSection, lastValidTimestamp, logFileId);

                // Обновляем контекст
                if (entry.getTimestamp() != null) {
                    lastValidTimestamp = entry.getTimestamp();
                }
                if (!"other".equals(entry.getSection())) {
                    currentSection = entry.getSection();
                }

                entries.add(entry);

            } catch (Exception e) {
                // Создаем запись с ошибкой парсинга
                LogEntry errorEntry = createErrorEntry(rawLine, lineNumber, e.getMessage(), logFileId);
                entries.add(errorEntry);
                errors.add(new ParsingError(lineNumber, rawLine, e.getMessage()));
            }
        }

        return new LogParseResult(entries, errors);
    }

    private LogEntry parseSingleLine(String rawLine, int lineNumber, String currentSection,
                                     Instant lastTimestamp, String logFileId) {
        // Пытаемся извлечь JSON
        JsonNode jsonData = extractJson(rawLine);
        String cleanLine = removeJsonFromLine(rawLine);

        // Парсим timestamp
        Instant timestamp = extractTimestamp(cleanLine);
        if (timestamp == null) {
            timestamp = lastTimestamp;
        }

        // Определяем уровень
        LogLevel level = detectLogLevel(cleanLine);

        // Определяем секцию
        String section = detectSection(cleanLine, Collections.singletonList(currentSection));

        // Извлекаем чистое сообщение
        String message = extractCleanMessage(cleanLine);

        return new LogEntry(
                rawLine, timestamp, level, section, message,
                jsonData != null, false, null, lineNumber
        );
    }
    private LogEntry createErrorEntry(String rawLine, int lineNumber, String errorMessage, String logFileId) {
        return new LogEntry(
                rawLine, Instant.now(), LogLevel.ERROR, "other",
                "PARSING ERROR: " + rawLine, false, true, errorMessage, lineNumber
        );
    }
    private JsonNode extractJson(String line) {
        try {
            Matcher matcher = JSON_PATTERN.matcher(line);
            if (matcher.find()) {
                String jsonStr = matcher.group();
                ObjectMapper mapper = new ObjectMapper();
                return mapper.readTree(jsonStr);
            }
        } catch (Exception e) {
            // Игнорируем ошибки парсинга JSON
        }
        return null;
    }
    private String removeJsonFromLine(String line) {
        return JSON_PATTERN.matcher(line).replaceAll("").trim();
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
    private String extractCleanMessage(String line) {
        String withoutTimestamp = line;
        // Убираем timestamp если нашли
        for (Pattern pattern : TIMESTAMP_PATTERNS) {
            if (pattern.matcher(line).find()) {
                withoutTimestamp = pattern.matcher(line).replaceAll("").trim();
                break;
            }
        }

        // Убираем уровень логирования если он в начале
        for (LogLevel level : LogLevel.values()) {
            if (withoutTimestamp.toLowerCase().startsWith(level.name().toLowerCase())) {
                return withoutTimestamp.substring(level.name().length()).trim();
            }
        }

        return withoutTimestamp;
    }

    private List<String> readFileLines(MultipartFile file) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream()))) {
            return reader.lines().collect(Collectors.toList());
        }
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

}
