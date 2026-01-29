package app.console

import core.usecase.OrchestratorService
import infra.repository.FileUserProfileRepository
import infra.config.AppConfig
import infra.local.LocalLlmServiceImpl
import infra.mcp.McpServiceImpl
import infra.rag.RagServiceImpl
import infra.yandex.YandexLlmService
import infra.voice.VoiceInputService
import config.VoiceConfig
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    // 1. Initialize Configuration
    val config = AppConfig()
    println("🔧 Configuration loaded.")
    
    // Загрузка VoiceConfig
    val voiceConfig = try {
        VoiceConfig.load()
    } catch (e: Exception) {
        println("⚠️ VoiceConfig не загружен (голосовой ввод недоступен): ${e.message}")
        null
    }

    // 2. Initialize Infrastructure Services
    val yandexService = YandexLlmService(config)
    val localService = LocalLlmServiceImpl(config)
    val ragService = RagServiceImpl(config)
    val mcpService = McpServiceImpl(config)
    val userProfileRepo = FileUserProfileRepository()
    
    // Голосовой сервис (опционально)
    val voiceService = voiceConfig?.let { VoiceInputService(it) }
    
    // 3. Initialize Core Interaction
    val interactionService = ConsoleInteractionService()

    // 4. Initialize Core Logic
    val orchestrator = OrchestratorService(
        llmService = yandexService,
        localLlmService = localService,
        ragService = ragService,
        mcpService = mcpService,
        userProfileRepository = userProfileRepo
    )

    // 5. Start Application Loop
    interactionService.writeOutput("🤖 God Agent Initialized. Ready for commands.")
    val commandsHelp = if (voiceService != null) {
        "Commands: /expert [query], /privacy [query], /index [path], /voice, /exit"
    } else {
        "Commands: /expert [query], /privacy [query], /index [path], /exit"
    }
    interactionService.writeOutput(commandsHelp)

    while (true) {
        val input = interactionService.readInput()
        if (input.isBlank()) continue

        if (input.equals("/exit", ignoreCase = true)) {
            interactionService.writeOutput("Goodbye!")
            break
        }
        
        // ===== /voice command =====
        if (input.equals("/voice", ignoreCase = true)) {
            if (voiceService == null) {
                interactionService.writeError("Голосовой ввод недоступен (проверьте local.properties)")
                continue
            }
            
            interactionService.writeOutput("🎙️ Нажмите Enter, чтобы начать запись...")
            readlnOrNull()
            
            val transcribedText = voiceService.recordAndTranscribe()
            
            if (transcribedText.startsWith("Error")) {
                interactionService.writeError(transcribedText)
                continue
            }
            
            if (transcribedText.isBlank() || transcribedText.trim() == "[музыка]") {
                interactionService.writeOutput("Тишина или шум (текст не распознан). Попробуйте еще раз.")
                continue
            }
            
            interactionService.writeOutput("Вы сказали: \"$transcribedText\"")
            
            // Передаем распознанный текст в Orchestrator как обычный текстовый запрос
            try {
                val response = orchestrator.processUserMessage(transcribedText, isExpertMode = false, isPrivacyMode = false)
                interactionService.writeMessage(response)
            } catch (e: Exception) {
                interactionService.writeError("Error processing voice input: ${e.message}")
                e.printStackTrace()
            }
            continue
        }
        // ===== end /voice =====
        
        // Handle /index command specifically
        if (input.startsWith("/index")) {
            val path = input.removePrefix("/index").trim()
            if (path.isBlank()) {
                interactionService.writeError("Please specify a path: /index <path>")
            } else {
                interactionService.writeOutput("🔍 Indexing documents in '$path'...")
                try {
                    val count = ragService.indexer.indexDirectory(path)
                    interactionService.writeOutput("✅ Indexed $count chunks. Saved to index.json")
                } catch (e: Exception) {
                    interactionService.writeError("Indexing failed: ${e.message}")
                    e.printStackTrace()
                }
            }
            continue
        }

        try {
            var query = input
            var isExpert = false
            var isPrivacy = false

            if (input.startsWith("/expert")) {
                isExpert = true
                query = input.removePrefix("/expert").trim()
            } else if (input.startsWith("/privacy")) {
                isPrivacy = true
                query = input.removePrefix("/privacy").trim()
            }

            if (query.isBlank()) {
                interactionService.writeOutput("Please provide a query.")
                continue
            }

            val response = orchestrator.processUserMessage(query, isExpertMode = isExpert, isPrivacyMode = isPrivacy)
            interactionService.writeMessage(response)

        } catch (e: Exception) {
            interactionService.writeError("Critical error: ${e.message}")
            e.printStackTrace()
        }
    }
}
