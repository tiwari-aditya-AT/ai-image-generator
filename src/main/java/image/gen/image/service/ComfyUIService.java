package image.gen.image.service;

import image.gen.image.config.AppConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.file.*;
import java.util.List;
import java.util.Map;

@Service
public class ComfyUIService {

    @Autowired
    private AppConfig config;

    private final RestTemplate restTemplate = new RestTemplate();

    // ─── Copy image into ComfyUI's input folder ────────────────────────────────
    private String copyToComfyInput(String imagePath) throws Exception {
        Path source = Paths.get(imagePath);

        if (!Files.exists(source)) {
            throw new RuntimeException("Image not found: " + imagePath);
        }

        String fileName = source.getFileName().toString();

        // ✅ FIX #2: use config instead of hardcoded path
        Path target = Paths.get(config.getComfyInputDir(), fileName);
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);

        return fileName;
    }

    // ─── Poll ComfyUI history until job is done ────────────────────────────────
    private Map waitForResult(String promptId) throws Exception {
        for (int i = 0; i < 80; i++) {
            Thread.sleep(1000);

            Map history = restTemplate.getForObject(
                    config.getComfyUrl() + "/history/" + promptId,
                    Map.class
            );

            if (history != null && history.containsKey(promptId)) {
                return (Map) history.get(promptId);
            }
        }

        throw new RuntimeException("Image generation timed out after 80s");
    }

    // ─── Extract image URL from ComfyUI outputs ────────────────────────────────
    private String extractImageUrl(Map outputs) {
        for (Object nodeObj : outputs.values()) {
            Map node = (Map) nodeObj;

            if (node.containsKey("images")) {
                List images = (List) node.get("images");

                if (!images.isEmpty()) {
                    Map image    = (Map) images.get(0);
                    String filename = (String) image.get("filename");
                    return config.getComfyUrl() + "/view?filename=" + filename;
                }
            }
        }

        throw new RuntimeException("No image found in ComfyUI response");
    }

    // ─── TEXT → IMAGE ──────────────────────────────────────────────────────────
    public String generateImage(String prompt) throws Exception {

        Map<String, Object> request = Map.of(
                "prompt", Map.of(

                        "1", Map.of(
                                "class_type", "CheckpointLoaderSimple",
                                "inputs", Map.of("ckpt_name", "v1-5-pruned-emaonly.safetensors")
                        ),

                        // ✅ Positive prompt
                        "2", Map.of(
                                "class_type", "CLIPTextEncode",
                                "inputs", Map.of(
                                        "text", prompt,
                                        "clip", List.of("1", 1)
                                )
                        ),

                        // ✅ FIX #3: dedicated negative prompt node
                        "2b", Map.of(
                                "class_type", "CLIPTextEncode",
                                "inputs", Map.of(
                                        "text", "blurry, bad quality, distorted, watermark, low resolution, ugly",
                                        "clip", List.of("1", 1)
                                )
                        ),

                        "3", Map.of(
                                "class_type", "EmptyLatentImage",
                                "inputs", Map.of("width", 512, "height", 512, "batch_size", 1)
                        ),

                        "4", Map.of(
                                "class_type", "KSampler",
                                "inputs", Map.of(
                                        "seed",         42,
                                        "steps",        20,
                                        "cfg",          7,
                                        "sampler_name", "euler",
                                        "scheduler",    "normal",
                                        "denoise",      1.0,
                                        "model",        List.of("1", 0),
                                        "positive",     List.of("2", 0),   // ✅ positive
                                        "negative",     List.of("2b", 0),  // ✅ FIX #3: negative
                                        "latent_image", List.of("3", 0)
                                )
                        ),

                        "5", Map.of(
                                "class_type", "VAEDecode",
                                "inputs", Map.of(
                                        "samples", List.of("4", 0),
                                        "vae",     List.of("1", 2)
                                )
                        ),

                        "6", Map.of(
                                "class_type", "SaveImage",
                                "inputs", Map.of(
                                        "images",           List.of("5", 0),
                                        "filename_prefix",  "generated"
                                )
                        )
                )
        );

        ResponseEntity<Map> response = restTemplate.postForEntity(
                config.getComfyUrl() + "/prompt", request, Map.class
        );

        String promptId = (String) response.getBody().get("prompt_id");
        Map promptData  = waitForResult(promptId);
        Map outputs     = (Map) promptData.get("outputs");

        return extractImageUrl(outputs);
    }

    // ─── IMAGE → IMAGE ─────────────────────────────────────────────────────────
    public String imageToImage(String prompt, String imagePath) throws Exception {

        String fileName = copyToComfyInput(imagePath);

        Map<String, Object> request = Map.of(
                "prompt", Map.of(

                        "1", Map.of(
                                "class_type", "CheckpointLoaderSimple",
                                "inputs", Map.of("ckpt_name", "v1-5-pruned-emaonly.safetensors")
                        ),

                        // ✅ Positive prompt
                        "2", Map.of(
                                "class_type", "CLIPTextEncode",
                                "inputs", Map.of(
                                        "text", prompt,
                                        "clip", List.of("1", 1)
                                )
                        ),

                        // ✅ FIX #3: dedicated negative prompt node
                        "2b", Map.of(
                                "class_type", "CLIPTextEncode",
                                "inputs", Map.of(
                                        "text", "blurry, bad quality, distorted, watermark, low resolution, ugly",
                                        "clip", List.of("1", 1)
                                )
                        ),

                        "3", Map.of(
                                "class_type", "LoadImage",
                                "inputs", Map.of("image", fileName)
                        ),

                        "4", Map.of(
                                "class_type", "VAEEncode",
                                "inputs", Map.of(
                                        "pixels", List.of("3", 0),
                                        "vae",    List.of("1", 2)
                                )
                        ),

                        "5", Map.of(
                                "class_type", "KSampler",
                                "inputs", Map.of(
                                        "seed",         42,
                                        "steps",        20,
                                        "cfg",          7,
                                        "sampler_name", "euler",
                                        "scheduler",    "normal",
                                        "denoise",      0.7,
                                        "model",        List.of("1", 0),
                                        "positive",     List.of("2", 0),   // ✅ positive
                                        "negative",     List.of("2b", 0),  // ✅ FIX #3: negative
                                        "latent_image", List.of("4", 0)
                                )
                        ),

                        "6", Map.of(
                                "class_type", "VAEDecode",
                                "inputs", Map.of(
                                        "samples", List.of("5", 0),
                                        "vae",     List.of("1", 2)
                                )
                        ),

                        "7", Map.of(
                                "class_type", "SaveImage",
                                "inputs", Map.of(
                                        "images",          List.of("6", 0),
                                        "filename_prefix", "img2img"
                                )
                        )
                )
        );

        ResponseEntity<Map> response = restTemplate.postForEntity(
                config.getComfyUrl() + "/prompt", request, Map.class
        );

        String promptId = (String) response.getBody().get("prompt_id");
        Map promptData  = waitForResult(promptId);
        Map outputs     = (Map) promptData.get("outputs");

        return extractImageUrl(outputs);
    }
}