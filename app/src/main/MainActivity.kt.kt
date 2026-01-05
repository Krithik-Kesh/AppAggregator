package com.example.therapybotautomation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import java.io.File

class `MainActivity.kt` : AppCompatActivity() {

    private lateinit var btnStartTests: Button
    private lateinit var tvStatus: TextView
    private lateinit var tvLog: TextView

    private val PERMISSION_REQUEST_CODE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        checkPermissions()
        setupListeners()
    }

    private fun initViews() {
        btnStartTests = findViewById(R.id.btn_start_tests)
        tvStatus = findViewById(R.id.tv_status)
        tvLog = findViewById(R.id.tv_log)

        tvStatus.text = "Ready! Place prompts.csv in Downloads folder"
    }

    private fun setupListeners() {
        btnStartTests.setOnClickListener {
            startAutomationTests()
        }
    }

    private fun checkPermissions() {
        val permissions = arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )

        val permissionsNeeded = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsNeeded.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsNeeded.toTypedArray(),
                PERMISSION_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                Toast.makeText(this, "Permissions granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Permissions required for file access", Toast.LENGTH_LONG).show()
            }
        }
    }


    private fun startAutomationTests() {
        // Auto-detect CSV file
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val csvPath = "$downloadsDir/prompts.csv"

        val file = File(csvPath)
        if (!file.exists()) {
            Toast.makeText(this, "prompts.csv not found in Downloads folder!", Toast.LENGTH_LONG).show()
            log("✗ Error: prompts.csv not found at: $csvPath")
            log("Please place your CSV file in the Downloads folder and try again")
            return
        }

        btnStartTests.isEnabled = false
        tvStatus.text = "Running tests..."

        CoroutineScope(Dispatchers.IO).launch {
            try {
                log("Initializing Appium automation...")
                val automation = TherapyBotAutomation()

                withContext(Dispatchers.Main) {
                    log("Connecting to Appium server...")
                }

                automation.initializeDriver()

                withContext(Dispatchers.Main) {
                    log("Reading prompts from CSV...")
                }

                val prompts = automation.readPromptsFromCSV(csvPath)

                withContext(Dispatchers.Main) {
                    log("Loaded ${prompts.size} prompts")
                    tvStatus.text = "Testing in progress... (${prompts.size} prompts)"
                }

                val conversations = mutableListOf<Conversation>()

                // Test Ash
                withContext(Dispatchers.Main) {
                    log("\n--- Testing Ash ---")
                }
                conversations.add(automation.testAshApp(prompts))
                withContext(Dispatchers.Main) {
                    log("✓ Ash testing completed")
                }

                // Create test session
                val session = TestSession(
                    testDate = java.time.LocalDateTime.now()
                        .format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    conversations = conversations
                )

                // Save transcripts
                val outputDir = "${Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS
                )}/therapy_bot_transcripts"
                File(outputDir).mkdirs()

                val timestamp = System.currentTimeMillis()
                automation.saveTranscriptsAsJSON(session, "$outputDir/transcripts_$timestamp.json")
                automation.saveTranscriptsAsText(session, "$outputDir/transcripts_$timestamp.txt")

                withContext(Dispatchers.Main) {
                    log("\n✓ All tests completed successfully!")
                    log("Transcripts saved to: $outputDir")
                    tvStatus.text = "Tests completed! Check Downloads folder"
                    btnStartTests.isEnabled = true
                    Toast.makeText(
                        this@MainActivity,
                        "Tests completed! Transcripts saved.",
                        Toast.LENGTH_LONG
                    ).show()
                }

                automation.quit()

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    log("\n✗ Error: ${e.message}")
                    e.printStackTrace()
                    tvStatus.text = "Error occurred. Check logs."
                    btnStartTests.isEnabled = true
                    Toast.makeText(
                        this@MainActivity,
                        "Error: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun log(message: String) {
        runOnUiThread {
            val currentLog = tvLog.text.toString()
            tvLog.text = "$currentLog\n$message"

            // Auto-scroll to bottom
            val scrollView = findViewById<android.widget.ScrollView>(R.id.scroll_log)
            scrollView?.post {
                scrollView.fullScroll(android.widget.ScrollView.FOCUS_DOWN)
            }
        }
    }
}