package ru.konkurst1.ekb.terraform_logviewer.service;

public record ParsingError(Integer lineNumber, String rawLine, String errorMessage) {
}