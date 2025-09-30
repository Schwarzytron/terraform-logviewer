package ru.konkurst1.ekb.terraform_logviewer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class ConfigChecker implements CommandLineRunner {

	private static final Logger logger = LoggerFactory.getLogger(ConfigChecker.class);

	@Value("${spring.datasource.url:NOT_FOUND}")
	private String datasourceUrl;

	@Override
	public void run(String... args) throws Exception {
		logger.info("=== CONFIGURATION CHECK ===");
		logger.info("Datasource URL: {}", datasourceUrl);
		logger.info("=== CONFIGURATION CHECK COMPLETE ===");
	}
}
