package core.ports

import core.domain.model.Message

interface InteractionService {
    fun readInput(): String
    fun writeOutput(message: String)
    fun writeMessage(message: Message)
    fun writeError(error: String)
}
