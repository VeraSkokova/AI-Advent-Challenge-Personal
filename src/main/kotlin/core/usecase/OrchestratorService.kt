package core.usecase

import core.domain.model.ConversationContext
import core.domain.model.Message
import core.domain.model.Role
import core.domain.model.ToolType
import core.ports.LlmService
import core.ports.LocalLlmService
import core.ports.McpService
import core.ports.RagService
import core.ports.UserProfileRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class OrchestratorService(
    private val router: Router,
    private val llmService: LlmService,
    private val localLlmService: LocalLlmService,
    private val ragService: RagService,
    private val mcpService: McpService,
    private val userProfileRepository: UserProfileRepository
) {
    private val messages = mutableListOf<Message>()

    suspend fun processUserMessage(
        input: String,
        isExpertMode: Boolean = false,
        isPrivacyMode: Boolean = false
    ): Message {
        // 1. Save user message
        val userMessage = Message(Role.USER, input)
        messages.add(userMessage)

        // 2. Load context
        val profile = userProfileRepository.loadProfile()
        val context = ConversationContext(messages, profile)

        // 3. Expert Mode: Run Cloud and Local in parallel
        if (isExpertMode) {
            return coroutineScope {
                val cloudDeferred = async { safeCallCloud(context) }
                val localDeferred = async { safeCallLocal(context) }

                val cloudResponse = cloudDeferred.await()
                val localResponse = localDeferred.await()

                val combinedContent = """
                    📊 **Expert Mode Analysis**
                    
                    ☁️ **Cloud Perspective:**
                    ${cloudResponse.content}
                    
                    🏠 **Local Perspective:**
                    ${localResponse.content}
                """.trimIndent()
                
                val result = Message(Role.ASSISTANT, combinedContent)
                messages.add(result)
                result
            }
        }

        // 4. Determine strategy via Router
        val toolType = router.determineTool(input, forcePrivacy = isPrivacyMode)

        // 5. Execute Strategy
        val response = when (toolType) {
            ToolType.LOCAL_LLM -> {
                safeCallLocal(context)
            }
            ToolType.RAG -> {
                ragService.searchAndAnswer(input)
            }
            ToolType.MCP -> {
                mcpService.executeTool(input)
            }
            ToolType.CLOUD_LLM -> {
                try {
                    if (llmService.isAvailable()) {
                        llmService.generateResponse(context)
                    } else {
                        // Fallback to Local
                        println("⚠️ Cloud LLM unavailable (config check), falling back to Local")
                        safeCallLocal(context)
                    }
                } catch (e: Exception) {
                    // Fallback on error
                    println("⚠️ Cloud LLM error: ${e.message}, falling back to Local")
                    e.printStackTrace()
                    safeCallLocal(context)
                }
            }
        }

        messages.add(response)
        return response
    }

    private suspend fun safeCallCloud(context: ConversationContext): Message {
        return try {
            llmService.generateResponse(context)
        } catch (e: Exception) {
            println("⚠️ Error in safeCallCloud: ${e.message}")
            Message(Role.ASSISTANT, "Cloud Service Error: ${e.message}")
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
            println("⚠️ Error in safeCallLocal: ${e.message}")
            Message(Role.ASSISTANT, "Local Service Error: ${e.message}")
        }
    }
}
