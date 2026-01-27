package service

import config.VoiceConfig
import java.io.File
import java.util.concurrent.TimeUnit

class AudioRecorder(private val config: VoiceConfig) {

    private var process: Process? = null

    fun startRecording(outputFile: File) {
        if (outputFile.exists()) {
            outputFile.delete()
        }

        // Prepare command
        // ffmpeg -f dshow -i audio="Device Name" -ar 16000 -ac 1 -c:a pcm_s16le -y output.wav
        // Note: -y to overwrite
        val command = listOf(
            config.ffmpegPath,
            "-f", "dshow",
            "-i", config.audioInputDevice,
            "-ar", "16000",
            "-ac", "1",
            "-c:a", "pcm_s16le",
            "-y",
            outputFile.absolutePath
        )

        try {
            val processBuilder = ProcessBuilder(command)
            processBuilder.redirectErrorStream(true) // Merge stdout and stderr
            // We don't inherit IO for stdin/stdout because we want to control them
            // But we might want to see logs? Let's redirect output to null or inherit if debugging
            // For now, let's keep it quiet or print to console if needed.
            // processBuilder.inheritIO() // If we inherit IO, we might lose control of stdin for "q"
            
            process = processBuilder.start()
            println("Recording started... (Process ID: ${process?.pid()})")
            
            // Consume output in a separate thread to prevent buffer blocking
            Thread {
                process?.inputStream?.bufferedReader()?.forEachLine { 
                    // println("FFMPEG: $it") // Uncomment for debug
                }
            }.start()

        } catch (e: Exception) {
            println("Error starting ffmpeg: ${e.message}")
            e.printStackTrace()
        }
    }

    fun stopRecording() {
        val p = process
        if (p != null && p.isAlive) {
            try {
                // Send 'q' to stop recording gracefully
                val writer = p.outputStream.bufferedWriter()
                writer.write("q")
                writer.newLine()
                writer.flush()
                writer.close()
                
                // Wait for it to finish
                val finished = p.waitFor(5, TimeUnit.SECONDS)
                if (!finished) {
                    println("FFmpeg didn't stop gracefully, killing...")
                    p.destroy()
                }
            } catch (e: Exception) {
                println("Error stopping recording: ${e.message}")
                p.destroy()
            }
        }
        process = null
        println("Recording stopped.")
    }
}
