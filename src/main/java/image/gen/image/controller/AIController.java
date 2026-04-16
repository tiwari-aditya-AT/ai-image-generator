package image.gen.image.controller;

import image.gen.image.service.ComfyUIService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*") // allow frontend (optional)
public class AIController {

    private final ComfyUIService comfyUIService;

    public AIController(ComfyUIService comfyUIService) {
        this.comfyUIService = comfyUIService;
    }

    @PostMapping("/text-to-image")
    public ResponseEntity<?> generate(@RequestBody Map<String, String> body) {
        try {
            // 1. Get prompt
            String prompt = body.get("prompt");

            if (prompt == null || prompt.isEmpty()) {
                return ResponseEntity.badRequest().body(
                        Map.of("error", "Prompt is required")
                );
            }

            // 2. Generate image via ComfyUI
            String imageUrl = comfyUIService.generateImage(prompt);

            // 3. Return response
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "prompt", prompt,
                    "imageUrl", imageUrl
            ));

        } catch (Exception e) {
            e.printStackTrace(); // debug in console

            return ResponseEntity.status(500).body(
                    Map.of(
                            "status", "error",
                            "message", e.getMessage()
                    )
            );
        }
    }
}