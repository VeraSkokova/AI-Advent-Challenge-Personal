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
}
