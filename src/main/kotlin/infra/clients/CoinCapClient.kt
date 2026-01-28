package infra.clients

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

@Serializable
data class CoinCapResponse(
    val data: List<CryptoData>
)

@Serializable
data class CryptoData(
    val id: String,
    val symbol: String,
    val priceUsd: String
)

class CoinCapClient(private val apiKey: String? = null) {
    private val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }
    
    suspend fun getRates(coins: List<String>): Map<String, Double> {
        val results = mutableMapOf<String, Double>()
        for (coinId in coins) {
            try {
                val price = getCryptoPrice(coinId)
                if (price != null) results[coinId] = price
            } catch (e: Exception) {
                // Ignore errors for individual coins
            }
        }
        return results
    }
    
    suspend fun getCryptoPrice(assetId: String): Double? {
        return try {
            val response: CoinCapResponse = httpClient.get("https://rest.coincap.io/v3/assets") {
                parameter("search", assetId)
                parameter("limit", 1)
                if (apiKey != null) header("Authorization", "Bearer $apiKey")
            }.body()
            
            val asset = response.data.firstOrNull()
            asset?.priceUsd?.toDoubleOrNull()
        } catch (e: Exception) {
            null
        }
    }
}
