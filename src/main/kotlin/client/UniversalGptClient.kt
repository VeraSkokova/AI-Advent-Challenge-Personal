package client

import config.ApiConfig
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.HttpResponse
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.*
import model.*

class UniversalGptClient(
    private val config: ApiConfig
) {

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                ignoreUnknownKeys = true
                encodeDefaults = true
            })
        }
        install(Logging) {
            level = LogLevel.NONE
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 120_000
            connectTimeoutMillis = 30_000
            socketTimeoutMillis = 120_000
        }
    }

    suspend fun sendMessage(
        messages: List<Message>,
        systemPrompt: String,
        model: ModelConfig = ModelsRepository.YandexPro,
        maxTokens: Int = 2000,
        temperature: Double = 0.6
    ): String = runCatching {

        val folderId = config.folderId
        val modelUri = model.uriTemplate.format(folderId)

        val systemMessage = Message("system", systemPrompt)
        val fullHistory = listOf(systemMessage) + messages

        if (model.clientType == ClientType.YANDEX_NATIVE) {
            // === YANDEX NATIVE LOGIC ===
            val requestBody = buildJsonObject {
                put("modelUri", modelUri)
                putJsonObject("completionOptions") {
                    put("stream", false)
                    put("temperature", temperature)
                    put("maxTokens", maxTokens.toString())
                }
                if (model.supportsJsonMode) {
                    put("jsonObject", true) // Ensure JSON output if supported
                }
                putJsonArray("messages") {
                    fullHistory.forEach { msg ->
                        addJsonObject {
                            put("role", msg.role)
                            put("text", msg.text)
                        }
                    }
                }
            }

            val response: HttpResponse = client.post("https://llm.api.cloud.yandex.net/foundationModels/v1/completion") {
                header("Authorization", "Api-Key ${config.apiKey}")
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }

            val json = response.body<JsonObject>()

            // Handle Error
            if (response.status != HttpStatusCode.OK) {
                throw Exception("API Error: ${response.status} $json")
            }

            val result = json["result"]?.jsonObject
            val text = result?.get("alternatives")?.jsonArray?.get(0)?.jsonObject
                ?.get("message")?.jsonObject?.get("text")?.jsonPrimitive?.content ?: ""
                
            return@runCatching text

        } else {
             // Fallback or other models (omitted for Yandex task)
             throw UnsupportedOperationException("Only Yandex Native supported in this simplified version")
        }
    }.getOrElse { e ->
        e.printStackTrace()
        "Error: ${e.message}"
    }

    fun close() {
        client.close()
    }
}
