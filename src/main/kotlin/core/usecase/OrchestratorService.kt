package core.usecase

import core.domain.model.ConversationContext
import core.domain.model.Message
import core.domain.model.Role
import core.ports.LlmService
import core.ports.LocalLlmService
import core.ports.McpService
import core.ports.RagService
import core.ports.UserProfileRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class OrchestratorService(
    private val llmService: LlmService,
    private val localLlmService: LocalLlmService,
    private val ragService: RagService,
    private val mcpService: McpService,
    private val userProfileRepository: UserProfileRepository
) {
    private val messages = mutableListOf<Message>()

    private val GREEN_THRESHOLD = 0.52
    private val GREY_THRESHOLD = 0.45
    private val MIN_GAP = 0.02

    suspend fun processUserMessage(
        input: String,
        isExpertMode: Boolean = false,
        isPrivacyMode: Boolean = false
    ): Message {
        val userMessage = Message(Role.USER, input)
        messages.add(userMessage)

        val profile = userProfileRepository.loadProfile()

        val capabilities = mutableListOf(
            "Access to RAG Knowledge Base (documentation)",
            "Privacy Mode (Local LLM only)",
            "Expert Mode (Parallel Cloud + Local analysis)"
        )

        val mcpTools = mcpService.getAvailableTools()
        if (mcpTools.isNotEmpty()) {
            capabilities.add("MCP Tools (To use, output JSON: {\"tool\": \"name\", \"params\": {...}}):")
            mcpTools.forEach { tool ->
                capabilities.add("  - ${tool.name}: ${tool.description} (params: ${tool.parameters.joinToString()})")
            }
        }

        var context = ConversationContext(messages, profile, capabilities)

        if (isExpertMode) {
            return coroutineScope {
                val cloudDeferred = async { safeCallCloud(context) }
                val localDeferred = async { safeCallLocal(context) }
                val combined = "📊 **Expert Mode**\n\n☁️ **Cloud:**\n${cloudDeferred.await().content}\n\n🏠 **Local:**\n${localDeferred.await().content}"
                val result = Message(Role.ASSISTANT, combined)
                messages.add(result)
                result
            }
        }

        // === RAG Logic ===
        var ragZone: String? = null
        var ragScore: Double? = null
        var ragSource: String? = null

        val ragResult = ragService.retrieve(input)

        if (ragResult != null) {
            val scoreGap = ragResult.scoreGap ?: 0.0
            val maxScore = ragResult.maxScore

            val zone = when {
                maxScore >= GREEN_THRESHOLD && scoreGap >= MIN_GAP -> "green"
                maxScore >= GREY_THRESHOLD -> "grey"
                else -> "red"
            }

            //println("🔍 RAG Debug: MaxScore=${String.format("%.3f", maxScore)}, Gap=${String.format("%.3f", scoreGap)} -> ZONE: ${zone.uppercase()}")
            //println("📜 Context Preview: ${ragResult.contextText.take(200).replace("\n", " ")}...")

            when (zone) {
                "green" -> {
                    val systemPrompt = "📚 **Trusted Knowledge Base**\n" +
                            "Use the following context to answer the user's question. \n" +
                            "Priority: HIGH. Base your answer strictly on this information if possible.\n\n" +
                            ragResult.contextText
                    messages.add(Message(Role.SYSTEM, systemPrompt))
                    context = ConversationContext(messages, profile, capabilities)

                    ragZone = "✅ GREEN"
                    ragScore = maxScore
                    ragSource = extractSourceFromContext(ragResult.contextText)
                }
                "grey" -> {
                    val cautionText = "⚠️ **Potential Context** (score=${String.format("%.2f", maxScore)})\n" +
                            "The following context might be relevant. Check if it contains the answer.\n" +
                            "If it helps, use it. If it's irrelevant, answer using your general knowledge.\n\n" +
                            ragResult.contextText
                    messages.add(Message(Role.SYSTEM, cautionText))
                    context = ConversationContext(messages, profile, capabilities)

                    ragZone = "⚠️ GREY"
                    ragScore = maxScore
                    ragSource = extractSourceFromContext(ragResult.contextText)
                }
                "red" -> {
                    println("   -> Skipping RAG context (too low score)")
                    ragZone = "❌ RED (not used)"
                    ragScore = maxScore
                }
            }
        } else {
            println("🔍 RAG Debug: No result or Index missing.")
        }

        // === Выбор LLM ===
        var response = if (isPrivacyMode) {
            safeCallLocal(context)
        } else {
            safeCallCloud(context)
        }

        // === Tool Call Handling ===
        if (isToolCall(response.content)) {
            val toolResult = mcpService.executeTool(response.content)
            messages.add(response)
            messages.add(toolResult)
            context = ConversationContext(messages, profile, capabilities)

            val finalResponse = if (isPrivacyMode) {
                safeCallLocal(context)
            } else {
                safeCallCloud(context)
            }
            response = finalResponse
        }

        // === Добавляем RAG Footer ===
        if (ragZone != null) {
            val footer = buildString {
                append("\n\n---\n")
                append("🔍 **RAG Status:** $ragZone")
                if (ragScore != null) {
                    append(" | Score: ${String.format("%.2f", ragScore)}")
                }
                if (ragSource != null) {
                    append(" | Source: `$ragSource`")
                }
            }
            response = Message(response.role, response.content + footer)
        }

        messages.add(response)
        return response
    }

    private fun extractSourceFromContext(contextText: String): String? {
        // Извлекаем имя файла из строки типа "**[1] chunking.txt** (Score: 0.54)"
        val regex = """\*\*\[1\] ([^\*]+)\*\*""".toRegex()
        return regex.find(contextText)?.groupValues?.getOrNull(1)?.trim()
    }

    private fun isToolCall(content: String): Boolean {
        val trimmed = content.trim()
        return trimmed.startsWith("{") && trimmed.contains("\"tool\"")
    }

    private suspend fun safeCallCloud(context: ConversationContext): Message {
        return try {
            llmService.generateResponse(context)
        } catch (e: Exception) {
            Message(Role.ASSISTANT, "Cloud Error: ${e.message}")
        }
    }

    private suspend fun safeCallLocal(context: ConversationContext): Message {
        return try {
            if (localLlmService.isAvailable()) {
                localLlmService.generateResponse(context)
            } else {
                Message(Role.ASSISTANT, "Local Service Unavailable")
            }
        } catch (e: Exception) {
            Message(Role.ASSISTANT, "Local Error: ${e.message}")
        }
    }
}
