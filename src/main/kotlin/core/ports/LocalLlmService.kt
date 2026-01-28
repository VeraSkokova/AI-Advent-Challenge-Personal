package core.ports

import core.domain.model.ConversationContext
import core.domain.model.Message

interface LocalLlmService {
    suspend fun generateResponse(context: ConversationContext): Message
    suspend fun isAvailable(): Boolean
}
