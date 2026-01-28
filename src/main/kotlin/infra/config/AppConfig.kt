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
        } else {
            println("ℹ️ local.properties not found")
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
        
        if (!localPropertiesFile.exists() && !configFile.exists()) {
            println("ℹ️ No property files found, using Environment Variables only")
        }
    }

    val yandex = YandexConfig(
        // Priority: Environment Variable > Properties File
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
        println("Yandex API Key present: ${yandex.apiKey.isNotBlank()}")
        val maskedFolderId = if (yandex.folderId.isNotBlank()) {
            if (yandex.folderId.length > 4) "${yandex.folderId.take(4)}****" else "****"
        } else "<not set>"
        println("Yandex Folder ID: $maskedFolderId")
        println("Yandex Model URI: ${yandex.modelUri.replace(yandex.folderId, maskedFolderId)}")
        println("Local Model: ${local.modelName} at ${local.baseUrl}")
        println("----------------------------")
    }
}

data class YandexConfig(val folderId: String, val apiKey: String, val modelUri: String)
data class LocalConfig(val baseUrl: String, val modelName: String)
data class RagConfig(val baseUrl: String)
data class McpConfig(val enabled: Boolean)
