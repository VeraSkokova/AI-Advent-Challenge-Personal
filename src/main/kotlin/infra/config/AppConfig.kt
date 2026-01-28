package infra.config

import java.util.Properties
import java.io.File

object AppConfig {
    private val properties = Properties()
    
    init {
        // Try to load local.properties first
        val localPropertiesFile = File("local.properties")
        if (localPropertiesFile.exists()) {
            try {
                properties.load(localPropertiesFile.inputStream())
                println("✅ Loaded configuration from local.properties")
            } catch (e: Exception) {
                println("⚠️ Failed to load local.properties: ${e.message}")
            }
        }
        
        // Also try config.properties as fallback
        val configFile = File("config.properties")
        if (configFile.exists()) {
            try {
                properties.load(configFile.inputStream())
                println("✅ Loaded configuration from config.properties")
            } catch (e: Exception) {
                println("⚠️ Failed to load config.properties: ${e.message}")
            }
        }
    }

    val yandex = YandexConfig(
        folderId = System.getenv("YANDEX_FOLDER_ID") ?: properties.getProperty("YANDEX_FOLDER_ID") ?: "",
        apiKey = System.getenv("YANDEX_API_KEY") ?: properties.getProperty("YANDEX_API_KEY") ?: "",
        modelUri = run {
            val folderId = System.getenv("YANDEX_FOLDER_ID") ?: properties.getProperty("YANDEX_FOLDER_ID") ?: ""
            "gpt://$folderId/yandexgpt/latest"
        }
    )

    val local = LocalConfig(
        baseUrl = System.getenv("LOCAL_LLM_URL") ?: properties.getProperty("LOCAL_LLM_URL") ?: "http://localhost:11434/api",
        modelName = System.getenv("LOCAL_LLM_MODEL") ?: properties.getProperty("LOCAL_LLM_MODEL") ?: "qwen2.5:1.5b"
    )
    
    val rag = RagConfig(
        baseUrl = System.getenv("RAG_SERVICE_URL") ?: properties.getProperty("RAG_SERVICE_URL") ?: "http://localhost:8081"
    )
    
    val mcp = McpConfig(
        enabled = true
    )

    fun printStatus() {
        println("--- Configuration Status ---")
        println("Yandex Service: ${if (yandex.apiKey.isNotBlank() && yandex.folderId.isNotBlank()) "✅ Configured" else "❌ Missing Credentials"}")
        println("Local Service: ${local.modelName} at ${local.baseUrl}")
        println("RAG Service: ${rag.baseUrl}")
        println("----------------------------")
    }
}

data class YandexConfig(val folderId: String, val apiKey: String, val modelUri: String)
data class LocalConfig(val baseUrl: String, val modelName: String)
data class RagConfig(val baseUrl: String)
data class McpConfig(val enabled: Boolean)
