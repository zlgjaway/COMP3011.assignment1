let mediaRecorder;
let audioChunks = [];

const startButton = document.getElementById("startButton");
const stopButton = document.getElementById("stopButton");
const status = document.getElementById("status");
const audioPlayer = document.getElementById("audioPlayer");

startButton.addEventListener("click", startRecording);
stopButton.addEventListener("click", stopRecording);

async function startRecording() {
    try {
        const stream = await navigator.mediaDevices.getUserMedia({
            audio: true
        });

        audioChunks = [];

        mediaRecorder = new MediaRecorder(stream);

        mediaRecorder.addEventListener("dataavailable", event => {
            if (event.data.size > 0) {
                audioChunks.push(event.data);
            }
        });

        mediaRecorder.addEventListener("stop", uploadAudio);

        mediaRecorder.start();

        status.textContent = "Recording...";
        startButton.disabled = true;
        stopButton.disabled = false;

    } catch (error) {
        console.error(error);
        status.textContent = "Could not access microphone.";
    }
}

function stopRecording() {
    mediaRecorder.stop();

    status.textContent = "Processing...";
    startButton.disabled = false;
    stopButton.disabled = true;
}

async function uploadAudio() {
    const audioBlob = new Blob(audioChunks, {
        type: mediaRecorder.mimeType
    });

    const audioUrl = URL.createObjectURL(audioBlob);
    audioPlayer.src = audioUrl;

    const formData = new FormData();

    formData.append("audio", audioBlob, "recording.webm");

    try {
        const response = await fetch("/api/v1/transcribe", {
            method: "POST",
            body: formData
        });

        if (!response.ok) {
            throw new Error("Upload failed");
        }

        status.textContent = "Audio successfully received by server.";

    } catch (error) {
        console.error(error);
        status.textContent = "Failed to upload audio.";
    }
}