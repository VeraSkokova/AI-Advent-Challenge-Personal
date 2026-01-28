package core.domain.model

data class Message(
    val role: Role,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

enum class Role {
    USER, ASSISTANT, SYSTEM
}
