package data.mappers

import core.domain.model.ConversationContext
import core.domain.model.Message
import core.domain.model.Role
import data.dto.local.LocalCompletionRequest
import data.dto.local.LocalCompletionResponse

object LocalMapper {
    fun toRequest(context: ConversationContext, modelName: String): LocalCompletionRequest {
        val lastMessage = context.messages.lastOrNull()?.content ?: ""
        
        return LocalCompletionRequest(
            model = modelName,
            prompt = lastMessage,
            stream = false
        )
    }

    fun toMessage(response: LocalCompletionResponse): Message {
        val content = response.response 
            ?: response.message?.content 
            ?: "Empty response from Local LLM"
            
        return Message(
            role = Role.ASSISTANT,
            content = content
        )
    }
}
