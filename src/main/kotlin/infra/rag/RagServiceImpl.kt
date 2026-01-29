package infra.rag

import core.domain.model.Message
import core.domain.model.RagRetrievalResult
import core.domain.model.Role
import core.ports.RagService
import infra.config.AppConfig
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.math.sqrt

class RagServiceImpl(
    private val config: AppConfig
) : RagService {

    // Helper client for embeddings (ensure it has getDocEmbedding / getQueryEmbedding)
    private val embeddingClient = YandexEmbeddingClient(config)
    private val json = Json { ignoreUnknownKeys = true }

    val indexer = Indexer(embeddingClient)

    /**
     * Main retrieval method for Grey Zone logic.
     * Returns structured result with scores, or null if retrieval failed/no index.
     */
    override suspend fun retrieve(query: String): RagRetrievalResult? {
        // 1. Load Index
        val indexFile = File("index.json")
        if (!indexFile.exists()) {
            return null
        }

        val chunks: List<Chunk> = try {
            json.decodeFromString(indexFile.readText())
        } catch (_: Exception) {
            return null
        }

        // 2. Embed Query (using QUERY embedding model)
        val queryEmbedding = embeddingClient.getQueryEmbedding(query)
        if (queryEmbedding.isEmpty()) {
            return null
        }

        // 3. Cosine Similarity Search
        // Calculate all scores first
        val scoredChunks = chunks.map { chunk ->
            val score = cosineSimilarity(queryEmbedding, chunk.embedding)
            chunk to score
        }
            .sortedByDescending { it.second } // Sort by score DESC
            .take(3) // Take top 3 candidates

        if (scoredChunks.isEmpty()) return null

        // 4. Calculate Scores & Gap
        val maxScore = scoredChunks.first().second

        // Calculate gap between 1st and 2nd result (if 2nd exists)
        val scoreGap = if (scoredChunks.size > 1) {
            scoredChunks[0].second - scoredChunks[1].second
        } else {
            1.0 // If only 1 result, gap is maximum (it's unique)
        }

        // 5. Construct Context Text
        val contextBuilder = StringBuilder("📚 **RAG Context Candidates:**\n")
        scoredChunks.forEachIndexed { idx, (chunk, score) ->
            contextBuilder.append("\n**[${idx + 1}] ${chunk.documentId}** (Score: ${String.format("%.2f", score)})\n")
            contextBuilder.append("> ${chunk.content.replace("\n", "\n> ")}\n")
        }

        return RagRetrievalResult(
            maxScore = maxScore,
            scoreGap = scoreGap,
            contextText = contextBuilder.toString()
        )
    }

    // Legacy method (optional, can be removed if not used elsewhere)
    override suspend fun searchAndAnswer(query: String): Message {
        val result = retrieve(query)
        return if (result != null && result.maxScore > 0.6) {
            Message(Role.SYSTEM, result.contextText)
        } else {
            Message(Role.SYSTEM, "NO_RAG_CONTEXT")
        }
    }

    // Deprecated method (logic moved to Orchestrator)
    override suspend fun isRelevant(query: String): Boolean {
        return true // Always try retrieval now
    }

    private fun cosineSimilarity(v1: List<Double>, v2: List<Double>): Double {
        if (v1.size != v2.size) return 0.0
        var dotProduct = 0.0
        var normA = 0.0
        var normB = 0.0
        for (i in v1.indices) {
            dotProduct += v1[i] * v2[i]
            normA += v1[i] * v1[i]
            normB += v2[i] * v2[i]
        }
        return if (normA > 0 && normB > 0) dotProduct / (sqrt(normA) * sqrt(normB)) else 0.0
    }
}
