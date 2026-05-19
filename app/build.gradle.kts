plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.prototipotesis"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.prototipotesis"
        minSdk = 26
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

}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // dependencias necesarias:
    // tensorflow lite (nucleo)
    implementation("org.tensorflow:tensorflow-lite:2.14.0")

    // Soporte GPU
    implementation("org.tensorflow:tensorflow-lite-gpu:2.14.0")
    implementation("org.tensorflow:tensorflow-lite-gpu-api:2.14.0")


    // Soporte para imágenes
    implementation("org.tensorflow:tensorflow-lite-support:0.4.4")

    // CameraX
    val cameraxVersion = "1.3.3"
    implementation("androidx.camera:camera-core:$cameraxVersion")
    implementation("androidx.camera:camera-camera2:$cameraxVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
    implementation("androidx.camera:camera-view:$cameraxVersion")

    // dependencia para el reconocimiento ocr
    implementation("com.google.mlkit:text-recognition:16.0.0")

    implementation ("com.google.android.material:material:1.12.0")
    implementation ("androidx.viewpager2:viewpager2:1.1.0")

     // controles para la reproduccion de videos
    implementation("androidx.media3:media3-exoplayer:1.4.1")
    implementation("androidx.media3:media3-ui:1.4.1")

    // herramienta para manejar imagenes
    implementation("com.github.chrisbanes:PhotoView:2.3.0")

    // opencv para manejar figuras complejas (segmentacion)
    implementation("org.opencv:opencv:4.10.0")

}