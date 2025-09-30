package ru.konkurst1.ekb.terraform_logviewer;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.konkurst1.ekb.terraform_logviewer.model.LogEntry;

import java.util.List;

@RestController
@RequestMapping("/api/logs")
@CrossOrigin(origins = "*")
public class LogController {

    @GetMapping
    public ResponseEntity<List<LogEntry>> getLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        // TODO: Implement log retrieval
        return ResponseEntity.ok(List.of(
                new LogEntry("2023-10-01T10:00:00", "ERROR", "Terraform apply failed", "unknown","1"),
                new LogEntry("2023-10-01T10:01:00", "INFO", "Resource creation started", "unknown","1")
        ));
    }

    @PostMapping("/upload")
    public ResponseEntity<String> uploadLogs(@RequestBody String logData) {
        // TODO: Implement log parsing and storage
        return ResponseEntity.ok("Logs processed");
    }
}