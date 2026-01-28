package core.domain.model

data class ConversationContext(
    val messages: List<Message>,
    val userPreferences: UserPreferences?,
    val capabilities: List<String> = emptyList()
)
