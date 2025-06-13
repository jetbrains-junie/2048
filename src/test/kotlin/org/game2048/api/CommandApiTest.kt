package org.game2048.api

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule

class CommandApiTest {
    private lateinit var commandApi: CommandApi
    private val testPort = 8081
    private val objectMapper = ObjectMapper().registerKotlinModule()
    private val httpClient = HttpClient.newBuilder().build()
    
    @BeforeEach
    fun setup() {
        commandApi = CommandApi(port = testPort)
        commandApi.start()
        // Give the server a moment to start
        Thread.sleep(500)
    }
    
    @AfterEach
    fun tearDown() {
        commandApi.stop()
        // Give the server a moment to stop
        Thread.sleep(500)
    }
    
    @Test
    fun `test execute simple command`() = runBlocking {
        // Create request payload
        val requestBody = objectMapper.writeValueAsString(CommandRequest(
            command = "echo 'Hello, World!'",
            timeoutSeconds = 5
        ))
        
        // Send request to API
        val request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:$testPort/api/execute"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build()
            
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        
        // Verify response
        assertEquals(200, response.statusCode())
        
        val commandResponse = objectMapper.readValue(response.body(), CommandResponse::class.java)
        assertEquals(0, commandResponse.exitCode)
        assertTrue(commandResponse.output.contains("Hello, World!"))
        assertNull(commandResponse.error)
    }
    
    @Test
    fun `test execute invalid command`() = runBlocking {
        // Create request payload with empty command
        val requestBody = objectMapper.writeValueAsString(CommandRequest(
            command = "",
            timeoutSeconds = 5
        ))
        
        // Send request to API
        val request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:$testPort/api/execute"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build()
            
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        
        // Verify response
        assertEquals(400, response.statusCode())
        
        val commandResponse = objectMapper.readValue(response.body(), CommandResponse::class.java)
        assertEquals(-1, commandResponse.exitCode)
        assertNotNull(commandResponse.error)
        assertTrue(commandResponse.error!!.contains("Command cannot be empty"))
    }
    
    @Test
    fun `test blocked command`() = runBlocking {
        // Create request payload with a blocked command
        val requestBody = objectMapper.writeValueAsString(CommandRequest(
            command = "rm -rf /some/path",
            timeoutSeconds = 5
        ))
        
        // Send request to API
        val request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:$testPort/api/execute"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build()
            
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        
        // Verify response
        assertEquals(403, response.statusCode())
        
        val commandResponse = objectMapper.readValue(response.body(), CommandResponse::class.java)
        assertEquals(-1, commandResponse.exitCode)
        assertNotNull(commandResponse.error)
        assertTrue(commandResponse.error!!.contains("blocked for security reasons"))
    }
}