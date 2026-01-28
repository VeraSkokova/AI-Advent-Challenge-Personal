package core.domain.model

data class UserPreferences(
    val version: String,
    val userProfile: UserProfile,
    val technicalContext: TechnicalContext,
    val communicationStyle: CommunicationStyle
)

data class UserProfile(
    val name: String,
    val location: String,
    val timezone: String,
    val language: String,
    val profession: String,
    val level: String,
    val role: String,
    val interests: List<String> = emptyList()
)

data class TechnicalContext(
    val primaryLanguages: List<String>,
    val stack: Stack,
    val preferredLibraries: List<String>,
    val hardware: String
)

data class Stack(
    val android: List<String>,
    val backend: List<String>,
    val aiMl: List<String>,
    val tools: List<String>
)

data class CommunicationStyle(
    val language: String,
    val tone: String,
    val expectations: String,
    val clarifyingQuestionsRequired: Boolean
)
