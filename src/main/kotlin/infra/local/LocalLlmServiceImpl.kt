package infra.local

import core.domain.model.ConversationContext
import core.domain.model.Message
import core.ports.LocalLlmService
import data.dto.local.LocalCompletionResponse
import data.mappers.LocalMapper
import infra.config.AppConfig
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class LocalLlmServiceImpl(
    private val config: AppConfig
) : LocalLlmService {

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                ignoreUnknownKeys = true
                encodeDefaults = true
            })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 120_000 // Local models can be slow
        }
    }

    override suspend fun generateResponse(context: ConversationContext): Message {
        val requestDto = LocalMapper.toRequest(context, config.local.modelName)
        
        try {
            val response: LocalCompletionResponse = client.post("${config.local.baseUrl}/generate") {
                contentType(ContentType.Application.Json)
                setBody(requestDto)
            }.body()

            return LocalMapper.toMessage(response)
        } catch (e: Exception) {
            throw RuntimeException("Local LLM API Error: ${e.message}", e)
        }
    }

    override suspend fun isAvailable(): Boolean {
        return try {
            // Simple ping check logic could be here, but for now we trust config
            true 
        } catch (e: Exception) {
            false
        }
    }
}
