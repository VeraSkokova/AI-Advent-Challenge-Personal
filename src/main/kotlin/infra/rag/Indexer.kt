package infra.rag

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

class Indexer(
    private val embeddingClient: YandexEmbeddingClient
) {
    private val json = Json { prettyPrint = true }

    suspend fun indexDirectory(path: String): Int {
        val dir = File(path)
        if (!dir.exists() || !dir.isDirectory) {
            println("❌ Directory not found: $path")
            return 0
        }

        val chunks = mutableListOf<Chunk>()
        val files = dir.walk().filter { it.isFile && (it.extension == "md" || it.extension == "txt") }

        println("📂 Found ${files.count()} files to index...")

        files.forEach { file ->
            println("  📄 Processing ${file.name}...")
            val content = file.readText()
            // Simple split by double newline (paragraphs)
            val parts = content.split("\n\n").filter { it.isNotBlank() }
            
            parts.forEach { part ->
                val embedding = embeddingClient.getEmbedding(part)
                if (embedding.isNotEmpty()) {
                    chunks.add(Chunk(
                        id = UUID.randomUUID().toString(),
                        documentId = file.name,
                        content = part.trim(),
                        embedding = embedding
                    ))
                }
                // Rate limit handling (naive delay)
                kotlinx.coroutines.delay(500)
            }
        }

        val indexFile = File("index.json")
        indexFile.writeText(json.encodeToString(chunks))
        println("💾 Index saved to ${indexFile.absolutePath} with ${chunks.size} chunks.")
        
        return chunks.size
    }
}
