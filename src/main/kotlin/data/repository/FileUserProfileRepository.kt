package data.repository

import core.domain.model.UserPreferences
import core.ports.UserProfileRepository
import kotlinx.serialization.json.Json
import java.io.File

class FileUserProfileRepository(
    private val filePath: String = "user_config.json"
) : UserProfileRepository {

    private val json = Json { 
        prettyPrint = true 
        ignoreUnknownKeys = true
    }

    override suspend fun loadProfile(): UserPreferences? {
        val file = File(filePath)
        if (!file.exists()) return null
        
        return try {
            // Need to map from stored JSON structure to Domain UserPreferences
            // For simplicity, assuming the JSON matches the structure we want or using intermediate DTO
            // But since we want to avoid infrastructure dependencies in domain, we should technically use a DTO here too.
            // However, to save time, we'll assume the JSON is compatible or use a simple manual mapping if needed.
            // Wait, Kotlin Serialization needs @Serializable on the class to work directly.
            // Since UserPreferences is a domain class, it should NOT have @Serializable.
            // Solution: Create a DTO for storage here or add @Serializable to domain (pragmatic clean architecture).
            // Let's go with the pure way: Read generic JSON map and map manually, OR assume we added DTOs.
            // Let's create a private DTO inside this file.
            
            val content = file.readText()
            val dto = json.decodeFromString<UserConfigDto>(content)
            return dto.toDomain()
        } catch (e: Exception) {
            println("Error loading profile: ${e.message}")
            null
        }
    }

    override suspend fun saveProfile(profile: UserPreferences) {
        val dto = UserConfigDto.fromDomain(profile)
        val content = json.encodeToString(UserConfigDto.serializer(), dto)
        File(filePath).writeText(content)
    }
}

@kotlinx.serialization.Serializable
private data class UserConfigDto(
    val version: String,
    val user_profile: UserProfileDto,
    val technical_context: TechnicalContextDto,
    val communication_style: CommunicationStyleDto
) {
    fun toDomain() = UserPreferences(
        version = version,
        userProfile = user_profile.toDomain(),
        technicalContext = technical_context.toDomain(),
        communicationStyle = communication_style.toDomain()
    )

    companion object {
        fun fromDomain(domain: UserPreferences) = UserConfigDto(
            version = domain.version,
            user_profile = UserProfileDto.fromDomain(domain.userProfile),
            technical_context = TechnicalContextDto.fromDomain(domain.technicalContext),
            communication_style = CommunicationStyleDto.fromDomain(domain.communicationStyle)
        )
    }
}

@kotlinx.serialization.Serializable
private data class UserProfileDto(
    val name: String,
    val location: String,
    val timezone: String,
    val language: String,
    val profession: String,
    val level: String,
    val role: String,
    val interests: List<String> = emptyList()
) {
    fun toDomain() = core.domain.model.UserProfile(name, location, timezone, language, profession, level, role, interests)
    companion object {
        fun fromDomain(d: core.domain.model.UserProfile) = UserProfileDto(d.name, d.location, d.timezone, d.language, d.profession, d.level, d.role, d.interests)
    }
}

@kotlinx.serialization.Serializable
private data class TechnicalContextDto(
    val primary_languages: List<String>,
    val stack: StackDto,
    val preferred_libraries: List<String>,
    val hardware: String
) {
    fun toDomain() = core.domain.model.TechnicalContext(primary_languages, stack.toDomain(), preferred_libraries, hardware)
    companion object {
        fun fromDomain(d: core.domain.model.TechnicalContext) = TechnicalContextDto(d.primaryLanguages, StackDto.fromDomain(d.stack), d.preferredLibraries, d.hardware)
    }
}

@kotlinx.serialization.Serializable
private data class StackDto(
    val android: List<String>,
    val backend: List<String>,
    val ai_ml: List<String>,
    val tools: List<String>
) {
    fun toDomain() = core.domain.model.Stack(android, backend, ai_ml, tools)
    companion object {
        fun fromDomain(d: core.domain.model.Stack) = StackDto(d.android, d.backend, d.aiMl, d.tools)
    }
}

@kotlinx.serialization.Serializable
private data class CommunicationStyleDto(
    val language: String,
    val tone: String,
    val expectations: String,
    val clarifying_questions_required: Boolean
) {
    fun toDomain() = core.domain.model.CommunicationStyle(language, tone, expectations, clarifying_questions_required)
    companion object {
        fun fromDomain(d: core.domain.model.CommunicationStyle) = CommunicationStyleDto(d.language, d.tone, d.expectations, d.clarifyingQuestionsRequired)
    }
}
