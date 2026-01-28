package data.mappers

import core.domain.model.ConversationContext
import core.domain.model.Message
import core.domain.model.Role
import core.usecase.PromptFactory
import data.dto.local.LocalChatRequest
import data.dto.local.LocalChatResponse
import data.dto.local.LocalMessage

object LocalMapper {
    fun toRequest(context: ConversationContext, modelName: String): LocalChatRequest {
        val systemPrompt = PromptFactory.createSystemPrompt(context.userPreferences)
        
        val messages = mutableListOf<LocalMessage>()
        
        // Add System Prompt first
        messages.add(LocalMessage(role = "system", content = systemPrompt))
        
        // Add conversation history
        messages.addAll(context.messages.map { msg ->
            LocalMessage(
                role = when (msg.role) {
                    Role.USER -> "user"
                    Role.ASSISTANT -> "assistant"
                    Role.SYSTEM -> "system"
                },
                content = msg.content
            )
        })

        return LocalChatRequest(
            model = modelName,
            messages = messages,
            stream = false
        )
    }

    fun toMessage(response: LocalChatResponse): Message {
        val content = if (response.error != null) {
            "Ollama Error: ${response.error}"
        } else {
            response.message?.content ?: "Empty response from Local LLM"
        }
        
        return Message(
            role = Role.ASSISTANT,
            content = content
        )
    }
}
