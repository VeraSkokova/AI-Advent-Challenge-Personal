package infra.mcp.tools

import core.ports.McpServerAdapter
import core.ports.ToolInfo
import infra.clients.CoinCapClient
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

class CryptoMcpServer(
    private val client: CoinCapClient
) : McpServerAdapter {

    override fun getTools(): List<ToolInfo> {
        return listOf(
            ToolInfo("check_crypto_rates", "Get crypto rates", listOf("coins"))
        )
    }

    override suspend fun execute(toolName: String, params: Map<String, String>): String {
        return when (toolName) {
            "check_crypto_rates" -> {
                // Params logic is tricky because originally it expected JSON array
                // For simplicity, let's assume comma-separated string in "coins"
                val coinsRaw = params["coins"] ?: return "Coins list required"
                val coins = if (coinsRaw.startsWith("[")) {
                     try {
                         Json.parseToJsonElement(coinsRaw).jsonArray.map { it.jsonPrimitive.content }
                     } catch (e: Exception) { coinsRaw.split(",").map { it.trim() } }
                } else {
                    coinsRaw.split(",").map { it.trim() }
                }
                
                val rates = client.getRates(coins)
                if (rates.isEmpty()) "No rates found" else rates.entries.joinToString("\n") { "${it.key}: $${it.value}" }
            }
            else -> "Unknown tool: $toolName"
        }
    }
}
