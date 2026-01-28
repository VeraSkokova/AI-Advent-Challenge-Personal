package infra.mcp.tools

import core.ports.McpServerAdapter
import core.ports.ToolInfo
import infra.clients.GitHubClient

class GitHubMcpServer(
    private val client: GitHubClient,
    private val owner: String,
    private val repo: String
) : McpServerAdapter {

    override fun getTools(): List<ToolInfo> {
        return listOf(
            ToolInfo("get_project_status", "List open PRs and issues", emptyList()),
            ToolInfo("create_task", "Create issue", listOf("title", "description", "priority")),
            ToolInfo("deploy_to_localhost", "Trigger CI/CD deploy", listOf("branch"))
        )
    }

    override suspend fun execute(toolName: String, params: Map<String, String>): String {
        return when (toolName) {
            "get_project_status" -> {
                val issues = client.listIssues(owner, repo)
                val prs = client.listPullRequests(owner, repo) // Returns raw JSON for now, need parsing or better client
                "Issues: ${issues.size}, PRs raw: ${prs.take(100)}..." // Simplified
            }
            "create_task" -> {
                val title = params["title"] ?: return "Title required"
                val desc = params["description"] ?: ""
                val priority = params["priority"] ?: "medium"
                client.createIssue(owner, repo, title, desc, listOf("ai-task", "priority:$priority"))
            }
            "deploy_to_localhost" -> {
                val branch = params["branch"] ?: "main"
                if (client.triggerWorkflow(owner, repo, "ci.yml", branch)) "Workflow triggered" else "Failed to trigger"
            }
            else -> "Unknown tool: $toolName"
        }
    }
}
