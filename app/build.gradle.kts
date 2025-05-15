// Archivo: app/build.gradle.kts

plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services") // Asegúrate de que esta línea esté en el archivo app/build.gradle.kts
}

android {
    namespace = "com.example.acortadorurlapp" // Reemplaza con tu namespace real
    compileSdk = 35 // O la versión de SDK que estés usando

    defaultConfig {
        applicationId = "com.example.acortadorurlapp" // Reemplaza con tu ID de aplicación real
        minSdk = 24 // O tu minSdk
        targetSdk = 34 // O tu targetSdk
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    // --- Dependencias estándar de AndroidX y Material Design ---
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")

    // --- Firebase Bill of Materials (BOM) ---
    // IMPORTANTE: Asegúrate de que esta sea la última versión estable del BOM.
    // Verifica aquí: https://firebase.google.com/docs/android/setup#available-libraries
    implementation(platform("com.google.firebase:firebase-bom:32.7.4")) // <-- ¡Verifica y actualiza si hay una más nueva!

    // --- Dependencias de Firebase (sin especificar versión, el BOM lo hace) ---
    // Nota la sintaxis con paréntesis y comillas dobles
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    // Solo si realmente necesitas la Realtime Database y la estás usando en tu código:
    implementation("com.google.firebase:firebase-database")

    // --- Google Play Services (Auth) ---
    implementation("com.google.android.gms:play-services-auth:20.7.0")

    // --- Retrofit
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    implementation(libs.credentials)
    implementation(libs.googleid)

}