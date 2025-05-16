package com.example.smartfreezer

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.ComponentActivity

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main) // Enlazamos con el XML

        // Referencia al botón en el XML
        val botonVamosAlla = findViewById<Button>(R.id.button)

        // Configurar el botón para navegar a la segunda pantalla
        botonVamosAlla.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }
    }


}
