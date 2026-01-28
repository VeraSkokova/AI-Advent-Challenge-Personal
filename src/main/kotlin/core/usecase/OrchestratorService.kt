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

        val toolType = router.determineTool(input, forcePrivacy = isPrivacyMode)

        val initialResponse = if (toolType == ToolType.RAG) {
            ragService.searchAndAnswer(input)
        } else {
            if (isPrivacyMode || toolType == ToolType.LOCAL_LLM) {
                safeCallLocal(context)
            } else {
                safeCallCloud(context)
            }
        }
        
        // Handle Tool Calls (Recursively or Single-Step)
        if (isToolCall(initialResponse.content)) {
            val toolResult = mcpService.executeTool(initialResponse.content)
            
            // Add tool execution to history
            // We simulate the conversation: 
            // User: "Status?" -> Assistant: "{tool: git...}" -> System: "Output: ..."
            // Then we ask Assistant again for the final answer.
            
            messages.add(initialResponse) // The JSON command
            messages.add(toolResult)      // The Tool Output
            
            // Refresh context with new history
            context = ConversationContext(messages, profile, capabilities)
            
            // Call LLM again to synthesize the final answer
            val finalResponse = if (isPrivacyMode || toolType == ToolType.LOCAL_LLM) {
                safeCallLocal(context)
            } else {
                safeCallCloud(context)
            }
            
            messages.add(finalResponse)
            return finalResponse
        } else {
            messages.add(initialResponse)
            return initialResponse
        }
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
