package core.ports

interface VoiceInputProvider {
    suspend fun recordAndTranscribe(): String
}
