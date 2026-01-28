package core.ports

data class ToolInfo(
    val name: String,
    val description: String,
    val parameters: List<String>
)

interface McpService {
    suspend fun executeTool(command: String): core.domain.model.Message
    fun needsTool(query: String): Boolean
    fun getAvailableTools(): List<ToolInfo>
}

// Interface for individual MCP Server implementations (Adapter pattern)
interface McpServerAdapter {
    fun getTools(): List<ToolInfo>
    suspend fun execute(toolName: String, params: Map<String, String>): String
}
