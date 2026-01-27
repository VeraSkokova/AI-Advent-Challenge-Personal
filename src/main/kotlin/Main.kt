import client.UniversalGptClient
import config.ApiConfig
import config.VoiceConfig
import model.Message
import service.AudioRecorder
import service.SpeechService
import java.io.File
import java.io.BufferedReader
import java.io.InputStreamReader
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
    
    // Используем BufferedReader вместо Scanner для более надежного чтения строк
    val reader = BufferedReader(InputStreamReader(System.`in`))

    val history = mutableListOf<Message>()
    val tempWav = File("temp_recording.wav")

    // 3. Основной цикл
    while (true) {
        println("\n------------------------------------------------")
        println("Нажмите ENTER, чтобы начать запись (или введите 'exit'):")
        
        val input = reader.readLine() ?: break
        if (input.trim().equals("exit", ignoreCase = true)) {
            break
        }

        // --- Запись ---
        println(">> Инициализация записи...")
        recorder.startRecording(tempWav)
        
        // Пауза, чтобы FFmpeg успел захватить устройство
        Thread.sleep(500)
        
        // Очистка буфера ввода перед началом ожидания остановки
        // Это важно, если пользователь случайно нажал Enter несколько раз
        while (reader.ready()) {
            reader.read()
        }
        
        println(">> ЗАПИСЬ ИДЕТ. Говорите в микрофон!")
        println(">> (Нажмите ENTER чтобы остановить запись)")
        
        // Блокирующее ожидание следующего Enter
        reader.readLine()
        
        println(">> Остановка...")
        recorder.stopRecording()

        // Даем небольшую паузу на финализацию файла
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
