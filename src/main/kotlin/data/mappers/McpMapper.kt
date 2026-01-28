package data.mappers

import core.domain.model.Message
import core.domain.model.Role
import data.dto.mcp.McpExecuteResponse

object McpMapper {
    fun toMessage(response: McpExecuteResponse): Message {
        val content = if (response.error != null) {
            "Tool execution error: ${response.error}\nPartial result: ${response.result}"
        } else {
            response.result
        }
        
        return Message(
            role = Role.SYSTEM, // Tools are usually system-level outputs
            content = content
        )
    }
}
