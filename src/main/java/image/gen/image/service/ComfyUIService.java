package image.gen.image.service;

import java.nio.file.*;
import java.util.List;

import image.gen.image.config.AppConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.*;

@Service
public class ComfyUIService {

    @Autowired
    private AppConfig config;

    private final RestTemplate restTemplate = new RestTemplate();


    private String copyToComfyInput(String imagePath) throws Exception {
        Path source = Paths.get(imagePath);

        if (!Files.exists(source)) {
            throw new RuntimeException("Image not found: " + imagePath);
        }

        String fileName = source.getFileName().toString();

        Path target = Paths.get("C:/Users/ADITYA/ComfyUI-master/input/" + fileName);

        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);

        return fileName;
    }


    private Map waitForResult(String promptId) throws Exception {
        Map promptData = null;

        for (int i = 0; i < 80; i++) {
            Thread.sleep(1000);

            Map history = restTemplate.getForObject(
                    config.getComfyUrl()+"/history/" + promptId,
                    Map.class
            );

            if (history != null && history.containsKey(promptId)) {
                promptData = (Map) history.get(promptId);
                break;
            }
        }

        if (promptData == null) {
            throw new RuntimeException("Image generation timeout");
        }

        return promptData;
    }


    private String extractImageUrl(Map outputs) {
        for (Object nodeObj : outputs.values()) {
            Map node = (Map) nodeObj;

            if (node.containsKey("images")) {
                List images = (List) node.get("images");

                if (!images.isEmpty()) {
                    Map image = (Map) images.get(0);
                    String filename = (String) image.get("filename");

                    return config.getComfyUrl() + "/view?filename=" + filename;
                }
            }
        }

        throw new RuntimeException("No image found in ComfyUI response");
    }


    public String generateImage(String prompt) throws Exception {

        Map<String, Object> request = Map.of(
                "prompt", Map.of(

                        "1", Map.of(
                                "class_type", "CheckpointLoaderSimple",
                                "inputs", Map.of(
                                        "ckpt_name", "v1-5-pruned-emaonly.safetensors"
                                )
                        ),

                        "2", Map.of(
                                "class_type", "CLIPTextEncode",
                                "inputs", Map.of(
                                        "text", prompt,
                                        "clip", List.of("1", 1)
                                )
                        ),

                        "3", Map.of(
                                "class_type", "EmptyLatentImage",
                                "inputs", Map.of(
                                        "width", 512,
                                        "height", 512,
                                        "batch_size", 1
                                )
                        ),

                        "4", Map.of(
                                "class_type", "KSampler",
                                "inputs", Map.of(
                                        "seed", 12345,
                                        "steps", 20,
                                        "cfg", 7,
                                        "sampler_name", "euler",
                                        "scheduler", "normal",
                                        "denoise", 1,
                                        "model", List.of("1", 0),
                                        "positive", List.of("2", 0),
                                        "negative", List.of("2", 0),
                                        "latent_image", List.of("3", 0)
                                )
                        ),

                        "5", Map.of(
                                "class_type", "VAEDecode",
                                "inputs", Map.of(
                                        "samples", List.of("4", 0),
                                        "vae", List.of("1", 2)
                                )
                        ),

                        "6", Map.of(
                                "class_type", "SaveImage",
                                "inputs", Map.of(
                                        "images", List.of("5", 0),
                                        "filename_prefix", "generated"
                                )
                        )
                )
        );

        ResponseEntity<Map> response = restTemplate.postForEntity(
                "http://127.0.0.1:8188/prompt",
                request,
                Map.class
        );

        String promptId = (String) response.getBody().get("prompt_id");

        Map promptData = waitForResult(promptId);
        Map outputs = (Map) promptData.get("outputs");

        return extractImageUrl(outputs);
    }

    // =========================
    // 🟣 IMAGE TO IMAGE
    // =========================
    public String imageToImage(String prompt, String imagePath) throws Exception {

        // ✅ COPY IMAGE FIRST
        String fileName = copyToComfyInput(imagePath);

        Map<String, Object> request = Map.of(
                "prompt", Map.of(

                        "1", Map.of(
                                "class_type", "CheckpointLoaderSimple",
                                "inputs", Map.of(
                                        "ckpt_name", "v1-5-pruned-emaonly.safetensors"
                                )
                        ),

                        "2", Map.of(
                                "class_type", "CLIPTextEncode",
                                "inputs", Map.of(
                                        "text", prompt,
                                        "clip", List.of("1", 1)
                                )
                        ),

                        "3", Map.of(
                                "class_type", "LoadImage",
                                "inputs", Map.of(
                                        "image", fileName
                                )
                        ),

                        "4", Map.of(
                                "class_type", "VAEEncode",
                                "inputs", Map.of(
                                        "pixels", List.of("3", 0),
                                        "vae", List.of("1", 2)
                                )
                        ),

                        "5", Map.of(
                                "class_type", "KSampler",
                                "inputs", Map.of(
                                        "seed", 12345,
                                        "steps", 20,
                                        "cfg", 7,
                                        "sampler_name", "euler",
                                        "scheduler", "normal",
                                        "denoise", 0.7,
                                        "model", List.of("1", 0),
                                        "positive", List.of("2", 0),
                                        "negative", List.of("2", 0),
                                        "latent_image", List.of("4", 0)
                                )
                        ),

                        "6", Map.of(
                                "class_type", "VAEDecode",
                                "inputs", Map.of(
                                        "samples", List.of("5", 0),
                                        "vae", List.of("1", 2)
                                )
                        ),

                        "7", Map.of(
                                "class_type", "SaveImage",
                                "inputs", Map.of(
                                        "images", List.of("6", 0),
                                        "filename_prefix", "img2img"
                                )
                        )
                )
        );

        ResponseEntity<Map> response = restTemplate.postForEntity(
                "http://127.0.0.1:8188/prompt",
                request,
                Map.class
        );

        String promptId = (String) response.getBody().get("prompt_id");

        Map promptData = waitForResult(promptId);
        Map outputs = (Map) promptData.get("outputs");

        return extractImageUrl(outputs);
    }
}