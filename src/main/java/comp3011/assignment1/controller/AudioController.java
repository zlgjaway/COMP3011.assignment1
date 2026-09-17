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

    private final GlobalStats globalStats;

    // Reuse one HTTP client instead of creating a new one for every request
    private final HttpClient client = HttpClient.newHttpClient();

    public AudioController(GlobalStats globalStats) {
        this.globalStats = globalStats;
    }

    @PostMapping("/transcribe")
    public ResponseEntity<String> transcribe(
            @RequestParam("audio") MultipartFile audio) {

        Path tempFile = null;

        try {
            // 2. Save uploaded audio temporarily
            String filename =
                    "recording-" + UUID.randomUUID() + ".webm";

            tempFile = Files.createTempFile(
                    "openai-audio-",
                    ".webm"
            );

            audio.transferTo(tempFile.toFile());

            // 3. Read audio bytes
            byte[] audioBytes =
                    Files.readAllBytes(tempFile);

            // 4. Create multipart boundary
            String boundary =
                    "----JavaBoundary" + UUID.randomUUID();

            String model =
                    "gpt-4o-mini-transcribe";

            // 5. Build multipart request body
            byte[] requestBody =
                    buildMultipartBody(
                            boundary,
                            model,
                            filename,
                            audioBytes
                    );

            // 6. Send request to OpenAI
            HttpRequest request =
                    HttpRequest.newBuilder()
                            .uri(URI.create(
                                    "http://127.0.0.1:8000/v1/audio/transcriptions"
                            ))
                            .header(
                                    "Content-Type",
                                    "multipart/form-data; boundary="
                                            + boundary
                            )
                            .POST(
                                    HttpRequest.BodyPublishers
                                            .ofByteArray(requestBody)
                            )
                            .build();

            HttpResponse<String> response =
                    client.send(
                            request,
                            HttpResponse.BodyHandlers.ofString()
                    );

            // 7. Check OpenAI response
            if (response.statusCode() < 200
                    || response.statusCode() >= 300) {

                return ResponseEntity
                        .status(response.statusCode())
                        .body(response.body());
            }

            // 8. Get the OpenAI response
            String responseBody = response.body();

            /*
             * For now, return the complete OpenAI JSON response.
             * We will parse the transcript and token usage next.
             */
            return ResponseEntity.ok(responseBody);

        } catch (Exception e) {

            return ResponseEntity.internalServerError()
                    .body("Transcription failed: "
                            + e.getMessage());

        } finally {

            // Always delete temporary file
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException ignored) {
                    // Nothing else to do
                }
            }
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