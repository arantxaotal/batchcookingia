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
        val inputIntolerances: MultiAutoCompleteTextView = findViewById(R.id.intoleranceInput)
        val btnGenerateMenu: Button = findViewById(R.id.generateMenuButton)
        val progressBar: ProgressBar = findViewById(R.id.progressBar)
        val tvMenu: TextView = findViewById(R.id.menuItem1)

        // Set up intolerances options
        val intolerancesList = arrayOf("Gluten", "Lactosa", "Nueces", "Soja", "Mariscos", "Huevo")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, intolerancesList)
        inputIntolerances.setAdapter(adapter)
        inputIntolerances.setTokenizer(MultiAutoCompleteTextView.CommaTokenizer())

        // Button to generate menu
        btnGenerateMenu.setOnClickListener {
            val age = inputAge.text.toString().trim()
            val weight = inputWeight.text.toString().trim()
            val height = inputHeight.text.toString().trim()
            val intolerances = inputIntolerances.text.toString().trim()

            if (age.isEmpty() || weight.isEmpty() || height.isEmpty()) {
                Toast.makeText(this, "Por favor, completa todos los campos obligatorios.", Toast.LENGTH_SHORT).show()
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
                // Create the prompt based on user data
                val prompt = """
                    Generate a healthy week menu in a table for a person who has the following characteristics:
                    - Age: $age years old
                    - Weight: $weight kg
                    - Height: $height cm
                    - Intolerances: ${if (intolerances.isEmpty()) "Ninguna" else intolerances}
                    Then include a list of ingredients and the details of preparation of the menu in two hours.
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
                            val menuResponse = response.body()?.string() ?: "No response body"
                            // Call the onResult callback to pass the result
                            onResult(menuResponse)  // Will run on the background thread
                        } else {
                            val errorMessage = response.body()?.string() ?: "No error message"
                            val errorCode = response.code()
                            onResult("Error: HTTP $errorCode - $errorMessage") // Pass error to callback
                        }
                    }

                    override fun onFailure(call: Call, e: IOException) {
                        // Handle failure
                        onResult("Error al generar el menú: ${e.message}")
                    }
                })
            } catch (e: Exception) {
                // Catch any errors that may occur
                onResult("Error al generar el menú: ${e.message}")
            }
        }
    }
}
