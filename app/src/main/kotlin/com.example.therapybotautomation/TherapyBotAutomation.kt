package com.example.therapybotautomation

import com.google.gson.reflect.TypeToken
import androidx.compose.foundation.layout.add
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.text
import androidx.core.util.readText
import io.appium.java_client.AppiumDriver
import io.appium.java_client.android.AndroidDriver
import io.appium.java_client.android.options.UiAutomator2Options
import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import org.openqa.selenium.support.ui.WebDriverWait
import org.openqa.selenium.support.ui.ExpectedConditions
import com.opencsv.CSVReader
import com.google.gson.GsonBuilder
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.net.URL
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

//Data Model
data class Prompt(
    val text: String,
    val emotion: String? = null,
    val id: Int? = null,
    val risk_level: String? = null
)

data class Message(
    val timestamp: String,
    val type: String,
    val content: String
)

data class Conversation(
    val appName: String,
    val sessionId: String,
    val startTime: String,
    val endTime: String,
    val messages: List<Message>
)

data class TestSession(
    val testDate: String,
    val conversations: List<Conversation>
)

// Main automation class
class TherapyBotAutomation {

    private lateinit var driver: AndroidDriver
    private val wait by lazy { WebDriverWait(driver, Duration.ofSeconds(30)) }
    private val gson = GsonBuilder().setPrettyPrinting().create()

    // App package names (THESE WILL BE UPDATED LATER WITH ACTUAL PACKAGE NAMES)
    companion object {
        const val ASH_PACKAGE = "com.ash.therapy" // Replace with actual package
        const val DORO_PACKAGE = "ca.raroze.doro.app"
        const val WYSA_PACKAGE = "bot.touchkin.ui.chat.WysaChatActivity"
        const val APPIUM_SERVER = "http://127.0.0.1:4723" // Default Appium server
    }

    // Initialize Appium driver
    fun initializeDriver() {
        val options = UiAutomator2Options()
            .setPlatformName("Android")
            .setAutomationName("UiAutomator2")
            .setNoReset(false) // Depending on whether or not we will want to keep the app data this will be set to true
            .setFullReset(false)

        driver = AndroidDriver(URL(APPIUM_SERVER), options)
    }

    fun readPromptsFromJSON(jsonPath: String): List<Prompt> {
        return try {
            val file = File(jsonPath)
            if (!file.exists()) {
                java.io.IO.println("JSON file not found at: $jsonPath")
                return java.util.Collections.emptyList()
            }

            val jsonString = file.readText()
            val listType = object : TypeToken<List<Prompt>>() {}.type
            gson.fromJson(jsonString, listType)
        } catch (e: java.lang.Exception) {
            java.io.IO.println("Error reading JSON prompts: ${e.message}")
            java.util.Collections.emptyList()
        }
    }

    // Test Ash app
    fun testAshApp(prompts: List<Prompt>): Conversation {
        println("Starting Ash automation...")
        val messages = mutableListOf<Message>()
        val sessionId = generateSessionId()
        val startTime = getCurrentTimestamp()

        try {
            // Launch Ash app
            launchApp(ASH_PACKAGE)
            Thread.sleep(3000) // Wait for app to load

            // Skip onboarding if needed
            skipOnboarding()

            prompts.forEach { prompt ->
                val response = sendPromptAndGetResponse(
                    prompt.text,
                    findChatInput(),
                    findSendButton(),
                    findBotResponse()
                )

                messages.add(Message(getCurrentTimestamp(), "user", prompt.text))
                if (response != null) {
                    messages.add(Message(getCurrentTimestamp(), "bot", response))
                }

                Thread.sleep(2000) // Wait between messages
            }

        } catch (e: Exception) {
            println("Error in Ash automation: ${e.message}")
            e.printStackTrace()
        } finally {
            closeApp()
        }

        return Conversation("Ash", sessionId, startTime, getCurrentTimestamp(), messages)
    }

    // Test Doro app
    fun testDoroApp(prompts: List<Prompt>): Conversation {
        java.io.IO.println("Starting Doro automation...")
        return runGenericTest("Doro", DORO_PACKAGE, prompts)
    }

    // Test Wysa app
    fun testWysaApp(prompts: List<Prompt>): Conversation {
        java.io.IO.println("Starting Wysa automation...")
        return runGenericTest("Wysa", WYSA_PACKAGE, prompts)
    }

