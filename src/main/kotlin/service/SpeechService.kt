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
        
        println("DEBUG: Executing Whisper: ${command.joinToString(" ")}")

        try {
            val processBuilder = ProcessBuilder(command)
            processBuilder.redirectErrorStream(true) 
            
            val process = processBuilder.start()
            
            val outputLog = StringBuilder()
            
            val readerThread = Thread {
                process.inputStream.bufferedReader().forEachLine { 
                    outputLog.appendLine(it)
                }
            }
            readerThread.start()

            val finished = process.waitFor(60, TimeUnit.SECONDS)
            readerThread.join(1000) // Wait for logs to be consumed

            if (!finished) {
                process.destroy()
                return "Error: Transcription timed out.\nLogs:\n$outputLog"
            }

            if (process.exitValue() != 0) {
                 return "Error: Whisper process failed with code ${process.exitValue()}.\nLogs:\n$outputLog"
            }

            if (outputTxtFile.exists()) {
                val text = outputTxtFile.readText().trim()
                outputTxtFile.delete()
                return text
            } else {
                return "Error: Output file not created.\nLogs:\n$outputLog"
            }

        } catch (e: Exception) {
            e.printStackTrace()
            return "Error: ${e.message}"
        }
    }
}
