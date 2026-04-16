package image.gen.image.service;



import org.springframework.stereotype.Service;

@Service
public class ImageService {

    public byte[] generateImage(String prompt) {

        String ascii = """
                ==============================
                GENERATED IMAGE (LOCAL)
                ==============================

                Prompt:
                %s

                [This is a placeholder image]

                ==============================
                """.formatted(prompt);

        return ascii.getBytes();
    }
}