package infra.config

import java.io.File
import java.util.Properties

class AppConfig {
    private val properties = Properties()

    init {
        val localProps = File("local.properties")
        if (localProps.exists()) {
            localProps.inputStream().use { properties.load(it) }
        }
    }

    fun get(key: String): String? {
        return properties.getProperty(key) ?: System.getenv(key)
    }

    fun getGitHubToken(): String = get("GITHUB_TOKEN") ?: ""
    fun getCoinCapKey(): String? = get("COINCAP_API_KEY")

    // Nested configs for compatibility
    val local = LocalConfig(this)
    val rag = RagConfig(this)
    val yandex = YandexConfig(this)

    class LocalConfig(private val config: AppConfig) {
        val baseUrl: String get() = config.get("LOCAL_LLM_URL") ?: "http://localhost:11434/api"
        val modelName: String get() = config.get("LOCAL_LLM_MODEL") ?: "qwen2.5:1.5b"
    }

    class RagConfig(private val config: AppConfig) {
        val baseUrl: String get() = config.get("RAG_SERVICE_URL") ?: "http://localhost:8081"
    }

    class YandexConfig(private val config: AppConfig) {
        val apiKey: String get() = config.get("YANDEX_API_KEY") ?: ""
        val folderId: String get() = config.get("YANDEX_FOLDER_ID") ?: ""
        val modelUri: String get() = "gpt://${folderId}/yandexgpt"
    }
}
