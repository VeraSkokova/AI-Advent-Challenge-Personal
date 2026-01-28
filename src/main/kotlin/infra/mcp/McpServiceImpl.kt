package infra.mcp

import core.domain.model.Message
import core.domain.model.Role
import core.ports.McpServerAdapter
import core.ports.McpService
import core.ports.ToolInfo
import infra.clients.CoinCapClient
import infra.clients.GitHubClient
import infra.config.AppConfig
import infra.mcp.tools.AndroidMcpServer
import infra.mcp.tools.CryptoMcpServer
import infra.mcp.tools.GitHubMcpServer
import infra.mcp.tools.LocalMcpServer
import infra.mcp.tools.ReminderMcpServer
import infra.utils.AdbManager
import infra.utils.GitClient
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

class McpServiceImpl(
    private val config: AppConfig
) : McpService {

    private val adapters = mutableListOf<McpServerAdapter>()
    private val toolRegistry = mutableMapOf<String, McpServerAdapter>()

    init {
        // Initialize Adapters
        adapters.add(LocalMcpServer())
        adapters.add(AndroidMcpServer(AdbManager()))
        
        val ghToken = System.getenv("GITHUB_TOKEN") ?: ""
        if (ghToken.isNotBlank()) {
            val gitClient = GitClient()
            val origin = try { gitClient.getRemoteOriginUrl() } catch(e: Exception) { "" }
            
            var owner = System.getenv("GITHUB_OWNER") ?: ""
            var repo = System.getenv("GITHUB_REPO") ?: ""
            
            if (owner.isBlank() || repo.isBlank()) {
                if (origin.contains("github.com")) {
                    val parts = origin.removeSuffix(".git").split("github.com")[1].trim('/', ':').split('/')
                    if (parts.size >= 2) {
                        if (owner.isBlank()) owner = parts[0]
                        if (repo.isBlank()) repo = parts[1]
                    }
                }
            }
            if (owner.isBlank()) owner = "VeraSkokova"
            if (repo.isBlank()) repo = "AI-Advent-Challenge-Personal"

            adapters.add(GitHubMcpServer(GitHubClient(ghToken), owner, repo))
        }

        val coinKey = System.getenv("COINCAP_API_KEY")
        adapters.add(CryptoMcpServer(CoinCapClient(coinKey)))
        
        adapters.add(ReminderMcpServer())

        // Register tools
        adapters.forEach { adapter ->
            adapter.getTools().forEach { tool ->
                toolRegistry[tool.name] = adapter
            }
        }
        
        println("✅ MCP Service Initialized with ${toolRegistry.size} tools")
    }

    override suspend fun executeTool(command: String): Message {
        try {
            val json = Json { ignoreUnknownKeys = true }
            val root = json.parseToJsonElement(command)
            
            if (root is JsonObject) {
                val toolName = root["tool"]?.jsonPrimitive?.content ?: return Message(Role.ASSISTANT, "Error: 'tool' field missing")
                val paramsElement = root["params"]
                
                val params = mutableMapOf<String, String>()
                if (paramsElement is JsonObject) {
                    paramsElement.forEach { (k, v) -> params[k] = v.jsonPrimitive.content }
                }

                val adapter = toolRegistry[toolName]
                if (adapter != null) {
                    val result = adapter.execute(toolName, params)
                    return Message(Role.ASSISTANT, "Tool Output:\n$result")
                } else {
                    return Message(Role.ASSISTANT, "Error: Tool '$toolName' not found")
                }
            } else {
                 return Message(Role.ASSISTANT, "Error: Invalid tool command format (expected JSON)")
            }
        } catch (e: Exception) {
             return Message(Role.ASSISTANT, "Error executing tool: ${e.message}")
        }
    }

    override fun needsTool(query: String): Boolean {
        // Simple heuristic: trigger if user explicitly asks or uses keywords
        // Ideally, LLM decides this via "capabilities" in system prompt.
        // But Router calls this.
        
        // This heuristic is weak. Better strategy:
        // Always let LLM see tools in system prompt (done via capabilities).
        // If LLM wants to use a tool, it outputs specific JSON format.
        // Router should check if LLM's *response* is a tool call? 
        // OR Router checks if *query* implies tool usage.
        
        // Let's stick to Router logic: keyword matching for now
        val keywords = toolRegistry.keys.flatMap { it.split("_") }.toSet() - setOf("get", "list", "read", "check")
        return keywords.any { query.contains(it, ignoreCase = true) }
    }

    override fun getAvailableTools(): List<ToolInfo> {
        return toolRegistry.keys.mapNotNull { name ->
             toolRegistry[name]?.getTools()?.find { it.name == name }
        }
    }
}
