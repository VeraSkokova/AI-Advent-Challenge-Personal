package infra.mcp.tools

import core.ports.McpServerAdapter
import core.ports.ToolInfo
import infra.utils.GitClient
import java.io.File

class LocalMcpServer(
    private val rootDir: File = File("."),
    private val gitClient: GitClient = GitClient(rootDir)
) : McpServerAdapter {

    override fun getTools(): List<ToolInfo> {
        return listOf(
            ToolInfo("git_log", "Get recent git commit history", listOf("limit")),
            ToolInfo("git_status", "Get git status", listOf()),
            ToolInfo("git_diff", "Get git diff", listOf()),
            ToolInfo("list_files", "List files in directory", listOf("path")),
            ToolInfo("read_file", "Read file content", listOf("path"))
        )
    }

    override suspend fun execute(toolName: String, params: Map<String, String>): String {
        return when (toolName) {
            "git_log" -> gitClient.runCommand("log", "-n", params["limit"] ?: "10", "--oneline").output
            "git_status" -> gitClient.status()
            "git_diff" -> gitClient.getDiffContent()
            "list_files" -> {
                val path = params["path"] ?: "."
                val file = File(rootDir, path)
                if (file.exists()) file.listFiles()?.joinToString { it.name } ?: "Empty" else "Path not found"
            }
            "read_file" -> {
                val path = params["path"] ?: return "Error: path required"
                val file = File(rootDir, path)
                if (file.exists() && file.isFile) file.readText().take(5000) else "File not found"
            }
            else -> "Unknown tool: $toolName"
        }
    }
    
    // Quick helper to reuse GitClient wrapper logic if needed, but GitClient is cleaner
    private fun GitClient.runCommand(vararg args: String) = 
        infra.utils.GitClient(rootDir).let { 
            // Reflection hack or just instantiate new process builder here? 
            // Since GitClient exposes methods, better use them.
            // But GitClient methods are high level. 
            // Let's implement minimal command runner here or expand GitClient.
            // For now, assume GitClient has everything or implement specific calls.
            // I'll stick to GitClient public API where possible.
            // But git_log is not in GitClient public API fully customized.
            // Let's rely on ProcessBuilder locally for custom ones.
            execute(listOf("git") + args)
        }

    private fun execute(command: List<String>): CommandResult {
        return try {
            val process = ProcessBuilder(command).directory(rootDir).start()
            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()
            CommandResult(process.exitValue() == 0, output)
        } catch(e: Exception) { CommandResult(false, e.message ?: "") }
    }
    
    data class CommandResult(val success: Boolean, val output: String)
}
