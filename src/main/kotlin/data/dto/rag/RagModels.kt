package data.dto.rag

import kotlinx.serialization.Serializable

@Serializable
data class RagSearchRequest(
    val query: String,
    val limit: Int = 3
)

@Serializable
data class RagSearchResponse(
    val results: List<RagSearchResult>
)

@Serializable
data class RagSearchResult(
    val content: String,
    val source: String,
    val score: Double
)
