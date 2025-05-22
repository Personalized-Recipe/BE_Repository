package ac.su.kdt.prompttest.controller;

import ac.su.kdt.prompttest.service.PerplexityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class TestController {

    private final PerplexityService perplexityService;

    @GetMapping("/perplexity")
    public ResponseEntity<String> testPerplexity() {
        String testPrompt = "간단한 김치찌개 레시피를 알려줘";
        String response = perplexityService.getResponseAsString(1, testPrompt);
        return ResponseEntity.ok(response);
    }
} 