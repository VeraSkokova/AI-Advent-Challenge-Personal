package infra.clients

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

@Serializable
data class GitHubIssue(val number: Int, val title: String, val html_url: String, val labels: List<GitHubLabel>)

@Serializable
data class GitHubLabel(val name: String)

class GitHubClient(private val token: String) {
    private val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }
    
    suspend fun listIssues(owner: String, repo: String): List<GitHubIssue> {
        return try {
            httpClient.get("https://api.github.com/repos/$owner/$repo/issues") {
                header("Authorization", "Bearer $token")
                header("Accept", "application/vnd.github.v3+json")
                parameter("state", "open")
            }.body()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    suspend fun listPullRequests(owner: String, repo: String): String {
        return try {
            httpClient.get("https://api.github.com/repos/$owner/$repo/pulls") {
                header("Authorization", "Bearer $token")
                header("Accept", "application/vnd.github.v3+json")
                parameter("state", "open")
            }.bodyAsText()
        } catch (e: Exception) {
            "[]"
        }
    }
    
    suspend fun createIssue(owner: String, repo: String, title: String, description: String, labels: List<String>): String {
        return try {
            val response = httpClient.post("https://api.github.com/repos/$owner/$repo/issues") {
                header("Authorization", "Bearer $token")
                header("Accept", "application/vnd.github.v3+json")
                contentType(ContentType.Application.Json)
                setBody(mapOf(
                    "title" to title,
                    "body" to description,
                    "labels" to labels
                ))
            }
            if (response.status.isSuccess()) {
                val issue = response.body<GitHubIssue>()
                "✓ Created issue #${issue.number}: ${issue.html_url}"
            } else {
                "Error: ${response.status}"
            }
        } catch (e: Exception) {
            "Error creating issue: ${e.message}"
        }
    }

    suspend fun getPullRequestDiff(owner: String, repo: String, prNumber: Int): String {
        return try {
             val response = httpClient.get("https://api.github.com/repos/$owner/$repo/pulls/$prNumber") {
                header("Authorization", "Bearer $token")
                header("Accept", "application/vnd.github.v3.diff")
            }
            if (response.status.isSuccess()) {
                response.bodyAsText()
            } else {
                "Error getting PR diff: ${response.status}"
            }
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }

    // Stub for triggerWorkflow used in Deploy server, simplified to not need full implementation
    suspend fun triggerWorkflow(owner: String, repo: String, workflowId: String, branch: String): Boolean {
         return try {
            val response = httpClient.post("https://api.github.com/repos/$owner/$repo/actions/workflows/$workflowId/dispatches") {
                header("Authorization", "Bearer $token")
                header("Accept", "application/vnd.github.v3+json")
                contentType(ContentType.Application.Json)
                setBody(mapOf("ref" to branch))
            }
            response.status.isSuccess()
        } catch (e: Exception) {
            false
        }
    }

    // Stubs for getLatestRun, listArtifacts, downloadArtifact - assuming simpler deploy for now or just log
    suspend fun getLatestRun(owner: String, repo: String, workflowId: String, branch: String): RunInfo? = null
    suspend fun listArtifacts(owner: String, repo: String, runId: Long): List<Artifact> = emptyList()
    suspend fun downloadArtifact(url: String, dest: java.io.File) {}
}

data class RunInfo(val id: Long, val status: String, val conclusion: String?)
data class Artifact(val name: String, val archive_download_url: String)
