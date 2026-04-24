package image.gen.image.controller;

import image.gen.image.config.AppConfig;
import image.gen.image.service.ComfyUIService;
import image.gen.image.service.OllamaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import java.util.Base64;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*") // allow frontend (optional)
public class AIController {

    @Autowired
    private AppConfig config;

    @Autowired
    private OllamaService ollamaService;

    private final ComfyUIService comfyUIService;

    public AIController(ComfyUIService comfyUIService) {
        this.comfyUIService = comfyUIService;
    }
    @PostMapping("/image-to-text")
    public String imageToText(@RequestBody Map<String, String> request) throws Exception {

        String prompt = request.get("prompt");
        String imagePath = request.get("imagePath");
        try {
            return ollamaService.imageToText(prompt, imagePath);
        } catch (Exception e) {
            return "Fallback: LLM not available";
        }
    }

    @PostMapping("/text-to-image")
    public ResponseEntity<?> generate(@RequestBody Map<String, String> body) {

        String prompt = body.get("prompt");

        if (prompt == null || prompt.isEmpty()) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Prompt is required")
            );
        }

        try {
            String imageUrl = comfyUIService.generateImage(prompt);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "prompt", prompt,
                    "imageUrl", imageUrl
            ));

        } catch (Exception e) {

            return ResponseEntity.ok(Map.of(
                    "status", "fallback",
                    "message", "Local GPU not available",
                    "prompt", prompt
            ));
        }
    }


    @PostMapping("/imagetext-to-text")
    public ResponseEntity<?> imagetextToText(@RequestBody Map<String, String> body) {
        try {
            String imagePath = body.get("imagePath");

            if (imagePath == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "imagePath required"));
            }

            String result = describeImage(imagePath);

            return ResponseEntity.ok(Map.of("description", result));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    private String describeImage(String imagePath) throws Exception {

        RestTemplate restTemplate = new RestTemplate();

        // convert image → base64
        byte[] imageBytes = Files.readAllBytes(Paths.get(imagePath));
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);

        Map<String, Object> request = new HashMap<>();
        request.put("model", "llava");
        request.put("prompt", "Describe this image in detail");
        request.put("images", List.of(base64Image));
        request.put("stream", false);

        ResponseEntity<Map> response = restTemplate.postForEntity(
                config.getOllamaUrl(),
                request,
                Map.class
        );

        return response.getBody().get("response").toString();
    }


    @PostMapping("/image-to-image")
    public ResponseEntity<?> imageToImage(@RequestBody Map<String, String> body) {
        try {
            String prompt = body.get("prompt");
            String imagePath = body.get("imagePath");

            String result = comfyUIService.imageToImage(prompt, imagePath);

            return ResponseEntity.ok(Map.of("imageUrl", result));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }


    }

}