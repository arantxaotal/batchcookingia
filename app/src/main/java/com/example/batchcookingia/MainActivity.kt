package com.example.batchcookingia

import android.app.VoiceInteractor.Prompt
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.itextpdf.kernel.font.PdfFont
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Paragraph
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import org.json.JSONArray
import java.io.File
import java.io.FileOutputStream


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Bind the views
        val inputAge: EditText = findViewById(R.id.ageInput)
        val inputWeight: EditText = findViewById(R.id.weightInput)
        val inputHeight: EditText = findViewById(R.id.heightInput)
        val inputIntolerances: Spinner = findViewById(R.id.intoleranceInput)  // Change to Spinner
        val btnGenerateMenu: Button = findViewById(R.id.generateMenuButton)
        val progressBar: ProgressBar = findViewById(R.id.progressBar)
        val tvMenu: TextView = findViewById(R.id.menuItem1)
        val generatePdfButton: Button = findViewById(R.id.generatePdfButton)

        // Set up intolerances options
        val intolerancesList = arrayOf("None", "Gluten", "Lactose", "Nuts", "Soy", "Shellfish", "Egg")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, intolerancesList)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        inputIntolerances.adapter = adapter  // Set the adapter to Spinner


        // Set up the OnClickListener for the PDF generation button
        generatePdfButton.setOnClickListener {
            // Get the menu content text from TextView
            val menuContent = tvMenu.text.toString()

            // Check if the menu content is empty
            if (menuContent.isEmpty()) {
                Toast.makeText(this, "Menu is empty, cannot generate PDF.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Call the function to generate and save the PDF
            generatePdf(menuContent)
        }

        // Button to generate menu
        btnGenerateMenu.setOnClickListener {
            val age = inputAge.text.toString().trim()
            val weight = inputWeight.text.toString().trim()
            val height = inputHeight.text.toString().trim()
            val intolerances = inputIntolerances.selectedItem.toString()  // Get selected intolerance

            if (age.isEmpty() || weight.isEmpty() || height.isEmpty()) {
                Toast.makeText(this, "Please fill out all required fields.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            progressBar.visibility = View.VISIBLE
            tvMenu.text = ""

            // Call the Hugging Face Chat Assistant API to generate menu
            CoroutineScope(Dispatchers.Main).launch {
                fetchMenu(age, weight, height, intolerances) { menuResult ->
                    // Post UI updates to the main thread
                    runOnUiThread {
                        progressBar.visibility = View.GONE
                        tvMenu.text = menuResult
                    }
                }
            }
        }
    }

    // Function to fetch the menu by interacting with Hugging Face Chat Assistant API
    private suspend fun fetchMenu(
        age: String,
        weight: String,
        height: String,
        intolerances: String,
        onResult: (String) -> Unit
    ) {
        withContext(Dispatchers.IO) {
            try {

                val prompt = """
    Generate a balanced weekly meal plan based on the following user details:
    
    - Age: $age
    - Weight: $weight
    - Height: $height
    - Intolerances: [${intolerances}]

    Please follow this format for the meal plan, providing the name of the meals and their preparation steps for each day of the week (Monday to Sunday):

    LIST OF INGREDIENTS: Provide a list of all ingredients quantities needed for the entire week(comma separated).

    MONDAY:
    - BREAKFAST: Name of breakfast meal
    - PREPARATION: Steps to prepare breakfast
    - LUNCH: Name of lunch meal
    - PREPARATION: Steps to prepare lunch
    - DINNER: Name of dinner meal
    - PREPARATION: Steps to prepare dinner
    
    TUESDAY:
    - BREAKFAST: Name of breakfast meal
    - PREPARATION: Steps to prepare breakfast
    - LUNCH: Name of lunch meal
    - PREPARATION: Steps to prepare lunch
    - DINNER: Name of dinner meal
    - PREPARATION: Steps to prepare dinner
    
    WEDNESDAY:
    - BREAKFAST: Name of breakfast meal
    - PREPARATION: Steps to prepare breakfast
    - LUNCH: Name of lunch meal
    - PREPARATION: Steps to prepare lunch
    - DINNER: Name of dinner meal
    - PREPARATION: Steps to prepare dinner
    
    THURSDAY:
    - BREAKFAST: Name of breakfast meal
    - PREPARATION: Steps to prepare breakfast
    - LUNCH: Name of lunch meal
    - PREPARATION: Steps to prepare lunch
    - DINNER: Name of dinner meal
    - PREPARATION: Steps to prepare dinner
    THURSDAY_END
    
    FRIDAY:
    - BREAKFAST: Name of breakfast meal
    - PREPARATION: Steps to prepare breakfast
    - LUNCH: Name of lunch meal
    - PREPARATION: Steps to prepare lunch
    - DINNER: Name of dinner meal
    - PREPARATION: Steps to prepare dinner
    
    SATURDAY:
    - BREAKFAST: Name of breakfast meal
    - PREPARATION: Steps to prepare breakfast
    - LUNCH: Name of lunch meal
    - PREPARATION: Steps to prepare lunch
    - DINNER: Name of dinner meal
    - PREPARATION: Steps to prepare dinner

    SUNDAY
    - BREAKFAST: Name of breakfast meal
    - PREPARATION: Steps to prepare breakfast
    - LUNCH: Name of lunch meal
    - PREPARATION: Steps to prepare lunch
    - DINNER: Name of dinner meal
    - PREPARATION: Steps to prepare dinner
    
    Please ensure the meals are balanced and appropriate for the user's age, weight, height, and intolerances.
    Provide a variety of meals for each day, ensuring there are no ingredients that the user is intolerant to.
""".trimIndent()


                val apiKey = "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" // Replace with your Hugging Face API key
                val client = OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)  // Set connection timeout
                    .readTimeout(30, TimeUnit.SECONDS)     // Set read timeout
                    .writeTimeout(30, TimeUnit.SECONDS)    // Set write timeout
                    .build()

                // Create JSON body for the API request
                val json = JSONObject()
                json.put("inputs", prompt)

                val body = RequestBody.create(
                    MediaType.parse("application/json"),
                    json.toString() // Using JSONObject for structured data
                )

                // Change the model endpoint URL to Hugging Face's Chat Assistant
                val request = Request.Builder()
                    .url("https://api-inference.huggingface.co/models/mistralai/Mistral-7B-Instruct-v0.2") // Change to your Chat Assistant model endpoint
                    .addHeader("Authorization", "Bearer $apiKey")
                    .post(body)
                    .build()

                // Make the API call
                client.newCall(request).enqueue(object : Callback {
                    override fun onResponse(call: Call, response: Response) {
                        if (response.isSuccessful) {
                            val responseBody = response.body()?.string() ?: "No response body"
                            try {
                                onResult(parseMenu(responseBody, prompt))
                            } catch (e: Exception) {
                                onResult("Error: Received non-JSON response from the API.")
                            }
                        } else {
                            val errorMessage = response.body()?.string() ?: "No error message"
                            val errorCode = response.code()
                            onResult("Error: HTTP $errorCode - $errorMessage")
                        }
                    }

                    override fun onFailure(call: Call, e: IOException) {
                        // Handle failure
                        onResult("Error generating the menu: ${e.message}")
                    }
                })
            } catch (e: Exception) {
                // Catch any errors that may occur
                onResult("Error generating the menu: ${e.message}")
            }
        }
    }



    fun parseMenu(response: String, prompt: String): String {
        try {
            // Parse the response as a JSONArray instead of JSONObject
            val jsonResponse = JSONArray(response)

            // Get the first element of the array (which contains the generated text)
            val generatedText = jsonResponse.getJSONObject(0).getString("generated_text")

            // Now format the meal plan based on the generated text
            val formattedResponse = formatMealPlan(generatedText, prompt)

            return formattedResponse
        } catch (e: Exception) {
            return "Error parsing the menu: ${e.message}"
        }
    }

    fun formatMealPlan(generatedText: String, prompt: String): String {
        // You can further process the text as needed, for now, simply returning it
        // Format the output nicely by splitting into days


        // Split the generated text by lines, and process accordingly
        val lines = generatedText.split(prompt)
        // You can add additional formatting or adjustments as necessary
        lines[1].replace("---", "")
        return lines[1]
    }

    // Function to generate and save the PDF
    private fun generatePdf(menuContent: String) {
        try {
            // Initialize the PdfWriter and PdfDocument
            val outputDir = File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "MenuPDFs")
            if (!outputDir.exists()) {
                outputDir.mkdirs()
            }
            val file = File(outputDir, "Menu_${System.currentTimeMillis()}.pdf")
            val fileOutputStream = FileOutputStream(file)

            // Initialize PdfWriter for iText 7
            val pdfWriter = PdfWriter(fileOutputStream)
            val pdfDocument = PdfDocument(pdfWriter)
            val document = Document(pdfDocument)

            // Set up the font (using a built-in font in iText 7, like Helvetica or Times-Roman)
            val font: PdfFont = PdfFontFactory.createFont("Helvetica")

            // Add content to the document
            document.add(Paragraph("Generated Menu\n").setFont(font))
            document.add(Paragraph(menuContent).setFont(font))

            // Close the document and file output stream
            document.close()
            fileOutputStream.close()

            // Notify user of success
            Toast.makeText(this, "PDF generated and saved at ${file.absolutePath}", Toast.LENGTH_LONG).show()

        } catch (e: Exception) {
            Toast.makeText(this, "Error generating PDF: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
