package image.gen.image.controller;

import image.gen.image.config.AppConfig;
import image.gen.image.model.ImageHistory;
import image.gen.image.model.User;
import image.gen.image.repository.ImageHistoryRepository;
import image.gen.image.service.ComfyUIService;
import image.gen.image.service.OllamaService;
import image.gen.image.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.StandardCopyOption;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class AIController {

    private final AppConfig config;
    private final OllamaService ollamaService;
    private final ComfyUIService comfyUIService;
    private final UserService userService;
    private final ImageHistoryRepository imageHistoryRepository;

    public AIController(AppConfig config,
                        OllamaService ollamaService,
                        ComfyUIService comfyUIService,
                        UserService userService,
                        ImageHistoryRepository imageHistoryRepository) {
        this.config                 = config;
        this.ollamaService          = ollamaService;
        this.comfyUIService         = comfyUIService;
        this.userService            = userService;
        this.imageHistoryRepository = imageHistoryRepository;
    }

    // ─── 1. IMAGE → TEXT ──────────────────────────────────────────────────────

    @PostMapping("/image-to-text")
    public ResponseEntity<?> imageToText(
            @RequestParam("prompt") String prompt,
            @RequestParam("image") MultipartFile image,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {

            // Create uploads directory
            String uploadDir = "uploads/";
            Files.createDirectories(Paths.get(uploadDir));

            // Save uploaded file
            String fileName =
                    System.currentTimeMillis() + "_" +
                            image.getOriginalFilename();

            String imagePath =
                    uploadDir + fileName;

            Files.copy(
                    image.getInputStream(),
                    Paths.get(imagePath),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING
            );

            // Send saved image to Ollama
            String result =
                    ollamaService.imageToText(prompt, imagePath);

            return ResponseEntity.ok(
                    Map.of("description", result)
            );

        } catch (Exception e) {

            return ResponseEntity.status(500).body(
                    Map.of(
                            "error", e.getMessage()
                    )
            );
        }
    }

    // ─── 2. TEXT → IMAGE (costs 1 credit, saves to history) ──────────────────
    @PostMapping("/text-to-image")
    public ResponseEntity<?> textToImage(
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserDetails userDetails) {

        String prompt = body.get("prompt");

        if (prompt == null || prompt.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Prompt is required"));
        }

        User user = userService.getByUsername(userDetails.getUsername());

        if (user.getCredits() < 1) {
            return ResponseEntity.status(402).body(
                    Map.of("error", "Insufficient credits. Please top up.")
            );
        }

        try {
            String imageUrl = comfyUIService.generateImage(prompt);

            userService.deductCredit(user);

            ImageHistory history = new ImageHistory();
            history.setUser(user);
            history.setPrompt(prompt);
            history.setImageUrl(imageUrl);
            history.setCreditsUsed(1);
            imageHistoryRepository.save(history);

            return ResponseEntity.ok(Map.of(
                    "status",             "success",
                    "prompt",             prompt,
                    "imageUrl",           imageUrl,
                    "creditsRemaining",   user.getCredits()
            ));

        } catch (Exception e) {
            return ResponseEntity.status(503).body(Map.of(
                    "status",  "error",
                    "message", "ComfyUI unavailable: " + e.getMessage()
            ));
        }
    }

    // ─── 3. IMAGE+TEXT → TEXT (describe image) ────────────────────────────────
    @PostMapping("/imagetext-to-text")
    public ResponseEntity<?> imagetextToText(
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserDetails userDetails) {

        String imagePath = body.get("imagePath");

        if (imagePath == null || imagePath.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "imagePath is required"));
        }

        try {
            String result = describeImage(imagePath);
            return ResponseEntity.ok(Map.of("description", result));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ─── 4. IMAGE → IMAGE (costs 1 credit, saves to history) ─────────────────
    @PostMapping("/image-to-image")
    public ResponseEntity<?> imageToImage(
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserDetails userDetails) {

        String prompt    = body.get("prompt");
        String imagePath = body.get("imagePath");

        if (imagePath == null || imagePath.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "imagePath is required"));
        }

        User user = userService.getByUsername(userDetails.getUsername());

        if (user.getCredits() < 1) {
            return ResponseEntity.status(402).body(
                    Map.of("error", "Insufficient credits. Please top up.")
            );
        }

        try {
            String result = comfyUIService.imageToImage(prompt, imagePath);

            userService.deductCredit(user);

            ImageHistory history = new ImageHistory();
            history.setUser(user);
            history.setPrompt(prompt != null ? prompt : "image-to-image");
            history.setImageUrl(result);
            history.setCreditsUsed(1);
            imageHistoryRepository.save(history);

            return ResponseEntity.ok(Map.of(
                    "imageUrl",           result,
                    "creditsRemaining",   user.getCredits()
            ));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ─── Helper ───────────────────────────────────────────────────────────────
    private String describeImage(String imagePath) throws Exception {
        RestTemplate restTemplate = new RestTemplate();

        byte[] imageBytes  = Files.readAllBytes(Paths.get(imagePath));
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);

        Map<String, Object> request = new HashMap<>();
        request.put("model",  "llava");
        request.put("prompt", "Describe this image in detail");
        request.put("images", List.of(base64Image));
        request.put("stream", false);

        ResponseEntity<Map> response = restTemplate.postForEntity(
                config.getOllamaUrl(), request, Map.class
        );

        return response.getBody().get("response").toString();
    }
}