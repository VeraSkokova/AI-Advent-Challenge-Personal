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

    private val embeddingClient = YandexEmbeddingClient(config)
    private val json = Json { ignoreUnknownKeys = true }
    
    val indexer = Indexer(embeddingClient)

    override suspend fun searchAndAnswer(query: String): Message {
        // 1. Load Index
        val indexFile = File("index.json")
        if (!indexFile.exists()) {
            return Message(Role.SYSTEM, "⚠️ Index file 'index.json' not found. Run '/index <path>' to ingest documents.")
        }

        val chunks: List<Chunk> = try {
            json.decodeFromString(indexFile.readText())
        } catch (e: Exception) {
            return Message(Role.SYSTEM, "❌ Failed to load index: ${e.message}")
        }

        // 2. Embed Query
        val queryEmbedding = embeddingClient.getEmbedding(query)
        if (queryEmbedding.isEmpty()) {
            return Message(Role.SYSTEM, "❌ Failed to generate embedding for query.")
        }

        // 3. Cosine Similarity Search
        val results = chunks.map { chunk ->
            val score = cosineSimilarity(queryEmbedding, chunk.embedding)
            chunk to score
        }
        .filter { it.second > 0.4 } // Slightly lowered threshold for better recall
        .sortedByDescending { it.second }
        .take(3)

        if (results.isEmpty()) {
             return Message(Role.SYSTEM, "ℹ️ No relevant info found in documentation (threshold 0.4).")
        }

        // 4. Construct Context
        val contextBuilder = StringBuilder("📚 **RAG Context Found:**\n")
        results.forEachIndexed { idx, (chunk, score) ->
            contextBuilder.append("\n**[${idx + 1}] ${chunk.documentId}** (Score: ${String.format("%.2f", score)})\n")
            contextBuilder.append("> ${chunk.content.replace("\n", "\n> ")}\n")
        }

        return Message(Role.SYSTEM, contextBuilder.toString())
    }

    override suspend fun isRelevant(query: String): Boolean {
        val triggers = listOf(
            // Tech/RAG specific
            "rag", "раг", "index", "индекс", "base", "база",
            
            // Docs/Info
            "doc", "док", "manual", "мануал", "guide", "гайд", 
            "tutorial", "туториал", "instruction", "инструкци",
            "reference", "справочник", "api", "апи",
            "example", "пример", "snippet", "сниппет",
            
            // Actions
            "find", "найди", "найти", "search", "поиск", "поищи",
            "how to", "как использовать", "как сделать", "как работает",
            
            // General Info questions (often implies knowledge lookup)
            "what is", "что такое", "explain", "объясни"
        )
        
        val lowerQuery = query.lowercase()
        return triggers.any { lowerQuery.contains(it) }
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
