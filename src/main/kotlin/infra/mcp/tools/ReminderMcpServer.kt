package infra.mcp.tools

import core.ports.McpServerAdapter
import core.ports.ToolInfo
import java.io.File
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

@Serializable
data class Reminder(val id: String, val title: String, val command: String, val cron: String)

class ReminderMcpServer(
    private val storageFile: File = File("reminders.json")
) : McpServerAdapter {

    private val json = Json { prettyPrint = true }

    override fun getTools(): List<ToolInfo> {
        return listOf(
            ToolInfo("add_reminder", "Add reminder", listOf("title", "command", "cron")),
            ToolInfo("list_reminders", "List reminders", emptyList()),
            ToolInfo("remove_reminder", "Remove reminder", listOf("id"))
        )
    }

    override suspend fun execute(toolName: String, params: Map<String, String>): String {
        return when (toolName) {
            "add_reminder" -> {
                val title = params["title"] ?: return "Title required"
                val cmd = params["command"] ?: ""
                val cron = params["cron"] ?: ""
                val rem = Reminder(System.currentTimeMillis().toString(), title, cmd, cron)
                save(rem)
                "Reminder added: ${rem.id}"
            }
            "list_reminders" -> loadAll().joinToString("\n") { "[${it.id}] ${it.title} (${it.cron})" }.ifEmpty { "No reminders" }
            "remove_reminder" -> {
                val id = params["id"] ?: return "Id required"
                remove(id)
                "Removed $id"
            }
            else -> "Unknown tool: $toolName"
        }
    }

    private fun loadAll(): List<Reminder> {
        if (!storageFile.exists()) return emptyList()
        return try {
            json.decodeFromString<List<Reminder>>(storageFile.readText())
        } catch (e: Exception) { emptyList() }
    }

    private fun save(reminder: Reminder) {
        val list = loadAll().toMutableList()
        list.add(reminder)
        storageFile.writeText(json.encodeToString(list))
    }

    private fun remove(id: String) {
        val list = loadAll().filter { it.id != id }
        storageFile.writeText(json.encodeToString(list))
    }
}