    // A generic helper to avoid repeating code for each app
    private fun runGenericTest(appName: String, packageName: String, prompts: List<Prompt>): Conversation {
        val messages = kotlin.collections.mutableListOf<Message>()
        val sessionId = generateSessionId()
        val startTime = getCurrentTimestamp()

        try {
            launchApp(packageName)
            java.lang.Thread.sleep(5000) // Apps take time to open

            prompts.forEach { prompt ->
                // This uses the "find" logic wrote multiple IDS
                val response = sendPromptAndGetResponse(
                    prompt.text,
                    findChatInput(),
                    findSendButton(),
                    findBotResponse()
                )

                messages.add(Message(getCurrentTimestamp(), "user", prompt.text))
                if (response != null) {
                    messages.add(Message(getCurrentTimestamp(), "bot", response))
                }
                Thread.sleep(2000)
            }
        } catch (e: java.lang.Exception) {
            java.io.IO.println("Error in $appName automation: ${e.message}")
        } finally {
            closeApp()
        }

        return Conversation(appName, sessionId, startTime, getCurrentTimestamp(), messages)
    }
    // Helper: Launch app
    private fun launchApp(packageName: String) {
        driver.activateApp(packageName)
    }

    // Helper: Close app
    private fun closeApp() {
        driver.terminateApp(driver.currentPackage)
    }

    // Helper: Skip onboarding screens
    private fun skipOnboarding() {
        try {
            // Look for common skip/continue buttons
            val skipButtons = listOf(
                "skip", "Skip", "SKIP",
                "continue", "Continue", "CONTINUE",
                "get started", "Get Started", "GET STARTED",
                "next", "Next", "NEXT"
            )

            skipButtons.forEach { text ->
                try {
                    val element = driver.findElement(
                        By.xpath("//*[contains(@text, '$text') or contains(@content-desc, '$text')]")
                    )
                    element.click()
                    Thread.sleep(1000)
                } catch (e: Exception) {
                    // Element not found, continue
                }
            }
        } catch (e: Exception) {
            println("No onboarding to skip or already past it")
        }
    }

    // Helper: Find chat input field
    private fun findChatInput(): WebElement {
        // Try multiple common selectors for chat input
        val selectors = listOf(
            By.id("message_input"),
            By.id("chat_input"),
            By.id("edittext"),
            By.className("android.widget.EditText"),
            By.xpath("//android.widget.EditText")
        )

        for (selector in selectors) {
            try {
                return wait.until(ExpectedConditions.presenceOfElementLocated(selector))
            } catch (e: Exception) {
                continue
            }
        }

        throw Exception("Could not find chat input field")
    }

    // Helper: Find send button
    private fun findSendButton(): WebElement {
        val selectors = listOf(
            By.id("send_button"),
            By.id("btn_send"),
            By.xpath("//*[@content-desc='Send' or @text='Send']"),
            By.xpath("//android.widget.ImageButton")
        )

        for (selector in selectors) {
            try {
                return driver.findElement(selector)
            } catch (e: Exception) {
                continue
            }
        }

        throw Exception("Could not find send button")
    }

    // Helper :Find bot response
    private fun findBotResponse(): WebElement {
        Thread.sleep(3000) // Wait for response

        val selectors = listOf(
            By.xpath("//android.widget.TextView[last()]"),
            By.className("android.widget.TextView")
        )
        for (selector in selectors) {
            try {
                val elements = driver.findElements(selector)
                if (elements.isNotEmpty()) {
                    return elements.last()
                }
            } catch (e: Exception) {
                continue
            }
        }

        throw Exception("Could not find bot response")
    }

    // Send prompt and get response
    private fun sendPromptAndGetResponse(
        prompt: String,
        inputField: WebElement,
        sendButton: WebElement,
        responseElement: WebElement
    ): String? {
        try {
            inputField.clear()
            inputField.sendKeys(prompt)
            Thread.sleep(500)
            sendButton.click()

            // Wait for response
            Thread.sleep(6000)

            return responseElement.text
        } catch (e: Exception) {
            println("Error sending prompt: ${e.message}")
            return null
        }
    }

    // Save transcripts as JSON WOOOP
    fun saveTranscriptsAsJSON(session: TestSession, outputPath: String) {
        val json = gson.toJson(session)
        FileWriter(outputPath).use { it.write(json) }
        println("Transcripts saved to: $outputPath")
    }

    // Utility functions
    private fun generateSessionId(): String {
        return "session_${System.currentTimeMillis()}"
    }

    private fun getCurrentTimestamp(): String {
        return LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    }

    // Cleanup
    fun quit() {
        if (::driver.isInitialized) {
            driver.quit()
        }
    }
    // (This closes the TherapyBotAutomation class)
}

// Main execution
fun main() {
    val automation = TherapyBotAutomation()
    try {
        automation.initializeDriver()

        // Path to the JSON file (ensure this is accessible to the test runner)
        val jsonPath = "/Users/krithiktamilselvan/Downloads/prompts.json"
        val prompts = automation.readPromptsFromJSON(jsonPath)

        if (prompts.isEmpty()) {
            java.io.IO.println("No prompts loaded. Exiting.")
            return
        }

        java.io.IO.println("Loaded ${prompts.size} prompts successfully.")

        // Run tests...
        val conversations = kotlin.collections.mutableListOf<Conversation>()
        conversations.add(automation.testAshApp(prompts))
// ...