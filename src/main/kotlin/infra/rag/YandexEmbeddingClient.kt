package infra.rag

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

class YandexEmbeddingClient(
    private val config: AppConfig
) {
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 60_000
        }
    }

    suspend fun getEmbedding(text: String): List<Double> {
        val folderId = config.yandex.folderId
        val uri = "emb://${folderId}/text-search-doc/latest"
        
        val request = YandexEmbeddingRequest(
            modelUri = uri,
            text = text
        )

        try {
            val response: YandexEmbeddingResponse = client.post("https://llm.api.cloud.yandex.net/foundationModels/v1/textEmbedding") {
                header("Authorization", "Api-Key ${config.yandex.apiKey}")
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body()

            return response.embedding
        } catch (e: Exception) {
            println("❌ Embedding Error: ${e.message}")
            return emptyList()
        }
    }
}
