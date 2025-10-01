package ru.konkurst1.ekb.terraform_logviewer;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import ru.konkurst1.ekb.terraform_logviewer.service.LogParserService;
import ru.konkurst1.ekb.terraform_logviewer.service.LogSearchService;
import ru.konkurst1.ekb.terraform_logviewer.service.LogStorageService;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class TerraformLogViewerIntegrationTest {

    @Autowired(required = false)
    private LogParserService logParserService;

    @Autowired(required = false)
    private LogStorageService logStorageService;

    @Autowired(required = false)
    private LogSearchService logSearchService;

    @Test
    void contextLoads() {
        // Test that basic beans are loaded
        assertAll(
                () -> assertNotNull(logParserService, "LogParserService should be loaded"),
                () -> assertNotNull(logStorageService, "LogStorageService should be loaded"),
                () -> assertNotNull(logSearchService, "LogSearchService should be loaded")
        );
    }
}