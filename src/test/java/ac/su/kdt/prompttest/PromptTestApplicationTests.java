package ac.su.kdt.prompttest;

import ac.su.kdt.prompttest.config.TestRedisConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@Import(TestRedisConfiguration.class)
@ActiveProfiles("test")
class PromptTestApplicationTests {

    @Test
    void contextLoads() {
    }

}
