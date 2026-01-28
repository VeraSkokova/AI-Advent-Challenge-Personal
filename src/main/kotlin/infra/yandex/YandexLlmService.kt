package infra.yandex

import core.domain.model.ConversationContext
import core.domain.model.Message
import core.ports.LlmService
import data.dto.yandex.YandexResponse
import data.mappers.YandexMapper
import infra.config.AppConfig
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class YandexLlmService(
    private val config: AppConfig
) : LlmService {

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                ignoreUnknownKeys = true
                encodeDefaults = true
            })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 60_000
        }
    }

    override suspend fun generateResponse(context: ConversationContext): Message {
        val requestDto = YandexMapper.toRequest(context, config.yandex.modelUri)
        
        try {
            val response: YandexResponse = client.post("https://llm.api.cloud.yandex.net/foundationModels/v1/completion") {
                header("Authorization", "Api-Key ${config.yandex.apiKey}")
                contentType(ContentType.Application.Json)
                setBody(requestDto)
            }.body()

            return YandexMapper.toMessage(response)
        } catch (e: Exception) {
            throw RuntimeException("Yandex GPT API Error: ${e.message}", e)
        }
    }

    override suspend fun isAvailable(): Boolean {
        // Simple check or assume true if config is present
        return config.yandex.apiKey.isNotBlank()
    }
}
