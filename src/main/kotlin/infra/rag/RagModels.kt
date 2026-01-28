package infra.rag

import kotlinx.serialization.Serializable

@Serializable
data class RagDocument(
    val id: String,
    val content: String,
    val metadata: Map<String, String> = emptyMap(),
    val chunks: List<Chunk> = emptyList()
)

@Serializable
data class Chunk(
    val id: String,
    val documentId: String,
    val content: String,
    val embedding: List<Double> = emptyList()
)

@Serializable
data class EmbeddingRequest(
    val modelUri: String,
    val text: String
)

@Serializable
data class EmbeddingResponse(
    val embedding: List<Double>
)

// Yandex Embeddings API Structure
@Serializable
data class YandexEmbeddingRequest(
    val modelUri: String,
    val text: String
)

@Serializable
data class YandexEmbeddingResponse(
    val embedding: List<Double>,
    val numTokens: String
)
