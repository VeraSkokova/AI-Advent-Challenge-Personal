package core.ports

import core.domain.model.Message

interface RagService {
    suspend fun searchAndAnswer(query: String): Message
    suspend fun isRelevant(query: String): Boolean
}
