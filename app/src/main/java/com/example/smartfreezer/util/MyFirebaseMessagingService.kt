package com.example.smartfreezer.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.smartfreezer.HomeActivity
import com.example.smartfreezer.R
import com.example.smartfreezer.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "MyFirebaseMsgService"
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "From: ${remoteMessage.from}")

        // Variables para los datos de la notificación
        var notificationTitle = remoteMessage.notification?.title ?: "SmartFreezer"
        var notificationBody = remoteMessage.notification?.body ?: "Tenemos nuevas sugerencias para ti."

        // Extraer datos personalizados
        val dataPayload = remoteMessage.data
        val filterDiet = dataPayload["FILTER_DIET"]
        val recipeId = dataPayload["RECIPE_ID"]

        Log.d(TAG, "Data Payload: $dataPayload")

        // Mostrar notificación
        sendNotification(notificationTitle, notificationBody, filterDiet, recipeId)
    }

    override fun onNewToken(token: String) {
        Log.d(TAG, "Refreshed token: $token")

    }

    private fun sendNotification(
        title: String,
        messageBody: String,
        filterDiet: String?,
        recipeId: String?
    ) {
        // Verificar si el usuario tiene activadas las notificaciones
        val sharedPrefManager = SharedPrefManager(applicationContext)
        if (!sharedPrefManager.getNotificationStatus()) {
            Log.d(TAG, "Notificaciones desactivadas por el usuario.")
            return
        }

        // Verificar si el usuario aún tiene esta preferencia
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        FirebaseFirestore.getInstance().collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                val user = document.toObject(User::class.java) ?: return@addOnSuccessListener

                when (filterDiet) {
                    "vegan" -> if (!user.vegan) return@addOnSuccessListener
                    "vegetarian" -> if (!user.vegetarian) return@addOnSuccessListener
                    "glutenFree" -> if (!user.glutenFree) return@addOnSuccessListener
                    "dairyFree" -> if (!user.dairyFree) return@addOnSuccessListener
                }

                showActualNotification(title, messageBody, filterDiet, recipeId)
            }
    }


    private fun showActualNotification(
        title: String,
        messageBody: String,
        filterDiet: String?,
        recipeId: String?
    ) {
        val intent = Intent(this, HomeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            if (recipeId != null) {
                putExtra("RECIPE_ID", recipeId) // Clave consistente
            }
            if (filterDiet != null) {
                putExtra("FILTER_DIET", filterDiet) // Pasar el filtro de dieta si existe
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = "smartfreezer_channel"
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.smartfreezer__3_) // Usa tu ícono de notificación
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Crear canal para Android Oreo en adelante
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "SmartFreezer Notificaciones",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Canal para notificaciones de recetas filtradas por dieta"
            }
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }

}