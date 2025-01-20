package com.example.batchcookingia

import android.app.VoiceInteractor.Prompt
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.airbnb.lottie.LottieAnimationView
import com.itextpdf.kernel.font.PdfFont
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Paragraph
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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
        val tvMenu: TextView = findViewById(R.id.menuItem1)
        val generatePdfButton: Button = findViewById(R.id.generatePdfButton)
        val languageselector: Spinner = findViewById(R.id.languageInput)
        val loadingAnimation: LottieAnimationView = findViewById(R.id.loadingAnimation)
        val favoriteButton: ImageButton = findViewById(R.id.favoriteButton)

        favoriteButton.visibility = View.GONE

        favoriteButton.setOnClickListener {
            Toast.makeText(this, "Added to favorites.", Toast.LENGTH_SHORT).show()
            if (favoriteButton.drawable.constantState == resources.getDrawable(R.drawable.baseline_favorite_border_24).constantState) {
                setFavorite(true)
            }
            else {
                setFavorite(false)
            }


        }

        // Set up intolerances options
        val intolerancesList = arrayOf("None", "Gluten", "Lactose", "Nuts", "Soy", "Shellfish", "Egg")
        val languageList = arrayOf("English", "Spanish", "French", "German", "Italian", "Japanese", "Chinese", "Korean", "Russian", "Arabic")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, intolerancesList)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        inputIntolerances.adapter = adapter  // Set the adapter to Spinner
        val adapter2 = ArrayAdapter(this, android.R.layout.simple_spinner_item, languageList)
        adapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        languageselector.adapter = adapter2


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
            val language = languageselector.selectedItem.toString()
            val intolerances = inputIntolerances.selectedItem.toString()  // Get selected intolerance

            if (age.isEmpty() || weight.isEmpty() || height.isEmpty()) {
                Toast.makeText(this, "Please fill out all required fields.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            loadingAnimation.visibility = View.VISIBLE
            loadingAnimation.playAnimation()
            tvMenu.text = ""

            // Call the Hugging Face Chat Assistant API to generate menu
            CoroutineScope(Dispatchers.Main).launch {
                fetchMenu(favoriteButton, tvMenu, age, weight, height, intolerances, language) { menuResult ->
                    // Post UI updates to the main thread
                    runOnUiThread {
                        loadingAnimation.visibility = View.GONE
                        loadingAnimation.cancelAnimation()
                        tvMenu.text = menuResult
                    }
                }
            }
        }
    }

    private fun setFavorite(isFavorite: Boolean) {
        val favoriteButton: ImageButton = findViewById(R.id.favoriteButton)
        if (isFavorite) {
            favoriteButton.setImageResource(R.drawable.baseline_favorite_24)
        } else {
            favoriteButton.setImageResource(R.drawable.baseline_favorite_border_24)
        }
    }

    private fun calculeIMC(weight: Double, height: Double): Double {
        return weight / (height * height)
    }

    // Function to fetch the menu by interacting with Hugging Face Chat Assistant API
    // Function to fetch the menu by interacting with Hugging Face Chat Assistant API
    private suspend fun fetchMenu(
        favoriteButton: ImageButton,
        tvMenu: TextView,
        age: String,
        weight: String,
        height: String,
        intolerances: String,
        language: String,
        onResult: (String) -> Unit
    ) {
        withContext(Dispatchers.IO) {

            try {

                val prompt = """
    Generate a balanced weekly meal plan in $language based on the following details of me:
    
    - Age: $age
    - Weight: $weight
    - Height: $height
    - Intolerances: [${intolerances}]

    Please follow this format for the meal plan, providing the name of the meals and their preparation steps for each day of the week (Monday to Sunday):

    LIST OF INGREDIENTS: Provide a list of all ingredients quantities needed for the entire week(comma separated).

    MONDAY:
    - BREAKFAST: Name of breakfast meal
    
    - PREPARATION: 
    Steps to prepare breakfast in a list
    
    - LUNCH: Name of lunch meal
    
    - PREPARATION: 
    Steps to prepare lunch in a list
    
    - DINNER: Name of dinner meal
    
    - PREPARATION: 
    Steps to prepare dinner in a list
    
    TUESDAY:
    - BREAKFAST: Name of breakfast meal
    
    - PREPARATION: 
    Steps to prepare breakfast in a list
    
    - LUNCH: Name of lunch meal
    
    - PREPARATION: 
    Steps to prepare lunch in a list
    
    - DINNER: Name of dinner meal
    
    - PREPARATION: 
    Steps to prepare dinner in a list
    
    WEDNESDAY:
    - BREAKFAST: Name of breakfast meal
    
    - PREPARATION: 
    Steps to prepare breakfast in a list
    
    - LUNCH: Name of lunch meal
    
    - PREPARATION: 
    Steps to prepare lunch in a list
    
    - DINNER: Name of dinner meal 
    
    - PREPARATION: 
    Steps to prepare dinner in a list
    
    THURSDAY:
    - BREAKFAST: Name of breakfast meal
    
    - PREPARATION: 
    Steps to prepare breakfast in a list
    
    - LUNCH: Name of lunch meal
    
    - PREPARATION: 
    Steps to prepare lunch in a list
    
    - DINNER: Name of dinner meal
    
    - PREPARATION: 
    Steps to prepare dinner in a list
    
    FRIDAY:
    - BREAKFAST: Name of breakfast meal
    
    - PREPARATION: 
    Steps to prepare breakfast in a list
    
    - LUNCH: Name of lunch meal
    
    - PREPARATION: 
    Steps to prepare lunch in a list
    
    - DINNER: Name of dinner meal
    
    - PREPARATION: 
    Steps to prepare dinner in a list
    
    SATURDAY:
    - BREAKFAST: Name of breakfast meal
    
    - PREPARATION: 
    Steps to prepare breakfast in a list
    
    - LUNCH: Name of lunch meal
    
    - PREPARATION: 
    Steps to prepare lunch in a list
    
    - DINNER: Name of dinner meal
    
    - PREPARATION: 
    Steps to prepare dinner in a list

    SUNDAY
    - BREAKFAST: Name of breakfast meal
    
    - PREPARATION: 
    Steps to prepare breakfast in a list
    
    - LUNCH: Name of lunch meal
    
    - PREPARATION: 
    Steps to prepare lunch
    
    - DINNER: Name of dinner meal
    
    - PREPARATION: 
    Steps to prepare dinner in a list
    
    Please ensure the meals are balanced and appropriate for my age, weight, height, and intolerances.
    Provide a variety of meals for each day, ensuring there are no ingredients that i'm intolerant to.
""".trimIndent()

                val apiKey = "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"
                val client = OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build()

                val json = JSONObject().apply {
                    put("inputs", prompt)
                }

                val body = RequestBody.create(
                    MediaType.parse("application/json"),
                    json.toString()
                )

                val request = Request.Builder()
                    .url("https://api-inference.huggingface.co/models/mistralai/Mistral-7B-Instruct-v0.2")
                    .addHeader("Authorization", "Bearer $apiKey")
                    .post(body)
                    .build()

                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val responseBody = response.body()?.string() ?: "No response body"
                    try {
                        val menuContent = parseMenu(responseBody, prompt)
                        withContext(Dispatchers.Main) {
                            favoriteButton.visibility = View.VISIBLE
                            onResult(menuContent)
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            onResult("Error: Received non-JSON response from the API.")
                        }
                    }
                } else {
                    val errorMessage = response.body()?.string() ?: "No error message"
                    val errorCode = response.code()
                    withContext(Dispatchers.Main) {
                        onResult("Error: HTTP $errorCode - $errorMessage")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onResult("Error generating the menu. Please restart.")
                }
            }

        }
    }




    fun parseMenu(response: String, prompt: String): String {
        try {
            // Parse the response as a JSONArray instead of JSONObject

            // Now format the meal plan based on the generated text
            val formattedResponse = formatMealPlan(response, prompt)

            return formattedResponse
        } catch (e: Exception) {
            return "Error parsing the menu: ${e.message}"
        }
    }

    fun formatMealPlan(generatedText: String, prompt: String): String {
        // You can further process the text as needed, for now, simply returning it
        // Format the output nicely by splitting into days
        val resp = JSONArray(generatedText)

        // Get the first element of the array (which contains the generated text)
        val generatedText = resp.getJSONObject(0).getString("generated_text")


        // Split the generated text by lines, and process accordingly
        val lines = generatedText.split(prompt)
        // You can add additional formatting or adjustments as necessary
        val result = lines[1].replace("---", "")
        return result
    }

    // Function to generate and save the PDF
    private fun generatePdf(menuContent: String) {
        try {
            // Initialize the PdfWriter and PdfDocument
            val outputDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "MenuPDFs")
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
                document.add(Paragraph("BATCH COOKING WEEK MENU\n").setFont(font))
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
