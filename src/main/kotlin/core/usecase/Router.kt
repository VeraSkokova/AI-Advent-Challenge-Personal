package core.usecase

import core.domain.model.ToolType
import core.ports.McpService
import core.ports.RagService

class Router(
    private val ragService: RagService,
    private val mcpService: McpService
) {
    suspend fun determineTool(query: String, forceLocal: Boolean = false, forcePrivacy: Boolean = false): ToolType {
        if (forcePrivacy) {
            return ToolType.LOCAL_LLM
        }

        if (forceLocal) {
            return ToolType.LOCAL_LLM
        }

        // Privacy check by keywords (simple heuristic)
        val privacyKeywords = listOf("secret", "private", "confidential", "password", "key", "приватно", "секрет")
        if (privacyKeywords.any { query.contains(it, ignoreCase = true) }) {
            return ToolType.LOCAL_LLM
        }

        // Check for MCP tools
        if (mcpService.needsTool(query)) {
            return ToolType.MCP
        }

        // Check for Documentation/RAG
        if (ragService.isRelevant(query)) {
            return ToolType.RAG
        }

        // Default to Cloud LLM
        return ToolType.CLOUD_LLM
    }
}
