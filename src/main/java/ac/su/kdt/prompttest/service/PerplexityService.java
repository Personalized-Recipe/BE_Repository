package ac.su.kdt.prompttest.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
public class PerplexityService {

    @Value("${perplexity.api.key}")
    private String apiKey;
    
    @Value("${perplexity.api.url}")
    private String apiUrl;
    
    private final RestTemplate restTemplate;
    private final PromptService promptService;
    
    public PerplexityService(PromptService promptService) {
        this.restTemplate = new RestTemplate();
        this.promptService = promptService;
    }

    @PostConstruct
    public void init() {
        log.info("API URL: {}", apiUrl);
        log.info("API Key length: {}", apiKey != null ? apiKey.length() : "null");
    }
    
    public String getResponse(Long userId, String userPrompt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);
        
        // PromptService를 통해 사용자 정보 기반의 프롬프트 생성
        String systemPrompt = promptService.generatePrompt(userId, userPrompt);
        
        Map<String, String> systemMessage = new HashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content", systemPrompt);
        
        Map<String, String> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", userPrompt);
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "sonar-pro");
        requestBody.put("messages", Arrays.asList(systemMessage, userMessage));
        
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        
        try {
            log.info("Sending request to Perplexity API with headers: {}", headers);
            log.info("Request body: {}", requestBody);
            
            Map<String, Object> response = restTemplate.postForObject(apiUrl, request, Map.class);
            
            if (response != null && response.containsKey("choices") 
                && !((List)response.get("choices")).isEmpty()) {
                Map<String, Object> choice = (Map<String, Object>)((List)response.get("choices")).get(0);
                Map<String, Object> messageResponse = (Map<String, Object>)choice.get("message");
                return (String) messageResponse.get("content");
            } else {
                return "No response received from Perplexity API";
            }
        } catch (Exception e) {
            log.error("Error calling Perplexity API", e);
            return "Error calling Perplexity API: " + e.getMessage();
        }
    }

    private String buildSystemPrompt(Map<String, String> userPreferences) {
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("당신은 레시피 추천 전문가입니다. 다음과 같은 사용자의 선호도와 요구사항을 고려하여 답변해주세요:\n\n");

        // 사용자 선호도 정보 추가
        if (userPreferences != null) {
            if (userPreferences.containsKey("dietaryRestrictions")) {
                promptBuilder.append("식단 제한사항: ").append(userPreferences.get("dietaryRestrictions")).append("\n");
            }
            if (userPreferences.containsKey("cookingLevel")) {
                promptBuilder.append("요리 실력: ").append(userPreferences.get("cookingLevel")).append("\n");
            }
            if (userPreferences.containsKey("preferredCuisine")) {
                promptBuilder.append("선호하는 요리: ").append(userPreferences.get("preferredCuisine")).append("\n");
            }
            if (userPreferences.containsKey("cookingTime")) {
                promptBuilder.append("원하는 조리시간: ").append(userPreferences.get("cookingTime")).append("\n");
            }
        }

        promptBuilder.append("\n레시피는 다음 형식으로 제공합니다:\n");
        promptBuilder.append("1. 요리 이름\n");
        promptBuilder.append("2. 필요한 재료와 양\n");
        promptBuilder.append("3. 조리 시간\n");
        promptBuilder.append("4. 난이도\n");
        promptBuilder.append("5. 상세한 조리 방법\n\n");
        promptBuilder.append("추가 고려사항:\n");
        promptBuilder.append("- 사용자의 요리 실력에 맞춘 설명 제공\n");
        promptBuilder.append("- 건강과 영양을 고려한 조리법 제안\n");
        promptBuilder.append("- 대체 재료 옵션 제공\n");
        promptBuilder.append("- 요리 팁과 주의사항 포함\n");

        return promptBuilder.toString();
    }
}