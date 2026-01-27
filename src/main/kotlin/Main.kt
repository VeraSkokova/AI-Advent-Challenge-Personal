import client.UniversalGptClient
import config.ApiConfig
import config.VoiceConfig
import model.Message
import service.AudioRecorder
import service.SpeechService
import java.io.File
import java.util.Scanner
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    // 1. Загрузка конфигурации
    println("Загрузка конфигурации...")
    val apiConfig = try {
        ApiConfig.load()
    } catch (e: Exception) {
        println("Ошибка загрузки API config: ${e.message}")
        return@runBlocking
    }
    
    val voiceConfig = try {
        VoiceConfig.load()
    } catch (e: Exception) {
        println("Ошибка загрузки Voice config: ${e.message}")
        println("Убедитесь, что local.properties содержит пути к ffmpeg и whisper.")
        return@runBlocking
    }

    println("=== Day 31: Голосовой агент ===")
    println("FFmpeg: ${voiceConfig.ffmpegPath}")
    println("Whisper: ${voiceConfig.whisperPath}")
    println("Device: ${voiceConfig.audioInputDevice}")
    println("===============================")

    // 2. Инициализация сервисов
    val client = UniversalGptClient(apiConfig)
    val recorder = AudioRecorder(voiceConfig)
    val speechService = SpeechService(voiceConfig)
    val scanner = Scanner(System.`in`)

    val history = mutableListOf<Message>()
    val tempWav = File("temp_recording.wav")

    // 3. Основной цикл
    while (true) {
        println("\nНажмите ENTER, чтобы начать запись (или введите 'exit' для выхода):")
        val input = scanner.nextLine()
        if (input.trim().equals("exit", ignoreCase = true)) {
            break
        }

        // --- Запись ---
        println(">> Запись идет... Нажмите ENTER для остановки.")
        recorder.startRecording(tempWav)
        
        // Ждем нажатия Enter
        scanner.nextLine()
        
        recorder.stopRecording()

        if (!tempWav.exists() || tempWav.length() == 0L) {
            println("Ошибка: Файл записи не создан или пуст.")
            continue
        }

        // --- Распознавание (STT) ---
        println(">> Распознавание...")
        val userText = speechService.transcribe(tempWav)
        
        if (userText.startsWith("Error")) {
            println(userText)
            continue
        }

        if (userText.isBlank()) {
            println("Тишина (текст не распознан).")
            continue
        }

        println("Вы сказали: $userText")

        // --- Генерация ответа (LLM) ---
        println(">> Ассистент думает...")
        history.add(Message("user", userText))

        val responseText = client.sendMessage(
            messages = history,
            systemPrompt = "Ты — голосовой помощник. Отвечай кратко, емко и дружелюбно. Не используй сложное форматирование, так как текст будет озвучен.",
            maxTokens = 500
        )

        println("Ассистент: $responseText")
        history.add(Message("assistant", responseText))
    }

    // Очистка
    if (tempWav.exists()) tempWav.delete()
    client.close()
    println("Программа завершена.")
}
