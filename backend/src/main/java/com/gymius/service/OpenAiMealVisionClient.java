package com.gymius.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gymius.dto.MealAnalysisDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;

@Component
public class OpenAiMealVisionClient implements MealVisionClient {

    private static final String OPENAI_RESPONSES_URL = "https://api.openai.com/v1";

    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;
    private final String imageDetail;
    private final RestClient restClient;
    private final Semaphore analysisPermits;

    public OpenAiMealVisionClient(
            ObjectMapper objectMapper,
            @Value("${app.openai.api-key:}") String apiKey,
            @Value("${app.openai.model:gpt-5-mini}") String model,
            @Value("${app.openai.image-detail:auto}") String imageDetail,
            @Value("${app.openai.connect-timeout:10s}") Duration connectTimeout,
            @Value("${app.openai.read-timeout:45s}") Duration readTimeout,
            @Value("${app.openai.max-concurrent-analyses:4}") int maxConcurrentAnalyses
    ) {
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.model = model;
        this.imageDetail = imageDetail;
        this.analysisPermits = new Semaphore(Math.max(1, maxConcurrentAnalyses));

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(connectTimeout)
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(readTimeout);
        this.restClient = RestClient.builder()
                .baseUrl(OPENAI_RESPONSES_URL)
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public MealAnalysisDto analyze(MultipartFile image) {
        if (!isConfigured()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "OpenAI API key is not configured.");
        }

        if (!analysisPermits.tryAcquire()) {
            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Meal analysis is busy. Please try again in a moment."
            );
        }

        try {
            JsonNode response = restClient
                    .post()
                    .uri("/responses")
                    .body(requestBody(image))
                    .retrieve()
                    .body(JsonNode.class);

            if (response == null) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "OpenAI returned no meal analysis response.");
            }

            String responseStatus = response.path("status").asText();
            if (!responseStatus.isBlank() && !"completed".equals(responseStatus)) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "OpenAI did not complete the meal analysis.");
            }

            String outputText = extractOutputText(response);
            if (outputText == null || outputText.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "OpenAI returned an empty meal analysis.");
            }

            return objectMapper.readValue(outputText, MealAnalysisDto.class);
        } catch (RestClientException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "OpenAI meal analysis failed.", exception);
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Meal analysis response could not be processed.", exception);
        } finally {
            analysisPermits.release();
        }
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    private Map<String, Object> requestBody(MultipartFile image) throws IOException {
        String contentType = image.getContentType() == null ? MediaType.IMAGE_JPEG_VALUE : image.getContentType();
        String dataUrl = "data:%s;base64,%s".formatted(
                contentType,
                Base64.getEncoder().encodeToString(image.getBytes())
        );

        return Map.of(
                "model", model,
                "store", false,
                "max_output_tokens", 2000,
                "instructions", instructions(),
                "input", List.of(Map.of(
                        "role", "user",
                        "content", List.of(
                                Map.of(
                                        "type", "input_text",
                                        "text", "Analyze this meal photo and return the structured nutrition estimate."
                                ),
                                Map.of(
                                        "type", "input_image",
                                        "image_url", dataUrl,
                                        "detail", imageDetail
                                )
                        )
                )),
                "text", Map.of("format", responseFormat())
        );
    }

    private String instructions() {
        return """
                You analyze meal photos for a fitness tracking app.
                Estimate visible food items, portions, total calories, calorie range, confidence, and macros.
                Return approximate estimates only, not medical advice.
                If the image is unclear, partially occluded, contains sauces, or has hidden ingredients, lower confidence and widen the calorie range.
                Do not identify people, locations, or personal background details. Focus only on visible food and drink.
                Keep userMessage encouraging and concise.
                """;
    }

    private Map<String, Object> responseFormat() {
        return Map.of(
                "type", "json_schema",
                "name", "meal_analysis",
                "strict", true,
                "schema", mealAnalysisSchema()
        );
    }

    private Map<String, Object> mealAnalysisSchema() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("estimatedCalories", boundedIntegerSchema());
        properties.put("calorieMin", boundedIntegerSchema());
        properties.put("calorieMax", boundedIntegerSchema());
        properties.put("confidence", Map.of("type", "string", "enum", List.of("LOW", "MEDIUM", "HIGH")));
        properties.put("foodItems", Map.of(
                "type", "array",
                "maxItems", 20,
                "items", foodItemSchema()
        ));
        properties.put("proteinGrams", boundedNullableNumberSchema());
        properties.put("carbsGrams", boundedNullableNumberSchema());
        properties.put("fatGrams", boundedNullableNumberSchema());
        properties.put("confidenceNote", Map.of("type", "string", "maxLength", 500));
        properties.put("userMessage", Map.of("type", "string", "maxLength", 300));

        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("additionalProperties", false);
        schema.put("required", new ArrayList<>(properties.keySet()));
        schema.put("properties", properties);
        return schema;
    }

    private Map<String, Object> foodItemSchema() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("name", Map.of("type", "string", "maxLength", 120));
        properties.put("portionEstimate", Map.of("type", "string", "maxLength", 200));
        properties.put("estimatedCalories", boundedIntegerSchema());

        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("additionalProperties", false);
        schema.put("required", new ArrayList<>(properties.keySet()));
        schema.put("properties", properties);
        return schema;
    }

    private Map<String, Object> boundedIntegerSchema() {
        return Map.of(
                "type", "integer",
                "minimum", 0,
                "maximum", 10000
        );
    }

    private Map<String, Object> boundedNullableNumberSchema() {
        return Map.of(
                "type", List.of("number", "null"),
                "minimum", 0,
                "maximum", 10000
        );
    }

    private String extractOutputText(JsonNode response) {
        JsonNode outputText = response.path("output_text");
        if (outputText.isTextual()) {
            return outputText.asText();
        }

        JsonNode output = response.path("output");
        if (!output.isArray()) {
            return null;
        }

        StringBuilder builder = new StringBuilder();
        for (JsonNode item : output) {
            JsonNode content = item.path("content");
            if (!content.isArray()) {
                continue;
            }

            for (JsonNode contentItem : content) {
                if ("output_text".equals(contentItem.path("type").asText())) {
                    builder.append(contentItem.path("text").asText());
                }
            }
        }

        return builder.isEmpty() ? null : builder.toString();
    }
}
