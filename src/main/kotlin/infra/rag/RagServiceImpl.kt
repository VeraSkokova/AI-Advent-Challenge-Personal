package infra.rag

import core.domain.model.Message
import core.domain.model.Role
import core.ports.RagService
import data.dto.rag.RagSearchRequest
import data.dto.rag.RagSearchResponse
import data.mappers.RagMapper
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

class RagServiceImpl(
    private val config: AppConfig
) : RagService {

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                ignoreUnknownKeys = true
                encodeDefaults = true
            })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
        }
    }

    override suspend fun searchAndAnswer(query: String): Message {
        val requestDto = RagSearchRequest(query = query)
        
        try {
            // Assuming the RAG service has a specific search endpoint
            // This URL structure is based on typical RAG setups, might need adjustment based on actual RAG repo code
            val response: RagSearchResponse = client.post("${config.rag.baseUrl}/api/search") {
                contentType(ContentType.Application.Json)
                setBody(requestDto)
            }.body()

            val contextString = RagMapper.toContextString(response)
            
            return Message(
                role = Role.SYSTEM,
                content = "Found relevant documentation:\n$contextString"
            )
        } catch (e: Exception) {
            // Fallback gracefully if RAG service is down or returns error
            return Message(Role.SYSTEM, "Failed to retrieve documentation: ${e.message}")
        }
    }

    override suspend fun isRelevant(query: String): Boolean {
        // Simple keyword heuristic for now, can be improved with a classifier
        val keywords = listOf("documentation", "docs", "how to", "api", "reference", "guide")
        return keywords.any { query.contains(it, ignoreCase = true) }
    }
}
