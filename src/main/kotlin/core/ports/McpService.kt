package core.ports

import core.domain.model.Message

interface McpService {
    suspend fun executeTool(query: String): Message
    suspend fun needsTool(query: String): Boolean
}
