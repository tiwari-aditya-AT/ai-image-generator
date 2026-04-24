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
//        READ IMAGE
        byte[] imageBytes = Files.readAllBytes(Paths.get(imagePath));
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);

        //  REQUEST BODY
        Map<String, Object> request = Map.of(
                "model", "llava",
                "prompt", prompt,
                "images", new String[]{base64Image}
        );

        // CALL OLLAMA
        Map response = restTemplate.postForObject(
                config.getOllamaUrl(),
                request,
                Map.class
        );

        return (String) response.get("response");
    }
}