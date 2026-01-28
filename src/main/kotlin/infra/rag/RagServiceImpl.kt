package infra.rag

import core.domain.model.Message
import core.domain.model.Role
import core.ports.RagService
import infra.config.AppConfig
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.math.sqrt

class RagServiceImpl(
    private val config: AppConfig
) : RagService {

    // Helper client for embeddings (reusing the one we built for Indexer)
    private val embeddingClient = YandexEmbeddingClient(config)
    private val json = Json { ignoreUnknownKeys = true }
    
    // We can expose indexer to Main if needed, but RagService focuses on Search
    val indexer = Indexer(embeddingClient)

    override suspend fun searchAndAnswer(query: String): Message {
        // 1. Load Index
        val indexFile = File("index.json")
        if (!indexFile.exists()) {
            return Message(Role.SYSTEM, "Index file not found. Please run '/index <path>' first.")
        }

        val chunks: List<Chunk> = try {
            json.decodeFromString(indexFile.readText())
        } catch (e: Exception) {
            return Message(Role.SYSTEM, "Failed to load index: ${e.message}")
        }

        // 2. Embed Query
        val queryEmbedding = embeddingClient.getEmbedding(query)
        if (queryEmbedding.isEmpty()) {
            return Message(Role.SYSTEM, "Failed to generate embedding for query.")
        }

        // 3. Cosine Similarity Search
        val results = chunks.map { chunk ->
            val score = cosineSimilarity(queryEmbedding, chunk.embedding)
            chunk to score
        }
        .filter { it.second > 0.5 } // Threshold
        .sortedByDescending { it.second }
        .take(3)

        if (results.isEmpty()) {
             return Message(Role.SYSTEM, "No relevant info found in documentation (threshold 0.5).")
        }

        // 4. Construct Context
        val contextBuilder = StringBuilder("Found relevant documentation:\n\n")
        results.forEachIndexed { idx, (chunk, score) ->
            contextBuilder.append("[${idx + 1}] Source: ${chunk.documentId} (Score: ${String.format("%.2f", score)})\n")
            contextBuilder.append("${chunk.content}\n\n")
        }

        return Message(Role.SYSTEM, contextBuilder.toString())
    }

    override suspend fun isRelevant(query: String): Boolean {
        return query.contains("doc") || query.contains("rag") // naive check
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
