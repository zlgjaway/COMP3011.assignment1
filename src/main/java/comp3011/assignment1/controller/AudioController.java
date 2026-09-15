package comp3011.assignment1.controller;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1")
public class AudioController {

    @PostMapping("/transcribe")
    public ResponseEntity<String> transcribe(
            @RequestParam("audio") MultipartFile audio) {

        try {

            // 1. Get API key from environment variable
            String apiKey = System.getenv("OPENAI_API_KEY");
            
            System.out.println(
            	    "API key loaded: " + (apiKey != null && !apiKey.isBlank())
            	);
            
            if (apiKey == null || apiKey.isBlank()) {
                return ResponseEntity.internalServerError()
                        .body("OPENAI_API_KEY is not set.");
            }

            // 2. Save uploaded audio temporarily
            String filename = "recording-" + UUID.randomUUID() + ".webm";

            Path tempFile = Files.createTempFile(
                    "openai-audio-",
                    ".webm"
            );

            audio.transferTo(tempFile.toFile());

            // 3. Read audio bytes
            byte[] audioBytes = Files.readAllBytes(tempFile);

            // 4. Create multipart boundary
            String boundary = "----JavaBoundary" + UUID.randomUUID();

            String model = "gpt-4o-mini-transcribe";

            // 5. Build multipart request body
            byte[] requestBody = buildMultipartBody(
                    boundary,
                    model,
                    filename,
                    audioBytes
            );

            // 6. Create HTTP client
            HttpClient client = HttpClient.newHttpClient();

            // 7. Send request to OpenAI
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(
                            "https://api.openai.com/v1/audio/transcriptions"
                    ))
                    .header(
                            "Authorization",
                            "Bearer " + apiKey
                    )
                    .header(
                            "Content-Type",
                            "multipart/form-data; boundary=" + boundary
                    )
                    .POST(
                            HttpRequest.BodyPublishers.ofByteArray(
                                    requestBody
                            )
                    )
                    .build();

            HttpResponse<String> response =
                    client.send(
                            request,
                            HttpResponse.BodyHandlers.ofString()
                    );

            // 8. Delete temporary file
            Files.deleteIfExists(tempFile);

            // 9. Check OpenAI response
            if (response.statusCode() < 200
                    || response.statusCode() >= 300) {

                System.out.println(
                        "OpenAI error: " + response.body()
                );

                return ResponseEntity
                        .status(response.statusCode())
                        .body(response.body());
            }

            // 10. Return OpenAI response to browser
            return ResponseEntity.ok(response.body());

        } catch (Exception e) {

            e.printStackTrace();

            return ResponseEntity.internalServerError()
                    .body("Transcription failed: " + e.getMessage());
        }
    }


    private byte[] buildMultipartBody(
            String boundary,
            String model,
            String filename,
            byte[] audioBytes
    ) throws IOException {

        byte[] prefix = (
                "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; "
                + "name=\"model\"\r\n\r\n"
                + model + "\r\n"

                + "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; "
                + "name=\"file\"; "
                + "filename=\"" + filename + "\"\r\n"
                + "Content-Type: audio/webm\r\n\r\n"
        ).getBytes();

        byte[] suffix = (
                "\r\n--" + boundary + "--\r\n"
        ).getBytes();

        byte[] body = new byte[
                prefix.length
                + audioBytes.length
                + suffix.length
        ];

        System.arraycopy(
                prefix,
                0,
                body,
                0,
                prefix.length
        );

        System.arraycopy(
                audioBytes,
                0,
                body,
                prefix.length,
                audioBytes.length
        );

        System.arraycopy(
                suffix,
                0,
                body,
                prefix.length + audioBytes.length,
                suffix.length
        );

        return body;
    }
}