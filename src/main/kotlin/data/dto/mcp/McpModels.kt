package data.dto.mcp

import kotlinx.serialization.Serializable

@Serializable
data class McpToolDefinition(
    val name: String,
    val description: String,
    val parameters: Map<String, String> // Simplified for now
)

@Serializable
data class McpExecuteRequest(
    val toolName: String,
    val arguments: Map<String, String>
)

@Serializable
data class McpExecuteResponse(
    val result: String,
    val error: String? = null
)
