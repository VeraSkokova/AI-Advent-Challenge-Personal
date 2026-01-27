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
        println("Убедитесь, что local.properties настроен корректно.")
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
    
    // Возвращаем Scanner, так как BufferedReader иногда требует двойного нажатия на Windows
    val scanner = Scanner(System.`in`)

    val history = mutableListOf<Message>()
    val tempWav = File("temp_recording.wav")

    // 3. Основной цикл
    while (true) {
        println("\n------------------------------------------------")
        println("Нажмите ENTER, чтобы начать запись (или введите 'exit'):")
        
        if (!scanner.hasNextLine()) break
        val input = scanner.nextLine()
        
        if (input.trim().equals("exit", ignoreCase = true)) {
            break
        }

        // --- Запись ---
        println(">> Инициализация записи...")
        recorder.startRecording(tempWav)
        
        // Увеличенная пауза (700мс), чтобы предотвратить мгновенную остановку от случайного двойного нажатия
        Thread.sleep(700)
        
        println(">> ЗАПИСЬ ИДЕТ. Говорите в микрофон!")
        println(">> (Нажмите ENTER чтобы остановить запись)")
        
        // Ждем следующего нажатия Enter
        if (scanner.hasNextLine()) {
            scanner.nextLine()
        }
        
        println(">> Остановка...")
        recorder.stopRecording()

        // Пауза на финализацию файла
        Thread.sleep(500)

        if (!tempWav.exists() || tempWav.length() == 0L) {
            println("Ошибка: Файл записи пуст. Возможно, микрофон не работает или запись была слишком короткой.")
            continue
        }

        // --- Распознавание (STT) ---
        println(">> Распознавание (Whisper)...")
        val userText = speechService.transcribe(tempWav)
        
        if (userText.startsWith("Error")) {
            println("Ошибка распознавания:")
            println(userText)
            continue
        }

        if (userText.isBlank() || userText.trim() == "[музыка]") {
            println("Тишина или шум (текст не распознан). Попробуйте еще раз.")
            continue
        }

        println("Вы сказали: \"$userText\"")

        // --- Генерация ответа (LLM) ---
        println(">> Ассистент думает...")
        history.add(Message("user", userText))

        val responseText = client.sendMessage(
            messages = history,
            systemPrompt = "Ты — голосовой помощник. Отвечай кратко (1-2 предложения), дружелюбно и по делу. Не используй markdown-разметку, так как текст предназначен для озвучки.",
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
