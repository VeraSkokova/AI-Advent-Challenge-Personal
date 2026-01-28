package infra.config

import java.util.Properties
import java.io.File

object AppConfig {
    private val properties = Properties()
    
    init {
        val configFile = File("config.properties")
        if (configFile.exists()) {
            properties.load(configFile.inputStream())
        }
    }

    val yandex = YandexConfig(
        folderId = System.getenv("YANDEX_FOLDER_ID") ?: properties.getProperty("yandex.folder_id") ?: "",
        apiKey = System.getenv("YANDEX_API_KEY") ?: properties.getProperty("yandex.api_key") ?: "",
        modelUri = "gpt://${System.getenv("YANDEX_FOLDER_ID") ?: properties.getProperty("yandex.folder_id") ?: ""}/yandexgpt/latest"
    )

    val local = LocalConfig(
        baseUrl = System.getenv("LOCAL_LLM_URL") ?: "http://localhost:11434/api",
        modelName = System.getenv("LOCAL_LLM_MODEL") ?: "llama3.2"
    )
    
    val rag = RagConfig(
        baseUrl = System.getenv("RAG_SERVICE_URL") ?: "http://localhost:8081"
    )
    
    val mcp = McpConfig(
        enabled = true // For now hardcoded
    )
}

data class YandexConfig(val folderId: String, val apiKey: String, val modelUri: String)
data class LocalConfig(val baseUrl: String, val modelName: String)
data class RagConfig(val baseUrl: String)
data class McpConfig(val enabled: Boolean)
