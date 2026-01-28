package core.domain.model

data class ConversationContext(
    val messages: List<Message>,
    val userPreferences: UserPreferences?,
    val metadata: Map<String, String> = emptyMap()
)
