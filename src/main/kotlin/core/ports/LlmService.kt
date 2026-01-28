package core.ports

import core.domain.model.ConversationContext
import core.domain.model.Message

interface LlmService {
    suspend fun generateResponse(context: ConversationContext): Message
    suspend fun isAvailable(): Boolean
}
