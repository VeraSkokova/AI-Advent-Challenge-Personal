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
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class YandexEmbeddingClient(
    private val config: AppConfig
) {
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true; encodeDefaults = true })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 60_000
        }
    }

    suspend fun getEmbedding(text: String): List<Double> {
        val folderId = config.yandex.folderId
        if (folderId.isBlank()) {
            println("❌ Error: YANDEX_FOLDER_ID is missing in config.")
            return emptyList()
        }
        
        val uri = "emb://${folderId}/text-search-doc/latest"
        
        val request = YandexEmbeddingRequest(
            modelUri = uri,
            text = text
        )

        try {
            val response = client.post("https://llm.api.cloud.yandex.net/foundationModels/v1/textEmbedding") {
                header("Authorization", "Api-Key ${config.yandex.apiKey}")
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            if (response.status != HttpStatusCode.OK) {
                val errorBody = response.bodyAsText()
                println("❌ Embedding API Error (${response.status}): $errorBody")
                println("   URI used: $uri")
                return emptyList()
            }

            val body: YandexEmbeddingResponse = response.body()
            return body.embedding
        } catch (e: Exception) {
            println("❌ Embedding Exception: ${e.message}")
            e.printStackTrace()
            return emptyList()
        }
    }
}
