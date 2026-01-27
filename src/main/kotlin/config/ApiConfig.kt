package config

import java.io.File
import java.io.FileInputStream
import java.util.Properties

data class ApiConfig(
    val apiKey: String,
    val folderId: String
) {
    companion object {
        fun load(fileName: String = "local.properties"): ApiConfig {
            val props = Properties()
            val file = File(fileName)

            if (file.exists()) {
                FileInputStream(file).use { props.load(it) }
            }

            // Using YANDEX_API_KEY and YANDEX_FOLDER_ID as per user request
            val apiKey = System.getenv("YANDEX_API_KEY")
                ?: props.getProperty("YANDEX_API_KEY")?.trim()
                ?: props.getProperty("yandex.apiKey")?.trim() // Fallback to property style if needed
                ?: throw IllegalStateException("API Key not found! Set YANDEX_API_KEY env var or in $fileName")

            val folderId = System.getenv("YANDEX_FOLDER_ID")
                ?: props.getProperty("YANDEX_FOLDER_ID")?.trim()
                ?: props.getProperty("yandex.folderId")?.trim()
                ?: throw IllegalStateException("Folder ID not found! Set YANDEX_FOLDER_ID env var or in $fileName")

            return ApiConfig(apiKey, folderId)
        }
    }
}
