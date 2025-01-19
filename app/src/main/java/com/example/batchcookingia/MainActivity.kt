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

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Vincular vistas
        val inputAge: EditText = findViewById(R.id.ageInput)
        val inputWeight: EditText = findViewById(R.id.weightInput)
        val inputHeight: EditText = findViewById(R.id.heightInput)
        val inputIntolerances: MultiAutoCompleteTextView = findViewById(R.id.intoleranceInput)
        val btnGenerateMenu: Button = findViewById(R.id.generateMenuButton)
        val progressBar: ProgressBar = findViewById(R.id.progressBar)
        val tvMenu: TextView = findViewById(R.id.menuItem1)

        // Configurar opciones para intolerancias
        val intolerancesList = arrayOf("Gluten", "Lactosa", "Nueces", "Soja", "Mariscos", "Huevo")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, intolerancesList)
        inputIntolerances.setAdapter(adapter)
        inputIntolerances.setTokenizer(MultiAutoCompleteTextView.CommaTokenizer())

        // Botón para generar el menú
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

            CoroutineScope(Dispatchers.Main).launch {
                fetchMenu(age, weight, height, intolerances) { menuResult ->
                    progressBar.visibility = View.GONE
                    tvMenu.text = menuResult
                }
            }
        }
    }

    private suspend fun fetchMenu(
        age: String,
        weight: String,
        height: String,
        intolerances: String,
        onResult: (String) -> Unit
    ) {
        withContext(Dispatchers.IO) {
            try {
                // Crear el prompt basado en los datos del usuario
                val prompt = """
                    Genera un menú semanal equilibrado para una persona con las siguientes características:
                    - Edad: $age años
                    - Peso: $weight kg
                    - Altura: $height cm
                    - Intolerancias alimentarias: ${if (intolerances.isEmpty()) "Ninguna" else intolerances}
                    Incluye lista de ingredientes y preparación detallada.
                """.trimIndent()

                val apiKey = "Bearer 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"
                val client = OkHttpClient()

                // Crear cuerpo de la solicitud
                val json = JSONObject()
                json.put("inputs", prompt)

                val body = RequestBody.create(
                    MediaType.get("application/json"),
                    json.toString()  // Using JSONObject for structured data
                )

                val request = Request.Builder()
                    .url("https://api-inference.huggingface.co/models/meta-llama/Llama-3.3-70B-Instruct")
                    .addHeader("Authorization", apiKey)
                    .post(body)
                    .build()

                client.newCall(request).enqueue(object : Callback {
                    override fun onResponse(call: Call, response: Response) {
                        if (response.isSuccessful) {
                            val menuResponse = response.body()?.string() ?: "No response body"
                            onResult(menuResponse)
                        } else {
                            // Logging the error code and response body
                            val errorMessage = response.body()?.string() ?: "No error message"
                            val errorCode = response.code()
                            onResult("Error: HTTP $errorCode - $errorMessage")
                        }
                    }

                    override fun onFailure(call: Call, e: IOException) {
                        onResult("Error al generar el menú: ${e.message}")
                    }
                })
            } catch (e: Exception) {
                onResult("Error al generar el menú: ${e.message}")
            }
        }
    }
}
