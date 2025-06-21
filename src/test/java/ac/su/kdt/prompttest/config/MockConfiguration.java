package ac.su.kdt.prompttest.config;

import ac.su.kdt.prompttest.service.PerplexityService;
import ac.su.kdt.prompttest.service.PromptService;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
// import org.springframework.data.redis.core.RedisTemplate;

@TestConfiguration
public class MockConfiguration {

    @Bean
    @Primary
    public PerplexityService perplexityService() {
        return Mockito.mock(PerplexityService.class);
    }

    @Bean
    @Primary
    public PromptService promptService() {
        return Mockito.mock(PromptService.class);
    }

    // @Bean
    // @Primary
    // @SuppressWarnings("unchecked")
    // public RedisTemplate<String, Object> redisTemplate() {
    //     return Mockito.mock(RedisTemplate.class);
    // }
} 