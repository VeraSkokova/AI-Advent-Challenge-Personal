package model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class UserConfig(
    val version: String,
    @SerialName("user_profile") val userProfile: UserProfile,
    @SerialName("technical_context") val technicalContext: TechnicalContext,
    @SerialName("communication_style") val communicationStyle: CommunicationStyle
)

@Serializable
data class UserProfile(
    val name: String,
    val location: String,
    val timezone: String,
    val language: String,
    val profession: String,
    val level: String,
    val role: String,
    val interests: List<String> = emptyList() // Added interests field
)

@Serializable
data class TechnicalContext(
    @SerialName("primary_languages") val primaryLanguages: List<String>,
    val stack: Stack,
    @SerialName("preferred_libraries") val preferredLibraries: List<String>,
    val hardware: String
)

@Serializable
data class Stack(
    val android: List<String>,
    val backend: List<String>,
    @SerialName("ai_ml") val aiMl: List<String>,
    val tools: List<String>
)

@Serializable
data class CommunicationStyle(
    val language: String,
    val tone: String,
    val expectations: String,
    @SerialName("clarifying_questions_required") val clarifyingQuestionsRequired: Boolean
)
