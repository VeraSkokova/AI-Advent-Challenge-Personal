package data.mappers

import core.domain.model.ConversationContext
import core.domain.model.Message
import core.domain.model.Role
import core.usecase.PromptFactory
import data.dto.yandex.CompletionOptions
import data.dto.yandex.YandexMessage
import data.dto.yandex.YandexRequest
import data.dto.yandex.YandexResponse
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

object YandexMapper {
    fun toRequest(context: ConversationContext, modelUri: String): YandexRequest {
        val systemPrompt = PromptFactory.createSystemPrompt(context.userPreferences, context.capabilities)
        
        val yandexMessages = mutableListOf<YandexMessage>()
        
        yandexMessages.add(YandexMessage(role = "system", text = systemPrompt))
        
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
            
        // Handle native Tool Calls from Yandex
        val toolCalls = firstAlternative.message.toolCallList?.toolCalls
        if (!toolCalls.isNullOrEmpty()) {
            val call = toolCalls.first().functionCall
            
            // Map Yandex Function Call to our internal JSON Protocol
            val internalJson = buildJsonObject {
                put("tool", call.name)
                put("params", call.arguments)
            }.toString()
            
            return Message(Role.ASSISTANT, internalJson)
        }

        return Message(
            role = Role.ASSISTANT,
            content = firstAlternative.message.text
        )
    }
}
