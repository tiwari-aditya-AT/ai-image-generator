package image.gen.image.service;

import image.gen.image.config.AppConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.file.*;
import java.util.Base64;
import java.util.Map;

@Service
public class OllamaService {

    @Autowired
    private AppConfig config;

    private final RestTemplate restTemplate = new RestTemplate();

    public String imageToText(String prompt, String imagePath) throws Exception {
        byte[] imageBytes  = Files.readAllBytes(Paths.get(imagePath));
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);

        Map<String, Object> request = Map.of(
                "model",  "llava",
                "prompt", prompt != null ? prompt : "Describe this image",
                "images", new String[]{base64Image},
                "stream", false  // ✅ FIX #4: prevent streaming hang in RestTemplate
        );

        Map response = restTemplate.postForObject(
                config.getOllamaUrl(),
                request,
                Map.class
        );

        if (response == null || !response.containsKey("response")) {
            throw new RuntimeException("Empty or invalid response from Ollama");
        }

        return (String) response.get("response");
    }
}