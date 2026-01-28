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
        // 1. Local Git/Files
        adapters.add(LocalMcpServer())

        // 2. Android
        adapters.add(AndroidMcpServer(AdbManager()))
        
        // 3. GitHub
        val ghToken = config.getGitHubToken()
        if (ghToken.isNotBlank()) {
            val gitClient = GitClient()
            val origin = try { gitClient.getRemoteOriginUrl() } catch(e: Exception) { "" }
            
            // Try to parse owner/repo from git origin, or fallback to defaults
            var owner = "VeraSkokova" 
            var repo = "AI-Advent-Challenge-Personal"
            
            if (origin.contains("github.com")) {
                val parts = origin.removeSuffix(".git").split("github.com")[1].trim('/', ':').split('/')
                if (parts.size >= 2) {
                    owner = parts[0]
                    repo = parts[1]
                }
            }

            adapters.add(GitHubMcpServer(GitHubClient(ghToken), owner, repo))
        }

        // 4. Crypto
        adapters.add(CryptoMcpServer(CoinCapClient(config.getCoinCapKey())))
        
        // 5. Reminders
        adapters.add(ReminderMcpServer())

        // Register tools
        adapters.forEach { adapter ->
            adapter.getTools().forEach { tool ->
                toolRegistry[tool.name] = adapter
            }
        }
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
                 return Message(Role.ASSISTANT, "Error: Invalid tool command format")
            }
        } catch (e: Exception) {
             return Message(Role.ASSISTANT, "Error executing tool: ${e.message}")
        }
    }

    override fun needsTool(query: String): Boolean {
        // This is now purely for Router heuristic if needed, 
        // but typically LLM decision is handled in Orchestrator via tool protocol
        return false 
    }

    override fun getAvailableTools(): List<ToolInfo> {
        return toolRegistry.keys.mapNotNull { name ->
             toolRegistry[name]?.getTools()?.find { it.name == name }
        }
    }
}
