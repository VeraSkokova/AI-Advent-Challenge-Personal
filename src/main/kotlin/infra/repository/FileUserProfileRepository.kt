package infra.repository

import core.domain.model.UserProfile
import core.domain.model.UserPreferences
import core.domain.model.TechnicalContext
import core.domain.model.CommunicationStyle
import core.domain.model.Stack
import core.ports.UserProfileRepository
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import java.io.File
import org.slf4j.LoggerFactory

// DTOs matching JSON structure exactly
@kotlinx.serialization.Serializable
data class ConfigDto(
    val version: String,
    val user_profile: ProfileDto,
    val technical_context: TechDto,
    val communication_style: StyleDto
)

@kotlinx.serialization.Serializable
data class ProfileDto(
    val name: String,
    val location: String,
    val timezone: String,
    val language: String,
    val profession: String,
    val level: String,
    val role: String,
    val interests: List<String>
)

@kotlinx.serialization.Serializable
data class TechDto(
    val primary_languages: List<String>,
    val stack: StackDto,
    val preferred_libraries: List<String>,
    val hardware: String
)

@kotlinx.serialization.Serializable
data class StackDto(
    val android: List<String>,
    val backend: List<String>,
    val ai_ml: List<String>,
    val tools: List<String>
)

@kotlinx.serialization.Serializable
data class StyleDto(
    val language: String,
    val tone: String,
    val expectations: String,
    val clarifying_questions_required: Boolean
)

class FileUserProfileRepository(
    private val configFileName: String = "user_config.json"
) : UserProfileRepository {

    private val logger = LoggerFactory.getLogger(FileUserProfileRepository::class.java)
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    override suspend fun loadProfile(): UserPreferences? {
        // Try to load from resources first (Packaged in JAR)
        val resourceStream = this::class.java.classLoader.getResourceAsStream(configFileName)
        val configContent = if (resourceStream != null) {
            logger.info("✅ Loading user config from resources: $configFileName")
            resourceStream.bufferedReader().use { it.readText() }
        } else {
            // Fallback to local file system (Root dir)
            val file = File(configFileName)
            if (file.exists()) {
                logger.info("⚠️ Loading user config from local file system: ${file.absolutePath}")
                file.readText()
            } else {
                logger.error("❌ User config not found in resources or file system: $configFileName")
                return null
            }
        }

        return try {
            val dto = json.decodeFromString<ConfigDto>(configContent)
            mapDtoToDomain(dto)
        } catch (e: Exception) {
            logger.error("❌ Failed to parse user config", e)
            null
        }
    }

    override suspend fun saveProfile(profile: UserPreferences) {
        // Since we load primarily from resources (read-only in JAR), saving back to resources is tricky/impossible at runtime.
        // We will save to the local file system override instead.
        try {
            val dto = mapDomainToDto(profile)
            val content = json.encodeToString(dto)
            val file = File(configFileName)
            file.writeText(content)
            logger.info("💾 Saved updated profile to ${file.absolutePath}")
        } catch (e: Exception) {
            logger.error("❌ Failed to save profile", e)
        }
    }

    private fun mapDtoToDomain(dto: ConfigDto): UserPreferences {
        return UserPreferences(
            version = dto.version,
            userProfile = UserProfile(
                name = dto.user_profile.name,
                location = dto.user_profile.location,
                timezone = dto.user_profile.timezone,
                language = dto.user_profile.language,
                profession = dto.user_profile.profession,
                level = dto.user_profile.level,
                role = dto.user_profile.role,
                interests = dto.user_profile.interests
            ),
            technicalContext = TechnicalContext(
                primaryLanguages = dto.technical_context.primary_languages,
                stack = Stack(
                    android = dto.technical_context.stack.android,
                    backend = dto.technical_context.stack.backend,
                    aiMl = dto.technical_context.stack.ai_ml,
                    tools = dto.technical_context.stack.tools
                ),
                preferredLibraries = dto.technical_context.preferred_libraries,
                hardware = dto.technical_context.hardware
            ),
            communicationStyle = CommunicationStyle(
                language = dto.communication_style.language,
                tone = dto.communication_style.tone,
                expectations = dto.communication_style.expectations,
                clarifyingQuestionsRequired = dto.communication_style.clarifying_questions_required
            )
        )
    }

    private fun mapDomainToDto(domain: UserPreferences): ConfigDto {
        return ConfigDto(
            version = domain.version,
            user_profile = ProfileDto(
                name = domain.userProfile.name,
                location = domain.userProfile.location,
                timezone = domain.userProfile.timezone,
                language = domain.userProfile.language,
                profession = domain.userProfile.profession,
                level = domain.userProfile.level,
                role = domain.userProfile.role,
                interests = domain.userProfile.interests
            ),
            technical_context = TechDto(
                primary_languages = domain.technicalContext.primaryLanguages,
                stack = StackDto(
                    android = domain.technicalContext.stack.android,
                    backend = domain.technicalContext.stack.backend,
                    ai_ml = domain.technicalContext.stack.aiMl,
                    tools = domain.technicalContext.stack.tools
                ),
                preferred_libraries = domain.technicalContext.preferredLibraries,
                hardware = domain.technicalContext.hardware
            ),
            communication_style = StyleDto(
                language = domain.communicationStyle.language,
                tone = domain.communicationStyle.tone,
                expectations = domain.communicationStyle.expectations,
                clarifying_questions_required = domain.communicationStyle.clarifyingQuestionsRequired
            )
        )
    }
}
