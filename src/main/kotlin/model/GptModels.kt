package model

import kotlinx.serialization.Serializable

@Serializable
data class Message(
    val role: String,
    val text: String
)

data class GenerationResult(
    val text: String,
    val inputTokens: Int,
    val outputTokens: Int,
    val costRub: Double,
    val durationMs: Long,
    val modelName: String
)

data class Persona(
    val name: String,
    val systemPrompt: String,
    val temperature: Double = 0.6
)

enum class ClientType { YANDEX_NATIVE, OPENAI_COMPATIBLE }

data class ModelConfig(
    val id: String,
    val name: String,
    val uriTemplate: String,
    val inputPrice: Double,
    val outputPrice: Double,
    val clientType: ClientType,
    val supportsJsonMode: Boolean = false
)

object ModelsRepository {
    val YandexPro = ModelConfig(
        id = "yandexgpt",
        name = "YandexGPT Pro",
        uriTemplate = "gpt://%s/yandexgpt/latest",
        inputPrice = 0.40,
        outputPrice = 0.40,
        clientType = ClientType.YANDEX_NATIVE,
        supportsJsonMode = true
    )
}
