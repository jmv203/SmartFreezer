package com.example.smartfreezer

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.smartfreezer.databinding.ActivityFeedbackBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.*

class FeedbackActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFeedbackBinding
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFeedbackBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firestore = Firebase.firestore
        auth = FirebaseAuth.getInstance()

        binding.buttonSubmitFeedback.setOnClickListener {
            submitFeedback()
        }

        binding.btnBackFeedback.setOnClickListener {
            finish()
        }
    }

    private fun submitFeedback() {
        val overallRating = binding.ratingBarOverall.rating
        val q1Rating = binding.ratingQuestion1.rating
        val q2Rating = binding.ratingQuestion2.rating
        val q3Rating = binding.ratingQuestion3.rating
        val q4Rating = binding.ratingQuestion4.rating
        val q5Rating = binding.ratingQuestion5.rating

        val errorDescription = binding.editTextErrorDescription.text.toString().trim()
        val improvementSuggestions = binding.editTextImprovementSuggestions.text.toString().trim()

        if (overallRating == 0f && errorDescription.isEmpty() && improvementSuggestions.isEmpty()) {
            Toast.makeText(this,
                getString(R.string.por_favor_proporciona_alguna_valoraci_n_o_comentario), Toast.LENGTH_LONG).show()
            return
        }

        binding.buttonSubmitFeedback.isEnabled = false
        Toast.makeText(this, getString(R.string.enviando_valoraci_n), Toast.LENGTH_SHORT).show()


        val feedbackData = hashMapOf(
            "userId" to (auth.currentUser?.uid ?: "anonymous_${UUID.randomUUID()}"),
            "timestamp" to FieldValue.serverTimestamp(),
            "overallRating" to overallRating,
            "questionnaire" to hashMapOf(
                "ease_of_use" to q1Rating,
                "good_ui" to q2Rating,
                "no_error" to q3Rating,
                "useful" to q4Rating,
                "understandability" to q5Rating,
            ),
            "errorDescription" to errorDescription,
            "improvementSuggestions" to improvementSuggestions,
            "deviceInfo" to hashMapOf(
                "model" to Build.MODEL,
                "manufacturer" to Build.MANUFACTURER,
                "osVersion" to Build.VERSION.RELEASE,
                "sdkInt" to Build.VERSION.SDK_INT
            )
        )

        firestore.collection("feedback")
            .add(feedbackData)
            .addOnSuccessListener { documentReference ->
                Log.d("FeedbackActivity", "Feedback enviado con ID: ${documentReference.id}")
                Toast.makeText(this, "¡Gracias por tu valoración!", Toast.LENGTH_LONG).show()
                finish() // Cierra la actividad después de enviar
            }
            .addOnFailureListener { e ->
                Log.w("FeedbackActivity", "Error al enviar feedback", e)
                Toast.makeText(this, "Error al enviar: ${e.message}", Toast.LENGTH_LONG).show()
                binding.buttonSubmitFeedback.isEnabled = true
            }
    }
}
