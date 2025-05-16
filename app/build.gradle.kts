plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
    id("androidx.navigation.safeargs.kotlin")
    id("com.google.dagger.hilt.android")
    id ("kotlin-kapt")
    id ("kotlin-parcelize")
}

android {
    namespace = "com.example.smartfreezer"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.smartfreezer"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        viewBinding = true
        dataBinding = true
    }
}

dependencies {
    implementation("com.google.code.gson:gson:2.10.1")

    // Importa la BoM de Firebase para gestionar versiones automáticamente
    implementation(platform("com.google.firebase:firebase-bom:33.13.0"))

    // Dependencia de Firebase Crashlytics
    implementation ("com.google.firebase:firebase-crashlytics-ktx")

    //Firebase Analytics para obtener más contexto en los informes de Crashlytics
    implementation ("com.google.firebase:firebase-analytics-ktx")

    // Servicios de Firebase (autenticación y Firestore)
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation ("com.google.firebase:firebase-messaging:24.1.1")

    // Also add the dependencies for the Credential Manager libraries and specify their versions
    implementation("androidx.credentials:credentials:1.5.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.5.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
    //Menu SmoothBottomBar
    implementation("com.github.ibrahimsn98:SmoothBottomBar:1.7.9")
    //Menu desplegable PowerSpinner
    implementation ("com.github.skydoves:powerspinner:1.2.7")
    implementation ("com.github.skydoves:powermenu:2.2.4")
    implementation("com.google.android.material:material:1.12.0")
    // Retrofit
    implementation ("com.squareup.retrofit2:retrofit:2.9.0")
    implementation ("com.squareup.retrofit2:converter-gson:2.9.0")
    // ViewModel + LiveData + Hilt
    implementation ("androidx.lifecycle:lifecycle-viewmodel-ktx")
    implementation ("androidx.lifecycle:lifecycle-livedata-ktx")
    implementation ("androidx.lifecycle:lifecycle-extensions:2.2.0")
    implementation ("androidx.hilt:hilt-common:1.2.0")
    implementation ("androidx.lifecycle:lifecycle-viewmodel-ktx:2.9.0")

    //Grafico
    implementation ("com.github.PhilJay:MPAndroidChart:v3.1.0")

    // Glide para imágenes
    implementation ("com.github.bumptech.glide:glide:4.12.0")
    implementation ("com.github.bumptech.glide:compiler:4.12.0")

    implementation("com.squareup.okhttp3:okhttp:4.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    // Hilt
    implementation("com.google.dagger:hilt-android:2.56.1")
    implementation(libs.firebase.datatransport)
    kapt ("com.google.dagger:hilt-android-compiler:2.56.1")
    //Cargar imagen
    implementation("io.coil-kt:coil:2.4.0")
    //Parsear html
    implementation("org.jsoup:jsoup:1.16.1")

    //Shimmer
    implementation("com.facebook.shimmer:shimmer:0.5.0")
    //Swipe Refresh Layout
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    //Rounded Image
    implementation ("com.makeramen:roundedimageview:2.3.0")
    //Palette
    implementation("androidx.palette:palette-ktx:1.0.0")

    implementation ("org.pytorch:pytorch_android:1.12.1")
    implementation ("org.pytorch:pytorch_android_torchvision:1.12.1")

    //Timber
    implementation ("com.jakewharton.timber:timber:5.0.1")

    implementation ("com.microsoft.onnxruntime:onnxruntime-android:1.15.1")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.material)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.recyclerview)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}