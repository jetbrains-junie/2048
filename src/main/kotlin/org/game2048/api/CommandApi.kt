package org.game2048.api

import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

/**
 * Data class representing a command execution request.
 */
data class CommandRequest(
    val command: String,
    val timeoutSeconds: Int = 30
) {
    /**
     * Validates the request parameters.
     * @throws IllegalArgumentException if validation fails
     */
    fun validate() {
        require(command.isNotBlank()) { "Command cannot be empty" }
        require(timeoutSeconds in 1..300) { "Timeout must be between 1 and 300 seconds" }
    }
}

/**
 * Data class representing a command execution response.
 */
data class CommandResponse(
    val output: String,
    val exitCode: Int,
    val error: String? = null
)

/**
 * Class responsible for setting up and managing the command execution API.
 */
class CommandApi(private val port: Int = 8080) {
    private var server: ApplicationEngine? = null
    private val logger = LoggerFactory.getLogger(CommandApi::class.java)
    
    // List of potentially dangerous commands that should be blocked
    private val blockedCommands = listOf(
        "rm -rf", "mkfs", "dd", ":(){ :|:& };:", "chmod -R 777 /", 
        "> /dev/sda", "/dev/null", "mv / /dev/null"
    )

    /**
     * Starts the API server.
     */
    fun start() {
        server = embeddedServer(Netty, port = port) {
            install(ContentNegotiation) {
                jackson()
            }
            
            routing {
                route("/api") {
                    post("/execute") {
                        try {
                            val request = call.receive<CommandRequest>()
                            
                            // Validate request
                            try {
                                request.validate()
                            } catch (e: IllegalArgumentException) {
                                logger.warn("Invalid request: ${e.message}")
                                call.respond(
                                    HttpStatusCode.BadRequest,
                                    CommandResponse(
                                        output = "",
                                        exitCode = -1,
                                        error = "Invalid request: ${e.message}"
                                    )
                                )
                                return@post
                            }
                            
                            // Check for dangerous commands
                            if (isCommandBlocked(request.command)) {
                                logger.warn("Blocked potentially dangerous command: ${request.command}")
                                call.respond(
                                    HttpStatusCode.Forbidden,
                                    CommandResponse(
                                        output = "",
                                        exitCode = -1,
                                        error = "Command execution blocked for security reasons"
                                    )
                                )
                                return@post
                            }
                            
                            logger.info("Executing command: ${request.command}")
                            val result = executeCommand(request.command, request.timeoutSeconds)
                            call.respond(HttpStatusCode.OK, result)
                        } catch (e: Exception) {
                            logger.error("Error processing request", e)
                            call.respond(
                                HttpStatusCode.InternalServerError,
                                CommandResponse(
                                    output = "",
                                    exitCode = -1,
                                    error = "Error executing command: ${e.message}"
                                )
                            )
                        }
                    }
                }
            }
        }.start(wait = false)
        
        logger.info("Command API server started on port $port")
        println("Command API server started on port $port")
    }

    /**
     * Stops the API server.
     */
    fun stop() {
        server?.stop(1, 5, TimeUnit.SECONDS)
        logger.info("Command API server stopped")
        println("Command API server stopped")
    }
    
    /**
     * Checks if a command contains blocked patterns.
     * 
     * @param command The command to check
     * @return true if the command is blocked, false otherwise
     */
    private fun isCommandBlocked(command: String): Boolean {
        val lowercaseCommand = command.lowercase()
        return blockedCommands.any { blockedCmd -> 
            lowercaseCommand.contains(blockedCmd.lowercase()) 
        }
    }

    /**
     * Executes an OS-level command and returns the result.
     *
     * @param command The command to execute
     * @param timeoutSeconds Timeout in seconds
     * @return CommandResponse containing the output and exit code
     */
    private suspend fun executeCommand(command: String, timeoutSeconds: Int): CommandResponse {
        return withContext(Dispatchers.IO) {
            try {
                logger.debug("Starting command execution: $command")
                val process = ProcessBuilder()
                    .command("sh", "-c", command)
                    .redirectErrorStream(true)
                    .start()

                val output = StringBuilder()
                BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        output.append(line).append("\n")
                    }
                }

                val completed = process.waitFor(timeoutSeconds.toLong(), TimeUnit.SECONDS)
                if (!completed) {
                    process.destroy()
                    logger.warn("Command execution timed out after $timeoutSeconds seconds: $command")
                    return@withContext CommandResponse(
                        output = output.toString(),
                        exitCode = -1,
                        error = "Command execution timed out after $timeoutSeconds seconds"
                    )
                }

                val exitCode = process.exitValue()
                logger.debug("Command completed with exit code $exitCode: $command")
                CommandResponse(
                    output = output.toString(),
                    exitCode = exitCode
                )
            } catch (e: Exception) {
                logger.error("Error executing command: $command", e)
                CommandResponse(
                    output = "",
                    exitCode = -1,
                    error = "Error executing command: ${e.message}"
                )
            }
        }
    }
}