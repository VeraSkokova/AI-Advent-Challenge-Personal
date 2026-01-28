package infra.mcp.tools

import core.ports.McpServerAdapter
import core.ports.ToolInfo
import infra.utils.AdbManager

class AndroidMcpServer(
    private val adbManager: AdbManager
) : McpServerAdapter {

    override fun getTools(): List<ToolInfo> {
        return listOf(
            ToolInfo("check_adb", "Check ADB status", emptyList()),
            ToolInfo("list_devices", "List connected devices", emptyList()),
            ToolInfo("start_emulator", "Start AVD", listOf("avdName")),
            ToolInfo("install_apk", "Install APK", listOf("apkPath")),
            ToolInfo("start_app", "Start App", listOf("packageName", "activityName"))
        )
    }

    override suspend fun execute(toolName: String, params: Map<String, String>): String {
        return when (toolName) {
            "check_adb" -> adbManager.checkAdb().let { if (it.success) "ADB OK: ${it.output}" else "ADB Fail: ${it.error}" }
            "list_devices" -> adbManager.listDevices().joinToString("\n") { "${it.serialNumber} (${it.state})" }.ifEmpty { "No devices" }
            "start_emulator" -> {
                val avd = params["avdName"] ?: return "AVD name required"
                val res = adbManager.startEmulator(avd)
                if (res.success) "Emulator started" else "Error: ${res.error}"
            }
            "install_apk" -> {
                val path = params["apkPath"] ?: return "Path required"
                val res = adbManager.installApk(path)
                if (res.success) res.output else "Error: ${res.error}"
            }
            "start_app" -> {
                val pkg = params["packageName"] ?: return "Pkg required"
                val act = params["activityName"] ?: "" // Optional usually, but AdbManager might need it
                val res = adbManager.startApp(pkg, act)
                if (res.success) res.output else "Error: ${res.error}"
            }
            else -> "Unknown tool: $toolName"
        }
    }
}
