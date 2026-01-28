package data.dto.local

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class LocalCompletionRequest(
    val model: String,
    val prompt: String,
    val stream: Boolean = false
)

@Serializable
data class LocalCompletionResponse(
    @SerialName("response") val response: String? = null,
    @SerialName("done") val done: Boolean? = null,
    @SerialName("context") val context: List<Int>? = null,
    // Add Ollama chat specific fields just in case the endpoint is /api/chat
    @SerialName("message") val message: LocalMessage? = null
)

@Serializable
data class LocalMessage(
    val role: String,
    val content: String
)
