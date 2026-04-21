package com.rohith.batch.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Service
public class AzureOpenAIService {

    @Value("${azure.openai.endpoint}")
    private String endpoint;

    @Value("${azure.openai.api-key}")
    private String apiKey;

    @Value("${azure.openai.deployment-name:gpt-4}")
    private String deploymentName;

    private final RestTemplate restTemplate;

    public AzureOpenAIService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Retryable(value = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public String extractStructuredData(String rawContent) {
        String url = endpoint + "/openai/deployments/" + deploymentName + "/chat/completions?api-version=2024-02-01";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("api-key", apiKey);

        String systemPrompt = """
            You are a data extraction assistant. Extract structured information from state documents.
            Return a valid JSON object with fields: applicantName, caseNumber, programType, eligibilityDate, amount.
            If a field is not found, set it to null.
            """;

        Map<String, Object> requestBody = Map.of(
            "messages", new Object[]{
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", "Extract data from: " + rawContent)
            },
            "temperature", 0.1,
            "max_tokens", 500
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

        // Extract content from response
        var choices = (java.util.List<?>) response.getBody().get("choices");
        var message = (Map<?, ?>) ((Map<?, ?>) choices.get(0)).get("message");
        return (String) message.get("content");
    }
}