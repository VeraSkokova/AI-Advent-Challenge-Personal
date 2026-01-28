package data.dto.local

import kotlinx.serialization.Serializable

@Serializable
data class LocalCompletionRequest(
    val model: String,
    val prompt: String,
    val stream: Boolean = false
)

@Serializable
data class LocalCompletionResponse(
    val response: String,
    val done: Boolean,
    val context: List<Int>? = null
)
