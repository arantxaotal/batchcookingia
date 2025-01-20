package com.example.batchcookingia

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

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

        // Set up intolerances options
        val intolerancesList = arrayOf("None", "Gluten", "Lactose", "Nuts", "Soy", "Shellfish", "Egg")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, intolerancesList)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        inputIntolerances.adapter = adapter  // Set the adapter to Spinner

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

    LIST_OF_TOTAL_INGREDIENTS: Provide a list of all ingredients needed for the entire week (comma separated).

    MONDAY:
    - MONDAY_BREAKFAST_NAME: Name of breakfast meal
    - MONDAY_BREAKFAST_PREPARATION: Steps to prepare breakfast
    - MONDAY_LUNCH_NAME: Name of lunch meal
    - MONDAY_LUNCH_PREPARATION: Steps to prepare lunch
    - MONDAY_DINNER_NAME: Name of dinner meal
    - MONDAY_DINNER_PREPARATION: Steps to prepare dinner

    TUESDAY:
    - TUESDAY_BREAKFAST_NAME: Name of breakfast meal
    - TUESDAY_BREAKFAST_PREPARATION: Steps to prepare breakfast
    - TUESDAY_LUNCH_NAME: Name of lunch meal
    - TUESDAY_LUNCH_PREPARATION: Steps to prepare lunch
    - TUESDAY_DINNER_NAME: Name of dinner meal
    - TUESDAY_DINNER_PREPARATION: Steps to prepare dinner

    (Repeat the same structure for the rest of the week: Wednesday, Thursday, Friday, Saturday, Sunday)
    
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
                                onResult(parseMenu(responseBody))
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



    fun parseMenu(response: String): String {
        try {
            // Parse the response JSON to extract the "generated_text" field
            val jsonResponse = JSONObject(response)
            val generatedText = jsonResponse.getJSONArray("generated_text").getString(0)

            // Now, we need to clean and format the generated text into a presentable form
            val formattedResponse = formatMealPlan(generatedText)

            return formattedResponse
        } catch (e: Exception) {
            return "Error parsing the menu: ${e.message}"
        }
    }

    fun formatMealPlan(generatedText: String): String {
        // You can further process the text as needed, for now, simply returning it
        // Format the output nicely by splitting into days
        val daysOfWeek = listOf("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY")

        var formattedText = ""
        var currentDay = ""

        // Split the generated text by lines, and process accordingly
        val lines = generatedText.split("\n")

        for (line in lines) {
            val dayFound = daysOfWeek.find { line.contains(it) }

            if (dayFound != null) {
                // If we find a day, add it as the current day header
                currentDay = dayFound
                formattedText += "\n$currentDay:\n"
            } else if (currentDay.isNotEmpty()) {
                // Otherwise, append meal information for the current day
                formattedText += "$line\n"
            }
        }

        // You can add additional formatting or adjustments as necessary
        return formattedText
    }

}
