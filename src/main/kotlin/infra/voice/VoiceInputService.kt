package infra.voice

import config.VoiceConfig
import core.ports.VoiceInputProvider
import service.AudioRecorder
import service.SpeechService
import java.io.File

class VoiceInputService(
    private val voiceConfig: VoiceConfig
) : VoiceInputProvider {
    private val recorder = AudioRecorder(voiceConfig)
    private val speechService = SpeechService(voiceConfig)
    private val tempWav = File("temp_recording.wav")

    override suspend fun recordAndTranscribe(): String {
        println(">> Инициализация записи...")
        recorder.startRecording(tempWav)
        
        Thread.sleep(500) // Пауза для запуска процесса
        
        println(">> ЗАПИСЬ ИДЕТ. Говорите в микрофон!")
        println(">> (Нажмите ENTER чтобы остановить запись)")
        
        readlnOrNull() // Ждем Enter
        
        println(">> Остановка...")
        recorder.stopRecording()

        if (!tempWav.exists() || tempWav.length() == 0L) {
            return "Error: Файл записи пуст."
        }

        println(">> Распознавание (Whisper)...")
        val userText = speechService.transcribe(tempWav)
        
        // Очистка
        if (tempWav.exists()) tempWav.delete()
        
        return userText
    }
}
