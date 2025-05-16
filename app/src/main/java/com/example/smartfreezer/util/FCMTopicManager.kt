
package com.example.smartfreezer.util

import com.google.firebase.messaging.FirebaseMessaging
import com.example.smartfreezer.models.User
import timber.log.Timber


object FCMTopicManager {

    // Suscribe al usuario según sus preferencias
    fun subscribeToDietTopics(user: User) {
        unsubscribeFromAllDietTopics() // Primero nos aseguramos de desuscribir de todo

        if (user.vegan) {
            FirebaseMessaging.getInstance().subscribeToTopic("vegan")
                .addOnCompleteListener { task ->
                    Timber.d("Subscribed to vegan topic: ${task.isSuccessful}")
                }
        }
        if (user.vegetarian) {
            FirebaseMessaging.getInstance().subscribeToTopic("vegetarian")
                .addOnCompleteListener { task ->
                    Timber.d("Subscribed to vegetarian topic: ${task.isSuccessful}")
                }
        }
        if (user.glutenFree) {
            FirebaseMessaging.getInstance().subscribeToTopic("glutenFree")
                .addOnCompleteListener { task ->
                    Timber.d("Subscribed to glutenFree topic: ${task.isSuccessful}")
                }
        }
        if (user.dairyFree) {
            FirebaseMessaging.getInstance().subscribeToTopic("dairyFree")
                .addOnCompleteListener { task ->
                    Timber.d("Subscribed to dairyFree topic: ${task.isSuccessful}")
                }
        }
    }

    // Desuscribe de todos los topics dietéticos
    fun unsubscribeFromAllDietTopics() {
        val topics = listOf("vegan", "vegetarian", "glutenFree", "dairyFree")
        topics.forEach { topic ->
            FirebaseMessaging.getInstance().unsubscribeFromTopic(topic)
                .addOnCompleteListener { task ->
                    Timber.d("Unsubscribed from $topic: ${task.isSuccessful}")
                }
        }
    }
}