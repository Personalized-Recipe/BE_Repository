package ac.su.kdt.prompttest.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {
    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        
        // 연결 타임아웃: 30초
        factory.setConnectTimeout(30000);
        
        // 읽기 타임아웃: 60초 (AI 응답이 느릴 수 있으므로)
        factory.setReadTimeout(60000);
        
        return new RestTemplate(factory);
    }
} 