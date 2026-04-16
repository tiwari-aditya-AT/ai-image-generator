package image.gen.image.service;

import java.util.List;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class ComfyUIService {

    private final RestTemplate restTemplate = new RestTemplate();
    public String generateImage(String prompt) throws Exception {

        // STEP 1: Send prompt (your existing payload can stay if working)
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
                                        "negative", List.of("7", 0),
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
                        ),
                        "7", Map.of(
                                "class_type", "CLIPTextEncode",
                                "inputs", Map.of(
                                        "text", "",
                                        "clip", List.of("1", 1)
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

        // STEP 2: WAIT (important)
        Map promptData = null;

        for (int i = 0; i < 20; i++) { // max wait ~20 sec
            Thread.sleep(1000);

            Map history = restTemplate.getForObject(
                    "http://127.0.0.1:8188/history/" + promptId,
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
        Map outputs = (Map) promptData.get("outputs");


        String filename = null;

        for (Object nodeObj : outputs.values()) {
            Map node = (Map) nodeObj;

            Object imagesObj = node.get("images");

            if (imagesObj instanceof List) {
                List images = (List) imagesObj;

                if (!images.isEmpty()) {
                    Object imgObj = images.get(0);

                    if (imgObj instanceof Map) {
                        Map image = (Map) imgObj;
                        filename = (String) image.get("filename");
                        break;
                    }
                }
            }
        }

        if (filename == null) {
            System.out.println("DEBUG OUTPUTS: " + outputs); // VERY IMPORTANT
            throw new RuntimeException("No image found in ComfyUI response");
        }

        // STEP 4: Return image URL
        return "http://127.0.0.1:8188/view?filename=" + filename;
    }

}