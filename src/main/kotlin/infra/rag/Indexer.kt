package infra.rag

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class Indexer(
    private val embeddingClient: YandexEmbeddingClient
) {
    private val json = Json { prettyPrint = true }
    private val chunker = TextChunker()

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
            
            // Use robust chunking logic
            val fileChunks = chunker.chunkDocument(file.name, content)
            println("     -> Split into ${fileChunks.size} chunks")

            fileChunks.forEach { chunk ->
                // Generate embedding for each chunk
                val embedding = embeddingClient.getEmbedding(chunk.content)
                if (embedding.isNotEmpty()) {
                    chunks.add(chunk.copy(embedding = embedding))
                } else {
                    println("     ⚠️ Failed to embed chunk: ${chunk.id}")
                }
                
                // Rate limit handling (naive delay)
                kotlinx.coroutines.delay(200) 
            }
        }

        val indexFile = File("index.json")
        indexFile.writeText(json.encodeToString(chunks))
        println("💾 Index saved to ${indexFile.absolutePath} with ${chunks.size} chunks.")
        
        return chunks.size
    }
}
