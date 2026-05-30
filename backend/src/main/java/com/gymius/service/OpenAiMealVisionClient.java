package com.gymius.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gymius.dto.MealAnalysisDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class OpenAiMealVisionClient implements MealVisionClient {

    private static final String OPENAI_RESPONSES_URL = "https://api.openai.com/v1";

    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;
    private final String imageDetail;

    public OpenAiMealVisionClient(
            ObjectMapper objectMapper,
            @Value("${app.openai.api-key:}") String apiKey,
            @Value("${app.openai.model:gpt-5-mini}") String model,
            @Value("${app.openai.image-detail:auto}") String imageDetail
    ) {
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.model = model;
        this.imageDetail = imageDetail;
    }

    @Override
    public MealAnalysisDto analyze(MultipartFile image) {
        if (!isConfigured()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "OpenAI API key is not configured.");
        }

        try {
            JsonNode response = RestClient.builder()
                    .baseUrl(OPENAI_RESPONSES_URL)
                    .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .build()
                    .post()
                    .uri("/responses")
                    .body(requestBody(image))
                    .retrieve()
                    .body(JsonNode.class);

            String outputText = extractOutputText(response);
            if (outputText == null || outputText.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "OpenAI returned an empty meal analysis.");
            }

            return objectMapper.readValue(outputText, MealAnalysisDto.class);
        } catch (RestClientException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "OpenAI meal analysis failed.", exception);
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Meal analysis response could not be processed.", exception);
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
        properties.put("estimatedCalories", Map.of("type", "integer"));
        properties.put("calorieMin", Map.of("type", "integer"));
        properties.put("calorieMax", Map.of("type", "integer"));
        properties.put("confidence", Map.of("type", "string", "enum", List.of("LOW", "MEDIUM", "HIGH")));
        properties.put("foodItems", Map.of(
                "type", "array",
                "items", foodItemSchema()
        ));
        properties.put("proteinGrams", Map.of("type", List.of("number", "null")));
        properties.put("carbsGrams", Map.of("type", List.of("number", "null")));
        properties.put("fatGrams", Map.of("type", List.of("number", "null")));
        properties.put("confidenceNote", Map.of("type", "string"));
        properties.put("userMessage", Map.of("type", "string"));

        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("additionalProperties", false);
        schema.put("required", new ArrayList<>(properties.keySet()));
        schema.put("properties", properties);
        return schema;
    }

    private Map<String, Object> foodItemSchema() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("name", Map.of("type", "string"));
        properties.put("portionEstimate", Map.of("type", "string"));
        properties.put("estimatedCalories", Map.of("type", "integer"));

        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("additionalProperties", false);
        schema.put("required", new ArrayList<>(properties.keySet()));
        schema.put("properties", properties);
        return schema;
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
