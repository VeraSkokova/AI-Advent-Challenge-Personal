package data.mappers

import core.domain.model.ConversationContext
import core.domain.model.Message
import core.domain.model.Role
import core.usecase.PromptFactory
import data.dto.yandex.CompletionOptions
import data.dto.yandex.YandexMessage
import data.dto.yandex.YandexRequest
import data.dto.yandex.YandexResponse

object YandexMapper {
    fun toRequest(context: ConversationContext, modelUri: String): YandexRequest {
        // Pass capabilities from context to factory
        val systemPrompt = PromptFactory.createSystemPrompt(context.userPreferences, context.capabilities)
        
        val yandexMessages = mutableListOf<YandexMessage>()
        
        // Add System Prompt first
        yandexMessages.add(YandexMessage(role = "system", text = systemPrompt))
        
        // Add conversation history
        yandexMessages.addAll(context.messages.map { msg ->
            YandexMessage(
                role = when (msg.role) {
                    Role.USER -> "user"
                    Role.ASSISTANT -> "assistant"
                    Role.SYSTEM -> "system"
                },
                text = msg.content
            )
        })
        
        return YandexRequest(
            modelUri = modelUri,
            completionOptions = CompletionOptions(),
            messages = yandexMessages
        )
    }

    fun toMessage(response: YandexResponse): Message {
        val firstAlternative = response.result.alternatives.firstOrNull()
            ?: return Message(Role.ASSISTANT, "No response from Yandex")
            
        return Message(
            role = Role.ASSISTANT,
            content = firstAlternative.message.text
        )
    }
}
