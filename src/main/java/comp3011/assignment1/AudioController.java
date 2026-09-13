package comp3011.assignment1;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1")
public class AudioController {

    @PostMapping("/transcribe")
    public ResponseEntity<String> receiveAudio(
            @RequestParam("audio") MultipartFile audio) {

        System.out.println(
                "Received audio: " + audio.getOriginalFilename()
                + " (" + audio.getSize() + " bytes)"
        );

        return ResponseEntity.ok("Audio received successfully");
    }
}