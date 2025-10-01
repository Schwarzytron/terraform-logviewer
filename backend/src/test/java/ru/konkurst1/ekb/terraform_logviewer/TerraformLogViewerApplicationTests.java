package ru.konkurst1.ekb.terraform_logviewer;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class TerraformLogViewerApplicationTests {

	@Test
	void contextLoads() {
		// Simple test that just checks if the test runs
		assertTrue(true, "Context should load with test profile");
	}
}