package model

import kotlinx.serialization.Serializable

@Serializable
data class YandexRequest(
    val modelUri: String,
    val completionOptions: CompletionOptions,
    val messages: List<Message>
)

@Serializable
data class CompletionOptions(
    val stream: Boolean,
    val temperature: Double,
    val maxTokens: String
)

@Serializable
data class Message(
    val role: String,
    val text: String
)

@Serializable
data class YandexResponse(
    val result: Result
)

@Serializable
data class Result(
    val alternatives: List<Alternative>
)

@Serializable
data class Alternative(
    val message: Message,
    val status: String
)
