package image.gen.image.config;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "ai")
public class AppConfig {

    private String ollamaUrl = "http://localhost:11434/api/generate";
    private String sdUrl = "http://127.0.0.1:7860/sdapi/v1/txt2img";
}
