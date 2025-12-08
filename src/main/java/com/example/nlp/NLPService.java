package com.example.nlp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

/**
 * NLP Service that communicates with OpenAI API
 * Processes natural language queries and returns structured responses
 */
public class NLPService {
    private static final Logger logger = LoggerFactory.getLogger(NLPService.class);
    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";
    
    private final String apiKey;
    private final String model;
    
    public NLPService(String apiKey, String model) {
        this.apiKey = apiKey;
        this.model = model;
    }
    
    /**
     * Process an NLP query and return the response
     * @param query The natural language query
     * @return The processed response from OpenAI
     */
    public String processQuery(String query) {
        try {
            if (apiKey == null || apiKey.isEmpty()) {
                logger.warn("OpenAI API key not configured, returning mock response");
                return generateMockResponse(query);
            }
            
            return callOpenAIAPI(query);
        } catch (Exception e) {
            logger.error("Error processing NLP query: {}", e.getMessage(), e);
            return "Error processing query: " + e.getMessage();
        }
    }
    
    /**
     * Call OpenAI API with the query
     */
    private String callOpenAIAPI(String query) throws IOException {
        URL url = new URL(OPENAI_API_URL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Authorization", "Bearer " + apiKey);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);
        
        String requestBody = String.format(
            "{\"model\": \"%s\", \"messages\": [{\"role\": \"user\", \"content\": \"%s\"}]}",
            model,
            escapeJson(query)
        );
        
        try (var os = connection.getOutputStream()) {
            byte[] input = requestBody.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }
        
        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            try (Scanner scanner = new Scanner(connection.getInputStream())) {
                scanner.useDelimiter("\\A");
                return scanner.hasNext() ? scanner.next() : "No response";
            }
        } else {
            logger.error("OpenAI API error: HTTP {}", responseCode);
            return "API Error: " + responseCode;
        }
    }
    
    /**
     * Generate a mock response for testing
     */
    private String generateMockResponse(String query) {
        return String.format(
            "{\"response\": \"Mock response for query: %s\", \"timestamp\": \"%s\"}",
            query,
            System.currentTimeMillis()
        );
    }
    
    /**
     * Escape special characters for JSON
     */
    private String escapeJson(String input) {
        return input.replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }
}
