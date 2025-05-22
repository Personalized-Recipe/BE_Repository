package ac.su.kdt.prompttest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class PromptTestApplication {

    public static void main(String[] args) {
        SpringApplication.run(PromptTestApplication.class, args);
    }

}
