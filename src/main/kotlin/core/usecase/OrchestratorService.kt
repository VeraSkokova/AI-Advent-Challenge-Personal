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
        val userMessage = Message(Role.USER, input)
        messages.add(userMessage)

        val profile = userProfileRepository.loadProfile()
        
        // Pass MCP tools into system prompt (capabilities) so LLM knows about them.
        val capabilities = mutableListOf(
            "Access to RAG Knowledge Base (documentation)",
            "Privacy Mode (Local LLM only)",
            "Expert Mode (Parallel Cloud + Local analysis)"
        )
        
        val mcpTools = mcpService.getAvailableTools()
        if (mcpTools.isNotEmpty()) {
            capabilities.add("MCP Tools (You can call these by outputting JSON: {\"tool\": \"name\", \"params\": {...}}):")
            mcpTools.forEach { tool ->
                capabilities.add("  - ${tool.name}: ${tool.description} (params: ${tool.parameters.joinToString()})")
            }
        }
        
        val context = ConversationContext(messages, profile, capabilities)

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

        val toolType = router.determineTool(input, forcePrivacy = isPrivacyMode)

        // If Router suggests MCP or Local/Cloud LLM, we let the LLM generate the response.
        // If the LLM generates a JSON tool call, we catch it and execute it.
        // This follows the pattern: LLM (with tools in prompt) -> JSON -> Execute -> Result -> LLM (optional) or Return
        
        val response = if (toolType == ToolType.RAG) {
            ragService.searchAndAnswer(input)
        } else {
            // Default path: Ask LLM (Local or Cloud)
            val initialResponse = if (isPrivacyMode || toolType == ToolType.LOCAL_LLM) {
                safeCallLocal(context)
            } else {
                safeCallCloud(context)
            }
            
            // Check if response is a tool call
            if (isToolCall(initialResponse.content)) {
                val toolResult = mcpService.executeTool(initialResponse.content)
                // Optionally feed result back to LLM, but for now just return the tool output
                // or a simple "Done: <output>"
                toolResult
            } else {
                initialResponse
            }
        }

        messages.add(response)
        return response
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
