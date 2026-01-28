package data.dto.yandex

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class YandexRequest(
    val modelUri: String,
    val completionOptions: CompletionOptions,
    val messages: List<YandexMessage>
)

@Serializable
data class CompletionOptions(
    val stream: Boolean = false,
    val temperature: Double = 0.6,
    val maxTokens: String = "2000"
)

@Serializable
data class YandexMessage(
    val role: String,
    val text: String = "" // Fix: Default to empty string to avoid "missing field" error
)

@Serializable
data class YandexResponse(
    val result: YandexResult
)

@Serializable
data class YandexResult(
    val alternatives: List<YandexAlternative>,
    val usage: YandexUsage
)

@Serializable
data class YandexAlternative(
    val message: YandexMessage,
    val status: String
)

@Serializable
data class YandexUsage(
    val inputTextTokens: String,
    val completionTokens: String,
    val totalTokens: String
)
