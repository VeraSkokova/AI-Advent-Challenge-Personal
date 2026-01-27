package service

import config.VoiceConfig
import java.io.File
import java.util.concurrent.TimeUnit

class SpeechService(private val config: VoiceConfig) {

    fun transcribe(audioFile: File): String {
        if (!audioFile.exists()) {
            return "Error: Audio file not found."
        }

        // Expected output file by whisper.cpp when using -otxt is input_filename.txt
        val outputTxtFile = File(audioFile.absolutePath + ".txt")
        if (outputTxtFile.exists()) {
            outputTxtFile.delete()
        }

        val command = listOf(
            config.whisperPath,
            "-m", config.whisperModelPath,
            "-f", audioFile.absolutePath,
            "-l", "ru",
            "-otxt"
        )
        
        try {
            val processBuilder = ProcessBuilder(command)
            processBuilder.redirectErrorStream(true) 
            
            val process = processBuilder.start()
            
            // Consume output in background to prevent blocking
            val readerThread = Thread {
                process.inputStream.bufferedReader().forEachLine { 
                    // Silent consumption
                }
            }
            readerThread.start()

            val finished = process.waitFor(60, TimeUnit.SECONDS)
            readerThread.join(1000)

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
