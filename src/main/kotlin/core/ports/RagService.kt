package core.ports
import core.domain.model.Message
import core.domain.model.RagRetrievalResult

interface RagService {
    suspend fun retrieve(query: String): RagRetrievalResult?
    suspend fun searchAndAnswer(query: String): Message
    suspend fun isRelevant(query: String): Boolean
}
