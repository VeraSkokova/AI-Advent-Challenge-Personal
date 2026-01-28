package infra.utils

import java.io.File
import java.util.concurrent.TimeUnit

class GitClient(private val rootDir: File = File(".")) {
    fun currentBranch(): String {
        return runCommand("git", "rev-parse", "--abbrev-ref", "HEAD").output
    }

    fun status(): String {
        return runCommand("git", "status").output
    }
    
    fun diffFiles(): List<String> {
        val output = runCommand("git", "diff", "--name-only", "HEAD").output
        return output.lines().filter { it.isNotBlank() }
    }
    
    fun getDiffContent(): String {
        return runCommand("git", "diff", "HEAD").output
    }
    
    fun getRemoteOriginUrl(): String {
        return runCommand("git", "config", "--get", "remote.origin.url").output
    }

    private fun runCommand(vararg args: String): CommandResult {
        return try {
            val process = ProcessBuilder(*args)
                .directory(rootDir)
                .redirectErrorStream(true)
                .start()
            
            process.waitFor(5, TimeUnit.SECONDS)
            val output = process.inputStream.bufferedReader().readText().trim()
            CommandResult(process.exitValue() == 0, output)
        } catch (e: Exception) {
            CommandResult(false, e.message ?: "Error")
        }
    }
    
    data class CommandResult(val success: Boolean, val output: String)
}
