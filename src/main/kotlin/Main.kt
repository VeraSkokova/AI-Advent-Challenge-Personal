import client.UniversalGptClient
import config.ApiConfig
import kotlinx.serialization.json.Json
import model.Message
import model.UserConfig
import java.io.File
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    // 1. Load Config
    val configFile = File("src/main/resources/user_config.json")
    if (!configFile.exists()) {
        println("Error: user_config.json not found!")
        return@runBlocking
    }
    
    val json = Json { ignoreUnknownKeys = true }
    val configText = configFile.readText()
    val config = json.decodeFromString<UserConfig>(configText)
    
    println("Loaded configuration for user: ${config.userProfile.name}")
    
    // 2. Build System Prompt
    val systemPrompt = buildString {
        append("Ты — персональный AI-ассистент для ${config.userProfile.name}. ")
        append("Она — ${config.userProfile.role}, уровень: ${config.userProfile.level}. ")
        append("Основной язык общения: ${config.communicationStyle.language}. ")
        append("Тон общения: ${config.communicationStyle.tone}. ")
        append("Ожидания пользователя: ${config.communicationStyle.expectations}. ")
        append("\n\nТехнический контекст:\n")
        append("Языки: ${config.technicalContext.primaryLanguages.joinToString(", ")}. ")
        append("Стек Android: ${config.technicalContext.stack.android.joinToString(", ")}. ")
        append("Backend: ${config.technicalContext.stack.backend.joinToString(", ")}. ")
        append("AI инструменты: ${config.technicalContext.stack.aiMl.joinToString(", ")}. ")
        append("Инструменты: ${config.technicalContext.stack.tools.joinToString(", ")}. ")
        append("\n\nАппаратное обеспечение: ${config.technicalContext.hardware}. ")
        if (config.communicationStyle.clarifyingQuestionsRequired) {
            append("\nВАЖНО: Перед выполнением сложной задачи задавай минимум 3 уточняющих вопроса.")
        }
    }
    
    // 3. Initialize Client with ApiConfig
    val apiConfig = try {
        ApiConfig.load()
    } catch (e: Exception) {
        println("Configuration Error: ${e.message}")
        return@runBlocking
    }
    
    val client = UniversalGptClient(apiConfig)
    val chatHistory = mutableListOf<Message>()
    
    println("\n--- Personal AI Agent Started ---")
    println("System Prompt initialized with user profile context.")
    println("Type 'exit' to quit.\n")
    
    // 4. REPL Loop
    while (true) {
        print("> ")
        val userInput = readlnOrNull() ?: break
        
        if (userInput.lowercase() == "exit") {
            break
        }
        
        if (userInput.isBlank()) continue
        
        chatHistory.add(Message("user", userInput))
        
        print("AI: Thinking...")
        val responseText = client.sendMessage(
            messages = chatHistory,
            systemPrompt = systemPrompt
        )
        
        print("\r") 
        println("AI: $responseText")
        
        chatHistory.add(Message("assistant", responseText))
    }
    
    client.close()
    println("Goodbye!")
}
