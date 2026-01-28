package core.usecase

import core.domain.model.UserPreferences

object PromptFactory {
    fun createSystemPrompt(preferences: UserPreferences?, capabilities: List<String> = emptyList()): String {
        val basePrompt = if (preferences != null) {
            val profile = preferences.userProfile
            val style = preferences.communicationStyle
            val tech = preferences.technicalContext

            """
            You are a Personal AI Assistant for ${profile.name}.
            
            USER PROFILE:
            - Role: ${profile.role} (${profile.level})
            - Profession: ${profile.profession}
            - Location: ${profile.location} (Timezone: ${profile.timezone})
            - Languages: ${profile.language}
            - Interests: ${profile.interests.joinToString(", ")}
            
            TECHNICAL CONTEXT:
            - Primary Languages: ${tech.primaryLanguages.joinToString(", ")}
            - Preferred Libraries: ${tech.preferredLibraries.joinToString(", ")}
            - Hardware: ${tech.hardware}
            - Stack: Android (${tech.stack.android.joinToString()}), Backend (${tech.stack.backend.joinToString()})
            
            COMMUNICATION STYLE:
            - Language: ${style.language}
            - Tone: ${style.tone}
            - Expectations: ${style.expectations}
            - Clarifying Questions: ${if (style.clarifyingQuestionsRequired) "Required when ambiguous" else "Only if critical"}
            """.trimIndent()
        } else {
            "You are a helpful AI assistant."
        }

        val capabilitiesSection = if (capabilities.isNotEmpty()) {
            """
            
            SYSTEM CAPABILITIES:
            ${capabilities.joinToString("\n") { "- $it" }}
            
            NOTE: You are part of an orchestrated system. If the user asks for these capabilities, explain that you have access to them via specific commands or natural language, but you (the LLM) focus on generating the final answer.
            """.trimIndent()
        } else ""

        return """
            $basePrompt
            $capabilitiesSection
            
            INSTRUCTIONS:
            - Always adapt your answers to the user's expertise level and stack.
            - Use the preferred language for code examples.
            - Be concise and helpful.
        """.trimIndent()
    }
}
