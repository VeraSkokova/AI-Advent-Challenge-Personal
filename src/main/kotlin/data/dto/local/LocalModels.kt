package data.dto.local

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LocalChatRequest(
    val model: String,
    val messages: List<LocalMessage>,
    val stream: Boolean = false
)

@Serializable
data class LocalChatResponse(
    val model: String? = null,
    val created_at: String? = null,
    val message: LocalMessage? = null,
    val done: Boolean? = null
)

@Serializable
data class LocalMessage(
    val role: String,
    val content: String
)
