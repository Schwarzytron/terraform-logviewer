package ru.konkurst1.ekb.terraform_logviewer.service;

import org.springframework.stereotype.Service;
import ru.konkurst1.ekb.terraform_logviewer.model.LogEntry;

import java.util.ArrayList;
import java.util.List;

@Service
public class SectionDetectionService {
    
    public List<LogEntry> detectSections(List<LogEntry> entries) {
        String currentSection = "other";
        List<LogEntry> result = new ArrayList<>();
        
        for (LogEntry entry : entries) {
            String detectedSection = detectSection(entry.getMessage(), currentSection);
            entry.setSection(detectedSection);
            currentSection = detectedSection;
            result.add(entry);
        }
        
        return result;
    }
    
    private String detectSection(String message, String currentSection) {
        if (message == null) return currentSection;
        
        // Эвристики для определения секций
        if (message.contains("backend/local: starting Plan operation") ||
            message.contains("Terraform will perform the following actions") ||
            message.toLowerCase().contains("terraform plan")) {
            return "plan";
        }
        
        if (message.contains("backend/local: starting Apply operation") ||
            message.contains("Applying...") ||
            message.toLowerCase().contains("terraform apply")) {
            return "apply";
        }
        
        if (message.contains("Apply complete!") ||
            message.contains("Plan:") ||
            message.contains("complete!")) {
            return "other";
        }
        
        return currentSection;
    }
}