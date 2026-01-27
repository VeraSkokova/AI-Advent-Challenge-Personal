package config

import java.io.File
import java.io.FileInputStream
import java.util.Properties

data class VoiceConfig(
    val ffmpegPath: String,
    val whisperPath: String,
    val whisperModelPath: String,
    val audioInputDevice: String
) {
    companion object {
        fun load(fileName: String = "local.properties"): VoiceConfig {
            val props = Properties()
            val file = File(fileName)

            if (file.exists()) {
                FileInputStream(file).use { props.load(it) }
            }

            val ffmpegPath = System.getenv("FFMPEG_PATH")
                ?: props.getProperty("ffmpeg.path")
                ?: "ffmpeg" // Default to PATH

            val whisperPath = System.getenv("WHISPER_MAIN_PATH")
                ?: props.getProperty("whisper.main.path")
                ?: "main.exe" // Default relative or in PATH

            val whisperModelPath = System.getenv("WHISPER_MODEL_PATH")
                ?: props.getProperty("whisper.model.path")
                ?: "ggml-model.bin" // Default relative

            // Default to a common Windows default or let user specify
            val audioInputDevice = System.getenv("AUDIO_INPUT_DEVICE")
                ?: props.getProperty("audio.input.device")
                ?: "audio=Microphone (Realtek(R) Audio)" 

            return VoiceConfig(ffmpegPath, whisperPath, whisperModelPath, audioInputDevice)
        }
    }
}
