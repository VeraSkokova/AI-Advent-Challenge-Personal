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

class YandexEmbeddingClient(private val config: AppConfig) {

    enum class EmbeddingType { DOC, QUERY }

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true; encodeDefaults = true }) }
        install(HttpTimeout) { requestTimeoutMillis = 60_000 }
    }

    suspend fun getDocEmbedding(text: String): List<Double> =
        getEmbedding(text, EmbeddingType.DOC)

    suspend fun getQueryEmbedding(text: String): List<Double> =
        getEmbedding(text, EmbeddingType.QUERY)

    private suspend fun getEmbedding(text: String, type: EmbeddingType): List<Double> {
        val folderId = config.yandex.folderId
        if (folderId.isBlank()) return emptyList()

        val uri = when (type) {
            EmbeddingType.DOC -> "emb://${folderId}/text-search-doc/latest"
            EmbeddingType.QUERY -> "emb://${folderId}/text-search-query/latest"
        }

        val request = YandexEmbeddingRequest(modelUri = uri, text = text)

        val response = client.post("https://llm.api.cloud.yandex.net/foundationModels/v1/textEmbedding") {
            header("Authorization", "Api-Key ${config.yandex.apiKey}")
            contentType(ContentType.Application.Json)
            setBody(request)
        }

        if (response.status != HttpStatusCode.OK) {
            println("❌ Embedding API Error (${response.status}): ${response.bodyAsText()}")
            return emptyList()
        }

        return response.body<YandexEmbeddingResponse>().embedding
    }
}
