package infra.rag

import java.util.UUID

class TextChunker {
    
    companion object {
        const val MAX_CHUNK_SIZE = 512
        const val CHUNK_OVERLAP = 100
    }

    fun chunkDocument(fileName: String, text: String): List<Chunk> {
        // 1. Split into sentences (using Lookbehind regex to keep punctuation)
        val sentenceRegex = Regex("(?<=[.!?\\n])\\s+")
        val sentences = text.split(sentenceRegex).filter { it.isNotBlank() }

        val chunks = mutableListOf<Chunk>()
        var currentChunkText = StringBuilder()
        
        // Simple sliding window algorithm
        var i = 0
        while (i < sentences.size) {
            val sentence = sentences[i]

            // If adding sentence exceeds limit and chunk is not empty
            if (currentChunkText.length + sentence.length > MAX_CHUNK_SIZE && currentChunkText.isNotEmpty()) {
                // Save current chunk
                chunks.add(
                    Chunk(
                        id = UUID.randomUUID().toString(),
                        documentId = fileName,
                        content = currentChunkText.toString().trim()
                    )
                )

                // Clear builder
                currentChunkText.clear()

                // Backtrack for Overlap
                var overlapSize = 0
                var backTrackIndex = i - 1
                val overlapBuffer = StringBuilder()

                // Collect sentences backwards until ~100 chars
                while (backTrackIndex >= 0 && overlapSize < CHUNK_OVERLAP) {
                    val prevSentence = sentences[backTrackIndex]
                    overlapBuffer.insert(0, "$prevSentence ")
                    overlapSize += prevSentence.length + 1
                    backTrackIndex--
                }

                currentChunkText.append(overlapBuffer)
            }

            currentChunkText.append(sentence).append(" ")
            i++
        }

        // Add tail
        if (currentChunkText.isNotEmpty()) {
            chunks.add(
                Chunk(
                    id = UUID.randomUUID().toString(),
                    documentId = fileName,
                    content = currentChunkText.toString().trim()
                )
            )
        }

        return chunks
    }
}
