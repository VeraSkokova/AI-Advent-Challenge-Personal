package service

import config.VoiceConfig
import java.io.File
import java.util.concurrent.TimeUnit

class SpeechService(private val config: VoiceConfig) {

    fun transcribe(audioFile: File): String {
        if (!audioFile.exists()) {
            return "Error: Audio file not found."
        }

        // Expected output file by whisper.cpp when using -otxt is input_filename.txt (usually appends .txt)
        // Check whisper.cpp documentation or behavior. Usually input.wav -> input.wav.txt
        val outputTxtFile = File(audioFile.absolutePath + ".txt")
        if (outputTxtFile.exists()) {
            outputTxtFile.delete()
        }

        // main.exe -m [model] -f [file] -l ru -otxt
        val command = listOf(
            config.whisperPath,
            "-m", config.whisperModelPath,
            "-f", audioFile.absolutePath,
            "-l", "ru",
            "-otxt" // Output to text file
        )

        try {
            val processBuilder = ProcessBuilder(command)
            processBuilder.redirectErrorStream(true) 
            
            val process = processBuilder.start()
            
            // Consume stdout to prevent blocking, but we don't really need it if we read the file
            Thread {
                process.inputStream.bufferedReader().forEachLine { 
                    // println("WHISPER: $it") 
                }
            }.start()

            val finished = process.waitFor(60, TimeUnit.SECONDS) // wait up to 60s
            if (!finished) {
                process.destroy()
                return "Error: Transcription timed out."
            }

            if (process.exitValue() != 0) {
                 return "Error: Whisper process failed with code ${process.exitValue()}"
            }

            if (outputTxtFile.exists()) {
                val text = outputTxtFile.readText().trim()
                outputTxtFile.delete()
                return text
            } else {
                return "Error: Output file not created."
            }

        } catch (e: Exception) {
            e.printStackTrace()
            return "Error: ${e.message}"
        }
    }
}
