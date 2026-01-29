package core.domain.model

data class RagRetrievalResult(
    val maxScore: Double,
    val scoreGap: Double?,
    val contextText: String
)
