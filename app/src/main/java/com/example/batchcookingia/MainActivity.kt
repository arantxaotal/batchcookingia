package com.example.batchcookingia


import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Vincular vistas
        val inputAge: EditText = findViewById(R.id.inputAge)
        val inputWeight: EditText = findViewById(R.id.inputWeight)
        val inputHeight: EditText = findViewById(R.id.inputHeight)
        val inputIntolerances: MultiAutoCompleteTextView = findViewById(R.id.inputIntolerances)
        val btnGenerateMenu: Button = findViewById(R.id.btnGenerateMenu)
        val progressBar: ProgressBar = findViewById(R.id.progressBar)
        val tvMenu: TextView = findViewById(R.id.tvMenu)

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

                // Simular llamada a API (o reemplazar con llamada real)
                kotlinx.coroutines.delay(2000) // Simular el tiempo de respuesta
                val menuResponse = """
                    Menú generado (sin incluir: $intolerances):
                    Lunes: Ensalada César sin gluten
                    Martes: Pasta integral con pollo
                    Miércoles: Sopa de verduras
                    Ingredientes: Pollo, pasta integral, vegetales.
                    Preparación: Sigue las instrucciones estándar para cada platillo.
                """.trimIndent()

                onResult(menuResponse)
            } catch (e: Exception) {
                onResult("Error al generar el menú.")
            }
        }
    }
}
