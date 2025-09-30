package ru.konkurst1.ekb.terraform_logviewer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

@SpringBootApplication
public class TerraformLogViewerApplication {

	public static void main(String[] args) {
		SpringApplication.run(TerraformLogViewerApplication.class, args);
	}

}

