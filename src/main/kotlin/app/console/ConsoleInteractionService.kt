package app.console

import core.domain.model.Message
import core.domain.model.Role
import core.ports.InteractionService

class ConsoleInteractionService : InteractionService {
    override fun readInput(): String {
        print("> ")
        return readlnOrNull() ?: ""
    }

    override fun writeOutput(message: String) {
        println(message)
    }

    override fun writeMessage(message: Message) {
        val prefix = when (message.role) {
            Role.USER -> "You"
            Role.ASSISTANT -> "AI"
            Role.SYSTEM -> "System"
        }
        println("\n[$prefix]: ${message.content}")
    }

    override fun writeError(error: String) {
        System.err.println("Error: $error")
    }
}
