package client

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import model.YandexRequest
import model.YandexResponse
import model.CompletionOptions
import model.Message

class YandexClient(
    private val folderId: String,
    private val iamToken: String
) {
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true
                encodeDefaults = true
            })
        }
    }

    suspend fun generateResponse(
        messages: List<Message>,
        systemPrompt: String? = null,
        temperature: Double = 0.6,
        maxTokens: String = "2000"
    ): String {
        val allMessages = mutableListOf<Message>()
        if (systemPrompt != null) {
            allMessages.add(Message("system", systemPrompt))
        }
        allMessages.addAll(messages)

        val requestBody = YandexRequest(
            modelUri = "gpt://$folderId/yandexgpt",
            completionOptions = CompletionOptions(
                stream = false,
                temperature = temperature,
                maxTokens = maxTokens
            ),
            messages = allMessages
        )

        try {
            val response: YandexResponse = client.post("https://llm.api.cloud.yandex.net/foundationModels/v1/completion") {
                header(HttpHeaders.Authorization, "Bearer $iamToken")
                header("x-folder-id", folderId)
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }.body()

            return response.result.alternatives.firstOrNull()?.message?.text ?: "No response from YandexGPT"
        } catch (e: Exception) {
            e.printStackTrace()
            return "Error communicating with Yandex API: ${e.message}"
        }
    }
    
    fun close() {
        client.close()
    }
}
