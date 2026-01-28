package infra.config

import java.util.Properties
import java.io.File

object AppConfig {
    private val properties = Properties()
    
    init {
        val configFile = File("config.properties")
        if (configFile.exists()) {
            try {
                properties.load(configFile.inputStream())
                println("✅ Loaded configuration from config.properties")
            } catch (e: Exception) {
                println("⚠️ Failed to load config.properties: ${e.message}")
            }
        } else {
            println("ℹ️ config.properties not found, using Environment Variables")
        }
    }

    val yandex = YandexConfig(
        folderId = System.getenv("YANDEX_FOLDER_ID") ?: properties.getProperty("yandex.folder_id") ?: "",
        apiKey = System.getenv("YANDEX_API_KEY") ?: properties.getProperty("yandex.api_key") ?: "",
        modelUri = "gpt://${System.getenv("YANDEX_FOLDER_ID") ?: properties.getProperty("yandex.folder_id") ?: ""}/yandexgpt/latest"
    )

    val local = LocalConfig(
        baseUrl = System.getenv("LOCAL_LLM_URL") ?: "http://localhost:11434/api",
        // Updated default model to qwen2.5:1.5b as requested
        modelName = System.getenv("LOCAL_LLM_MODEL") ?: "qwen2.5:1.5b"
    )
    
    val rag = RagConfig(
        baseUrl = System.getenv("RAG_SERVICE_URL") ?: "http://localhost:8081"
    )
    
    val mcp = McpConfig(
        enabled = true
    )

    fun printStatus() {
        println("--- Configuration Status ---")
        println("Yandex API Key present: ${yandex.apiKey.isNotBlank()}")
        println("Yandex Folder ID: ${yandex.folderId}")
        println("Local Model: ${local.modelName} at ${local.baseUrl}")
        println("----------------------------")
    }
}

data class YandexConfig(val folderId: String, val apiKey: String, val modelUri: String)
data class LocalConfig(val baseUrl: String, val modelName: String)
data class RagConfig(val baseUrl: String)
data class McpConfig(val enabled: Boolean)
