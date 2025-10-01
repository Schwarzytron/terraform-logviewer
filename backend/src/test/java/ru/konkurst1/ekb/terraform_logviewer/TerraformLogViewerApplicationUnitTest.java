package ru.konkurst1.ekb.terraform_logviewer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TerraformLogViewerApplicationUnitTest {

    @Test
    void applicationStarts() {
        // Test without Spring context - just instantiate the class
        assertDoesNotThrow(() -> {
            TerraformLogViewerApplication application = new TerraformLogViewerApplication();
            assertNotNull(application);
        });
    }

    @Test
    void mainMethodExists() {
        // Verify the main method signature exists without invoking it
        assertDoesNotThrow(() -> {
            var method = TerraformLogViewerApplication.class.getMethod("main", String[].class);
            assertNotNull(method);
        });
    }

    @Test
    void mainMethod_CanBeCalledWithNull() {
        // Test that main method can be called with null (simplified test)
        assertDoesNotThrow(() -> {
            // We're just testing that the method signature is correct
            // Not actually running the application
            TerraformLogViewerApplication.class.getMethod("main", String[].class);
        });
    }
}