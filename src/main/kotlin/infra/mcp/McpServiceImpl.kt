package infra.mcp

import core.domain.model.Message
import core.ports.McpService
import data.dto.mcp.McpExecuteRequest
import data.dto.mcp.McpExecuteResponse
import data.mappers.McpMapper
import infra.config.AppConfig
import kotlinx.coroutines.delay

class McpServiceImpl(
    private val config: AppConfig
) : McpService {

    // In a real implementation, this would likely connect to an MCP Server via stdio or HTTP
    // For this prototype, we'll simulate a simple tool execution

    override suspend fun executeTool(query: String): Message {
        // Mock execution
        delay(500) 
        
        val response = if (query.contains("time", ignoreCase = true)) {
            McpExecuteResponse(result = "Current system time is: ${java.time.LocalDateTime.now()}")
        } else if (query.contains("calculate", ignoreCase = true)) {
             McpExecuteResponse(result = "Calculation result: 42 (mocked)")
        } else {
             McpExecuteResponse(result = "Unknown tool action for query: $query", error = "ToolNotFound")
        }

        return McpMapper.toMessage(response)
    }

    override suspend fun needsTool(query: String): Boolean {
        if (!config.mcp.enabled) return false
        
        val triggerWords = listOf("calculate", "time", "weather", "stock", "git")
        return triggerWords.any { query.contains(it, ignoreCase = true) }
    }
}
