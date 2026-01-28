package infra.utils

import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.File
import java.util.concurrent.TimeUnit

@Serializable
data class CommandResult(
    val success: Boolean,
    val output: String,
    val error: String = ""
)

@Serializable
data class Device(
    val serialNumber: String,
    val state: String
)

data class ApkInfo(
    val packageName: String,
    val launchActivity: String?
)

class AdbManager {
    // Using println instead of Logger for simplicity in this project, or we can use SLF4J if dependency exists.
    // Keeping it simple as requested without heavy deps if possible, but the original used LoggerFactory.
    // I'll stick to println for console app context to avoid missing SLF4J binding issues, 
    // or generic "Logger" interface wrapper. Let's use println wrapped in private log.
    
    private val osName = System.getProperty("os.name").lowercase()
    private val isWindows = osName.contains("win")

    private val androidHome = findAndroidHome()
    private val adbPath = findAdbPath()
    private val emulatorPath = findEmulatorPath()
    private val aaptPath = findAaptPath()

    private var lastInstalledApkInfo: ApkInfo? = null

    init {
        log("🔧 AdbManager initialized. OS: $osName")
        if (androidHome == null) log("⚠️ ANDROID_HOME not found")
        if (adbPath == null) log("⚠️ ADB not found")
    }

    private fun log(msg: String) {
        println("[AdbManager] $msg")
    }

    fun getLastInstalledApkInfo(): ApkInfo? = lastInstalledApkInfo

    private fun findAndroidHome(): String? {
        val envHome = System.getenv("ANDROID_HOME") ?: System.getenv("ANDROID_SDK_ROOT")
        if (envHome != null && File(envHome).exists()) return envHome

        val defaultPaths = when {
            isWindows -> listOf(
                "${System.getenv("LOCALAPPDATA")}\\Android\\Sdk",
                "${System.getProperty("user.home")}\\AppData\\Local\\Android\\Sdk"
            )
            osName.contains("mac") -> listOf("${System.getProperty("user.home")}/Library/Android/sdk")
            osName.contains("nux") -> listOf("${System.getProperty("user.home")}/Android/Sdk", "/opt/android-sdk")
            else -> emptyList()
        }
        return defaultPaths.firstOrNull { File(it).exists() }
    }

    private fun findAdbPath(): String? {
        val adbExecutable = if (isWindows) "adb.exe" else "adb"
        if (androidHome != null) {
            val adbInSdk = File(androidHome, "platform-tools/$adbExecutable")
            if (adbInSdk.exists()) return adbInSdk.absolutePath
        }
        return null // Simplified for brevity, usually in platform-tools
    }

    private fun findEmulatorPath(): String? {
        val emulatorExecutable = if (isWindows) "emulator.exe" else "emulator"
        if (androidHome != null) {
            val emulatorInSdk = File(androidHome, "emulator/$emulatorExecutable")
            if (emulatorInSdk.exists()) return emulatorInSdk.absolutePath
        }
        return null
    }

    private fun findAaptPath(): String? {
        val aaptExecutable = if (isWindows) "aapt.exe" else "aapt"
        if (androidHome != null) {
            val buildToolsDir = File(androidHome, "build-tools")
            if (buildToolsDir.exists()) {
                buildToolsDir.listFiles()?.sortedByDescending { it.name }?.forEach { versionDir ->
                    val aaptFile = File(versionDir, aaptExecutable)
                    if (aaptFile.exists()) return aaptFile.absolutePath
                }
            }
        }
        return null
    }

    fun checkAdb(): CommandResult {
        if (adbPath == null) return CommandResult(false, "", "ADB not found")
        return executeCommand(listOf(adbPath, "version"))
    }

    fun listDevices(): List<Device> {
        if (adbPath == null) return emptyList()
        val result = executeCommand(listOf(adbPath, "devices"))
        if (!result.success) return emptyList()
        return result.output.lines().drop(1).mapNotNull { line ->
            val parts = line.split(Regex("\\s+"))
            if (parts.size >= 2) Device(parts[0], parts[1]) else null
        }
    }

    fun getLogcat(packageName: String, lines: Int = 50): CommandResult {
        if (adbPath == null) return CommandResult(false, "", "ADB not found")
        
        // Simplified grep for cross-platform (using pure shell might fail on Windows without tools)
        // Fallback to basic dump
        val command = listOf(adbPath, "logcat", "-d", "-t", lines.toString())
        return executeCommand(command, 10)
    }

    fun installApk(apkPath: String, reinstall: Boolean = true): CommandResult {
        if (adbPath == null) return CommandResult(false, "", "ADB not found")
        val command = mutableListOf(adbPath, "install").apply { if (reinstall) add("-r"); add(apkPath) }
        return executeCommand(command, 120)
    }

    fun startApp(packageName: String, activityName: String): CommandResult {
        if (adbPath == null) return CommandResult(false, "", "ADB not found")
        val fullActivityName = if (activityName.startsWith(".")) "$packageName$activityName" else activityName
        return executeCommand(listOf(adbPath, "shell", "am", "start", "-n", "$packageName/$fullActivityName"))
    }
    
    fun startEmulator(avdName: String): CommandResult {
        if (emulatorPath == null) return CommandResult(false, "", "Emulator not found")
        // Start detached
        try {
            val pb = ProcessBuilder(listOf(emulatorPath, "-avd", avdName, "-no-snapshot-load"))
            pb.start()
            return CommandResult(true, "Emulator starting...")
        } catch (e: Exception) {
            return CommandResult(false, "", e.message ?: "Error")
        }
    }
    
    fun waitForDevice(timeoutSeconds: Int = 300): CommandResult {
         if (adbPath == null) return CommandResult(false, "", "ADB not found")
         val res = executeCommand(listOf(adbPath, "wait-for-device"), timeoutSeconds.toLong())
         return res
    }

    private fun executeCommand(command: List<String>, timeoutSeconds: Long = 60): CommandResult {
        return try {
            val process = ProcessBuilder(command).start()
            val output = StringBuilder()
            val error = StringBuilder()
            
            // Read streams in background to prevent blocking
            val t1 = Thread { process.inputStream.bufferedReader().forEachLine { output.appendLine(it) } }
            val t2 = Thread { process.errorStream.bufferedReader().forEachLine { error.appendLine(it) } }
            t1.start(); t2.start()

            if (!process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
                process.destroyForcibly()
                return CommandResult(false, output.toString(), "Timeout")
            }
            t1.join(); t2.join()
            
            CommandResult(process.exitValue() == 0, output.toString().trim(), error.toString().trim())
        } catch (e: Exception) {
            CommandResult(false, "", e.message ?: "Error")
        }
    }
}
