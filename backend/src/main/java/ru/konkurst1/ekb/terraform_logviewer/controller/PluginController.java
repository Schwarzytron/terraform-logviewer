package ru.konkurst1.ekb.terraform_logviewer.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.konkurst1.ekb.terraform_logviewer.dto.SearchFilters;
import ru.konkurst1.ekb.terraform_logviewer.model.LogEntry;
import ru.konkurst1.ekb.terraform_logviewer.plugin.PluginInfo;
import ru.konkurst1.ekb.terraform_logviewer.plugin.PluginResponse;
import ru.konkurst1.ekb.terraform_logviewer.service.LogSearchService;
import ru.konkurst1.ekb.terraform_logviewer.service.PluginManagerService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/plugins")
@CrossOrigin(origins = "*")
public class PluginController {
    private static final Logger logger = LoggerFactory.getLogger(PluginController.class);

    @Autowired
    private PluginManagerService pluginManagerService;

    @Autowired
    private LogSearchService logSearchService;

    @PostMapping("/register")
    public ResponseEntity<String> registerPlugin(
            @RequestBody Map<String, String> request) {
        try {
            String name = request.get("name");
            String host = request.get("host");
            int port = Integer.parseInt(request.get("port"));

            pluginManagerService.registerPlugin(name, host, port);
            return ResponseEntity.ok("Plugin registered successfully");
        } catch (Exception e) {
            logger.error("Failed to register plugin", e);
            return ResponseEntity.badRequest().body("Failed to register plugin: " + e.getMessage());
        }
    }

    @GetMapping("/registered")
    public ResponseEntity<List<String>> getRegisteredPlugins() {
        return ResponseEntity.ok(pluginManagerService.getRegisteredPlugins());
    }

    @PostMapping("/{pluginName}/execute")
    public ResponseEntity<Map<String, Object>> executePlugin(
            @PathVariable String pluginName,
            @RequestBody Map<String, Object> request) {
        try {
            List<String> entryIds = (List<String>) request.get("entryIds");
            Map<String, String> parameters = (Map<String, String>) request.get("parameters");
            String logFileId = (String) request.get("logFileId");

            // Получаем записи для обработки
            List<LogEntry> entries;
            if (logFileId != null) {
                SearchFilters filters = new SearchFilters();
                filters.setLogFileId(logFileId);
                entries = logSearchService.advancedSearch(filters, logFileId).getContent();
            } else {
                throw new IllegalArgumentException("logFileId must be provided");
            }

            // Теперь возвращает PluginResponse
            PluginResponse response = pluginManagerService.processWithPlugin(pluginName, entries, parameters);

            Map<String, Object> result = new HashMap<>();
            result.put("statistics", response.getStatistics());
            result.put("processedCount", entries.size());
//            result.put("success", response.isSuccess());
            result.put("error", response.getErrorMessage());
//            result.put("processedEntries", response.getProcessedEntries()); // todo: надо разобраться с этим

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Plugin execution failed", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage(),
                    "success", false
            ));
        }
    }

    @GetMapping("/{pluginName}/info")
    public ResponseEntity<PluginInfo> getPluginInfo(@PathVariable String pluginName) {
        try {
            PluginInfo info = pluginManagerService.getPluginInfo(pluginName);
            return ResponseEntity.ok(info);
        } catch (Exception e) {
            logger.error("Failed to get plugin info", e);
            return ResponseEntity.badRequest().build();
        }
    }
}