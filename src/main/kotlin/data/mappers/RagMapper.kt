package data.mappers

import core.domain.model.Message
import core.domain.model.Role
import data.dto.rag.RagSearchResponse

object RagMapper {
    fun toContextString(response: RagSearchResponse): String {
        return response.results.joinToString(separator = "\n\n") { result ->
            "Source: ${result.source} (Confidence: ${result.score})\nContent: ${result.content}"
        }
    }
}
