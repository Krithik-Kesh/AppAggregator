// Main execution
fun main() {
    val automation = TherapyBotAutomation()

    try {
        // initialize Appium Driver
        automation.initializeDriver()

        // Read prompts from CSV
        val csvPath = "/sdcard/Download/prompts.csv" //this needs to be the csv path
        val prompts = automation.readPromptsFromCSV(csvPath)

        println("Loaded ${prompts.size} prompts from CSV")

        // Run tests on all three apps
        val conversations = mutableListOf<Conversation>()

        conversations.add(automation.testAshApp(prompts))
        conversations.add(automation.testDoroApp(prompts))
        conversations.add(automation.testWysaApp(prompts))

        // Create test session
        val session = TestSession(
            testDate = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            conversations = conversations
        )

        // Save transcripts
        val outputDir = "/sdcard/Download/therapy_bot_transcripts" //update later with the output paths
        File(outputDir).mkdirs()

        val timestamp = System.currentTimeMillis()
        automation.saveTranscriptsAsJSON(session, "$outputDir/transcripts_$timestamp.json")
        automation.saveTranscriptsAsText(session, "$outputDir/transcripts_$timestamp.txt")

        println("Testing completed successfully!")

    } catch (e: Exception) {
        println("Error during testing: ${e.message}")
        e.printStackTrace()
    } finally {
        automation.quit()
    }
}