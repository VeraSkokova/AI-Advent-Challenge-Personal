package data.mappers

import core.domain.model.ConversationContext
import core.domain.model.Message
import core.domain.model.Role
import data.dto.local.LocalCompletionRequest
import data.dto.local.LocalCompletionResponse

object LocalMapper {
    fun toRequest(context: ConversationContext, modelName: String): LocalCompletionRequest {
        // Simple strategy: concatenate last few messages or just send the last one as prompt
        // For 'instruct' models, we might need a specific format, but here we keep it simple
        val lastMessage = context.messages.lastOrNull()?.content ?: ""
        
        return LocalCompletionRequest(
            model = modelName,
            prompt = lastMessage,
            stream = false
        )
    }

    fun toMessage(response: LocalCompletionResponse): Message {
        return Message(
            role = Role.ASSISTANT,
            content = response.response
        )
    }
}
