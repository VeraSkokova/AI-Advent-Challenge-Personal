package infra.local

import core.domain.model.ConversationContext
import core.domain.model.Message
import core.ports.LocalLlmService
import data.dto.local.LocalChatResponse
import data.mappers.LocalMapper
import infra.config.AppConfig
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class LocalLlmServiceImpl(
    private val config: AppConfig
) : LocalLlmService {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(json)
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 120_000 // Local models can be slow
        }
    }

    override suspend fun generateResponse(context: ConversationContext): Message {
        val requestDto = LocalMapper.toRequest(context, config.local.modelName)
        
        try {
            val endpoint = if (config.local.baseUrl.endsWith("/")) {
                "${config.local.baseUrl}chat"
            } else {
                "${config.local.baseUrl}/chat"
            }

            // Debug logging
            println("🏠 Calling Local LLM at $endpoint with model ${config.local.modelName}")

            val response = client.post(endpoint) {
                contentType(ContentType.Application.Json)
                setBody(requestDto)
            }
            
            val responseBody = response.bodyAsText()
            // println("🏠 Raw Response: $responseBody") // Uncomment for deep debug

            val chatResponse = json.decodeFromString<LocalChatResponse>(responseBody)
            return LocalMapper.toMessage(chatResponse)

        } catch (e: Exception) {
            throw RuntimeException("Local LLM API Error: ${e.message}", e)
        }
    }

    override suspend fun isAvailable(): Boolean {
        return true
    }
}
