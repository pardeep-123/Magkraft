plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    kotlin("kapt")
    id("com.google.devtools.ksp")
    id ("kotlin-parcelize")
}

android {
    namespace = "com.app.magkraft"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.app.magkraft"
        minSdk = 24
        targetSdk = 36
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
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.gridlayout)
//    implementation(libs.litert.gpu)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // CameraX
    implementation ("androidx.camera:camera-core:1.3.1")
    implementation ("androidx.camera:camera-camera2:1.3.1")
    implementation ("androidx.camera:camera-lifecycle:1.3.1")
    implementation ("androidx.camera:camera-view:1.3.1")

    // ML Kit Face Detection
    implementation ("com.google.mlkit:face-detection:16.1.6"){
        exclude(group = "com.google.ai.edge.litert")
    }

    // TensorFlow Lite
//    implementation ("org.tensorflow:tensorflow-lite:2.14.0")
    implementation("org.tensorflow:tensorflow-lite:2.14.0")
   // {
     //   exclude (group= "com.google.ai.edge.litert")
    //}
//    implementation ("org.tensorflow:tensorflow-lite-support:0.4.4")
    implementation("org.tensorflow:tensorflow-lite-gpu:2.14.0")
    //{
      //  exclude(group = "com.google.ai.edge.litert")
   // }
    implementation("org.tensorflow:tensorflow-lite-support:0.4.4")
    //{
      //  exclude(group = "com.google.ai.edge.litert")
   // }
    implementation("org.tensorflow:tensorflow-lite-gpu-api:2.14.0")
    // Room Database
    implementation ("androidx.room:room-runtime:2.6.1")
    ksp ("androidx.room:room-compiler:2.6.1")
    implementation ("androidx.room:room-ktx:2.6.1")

    // Lifecycle
    implementation ("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.4")

    // Coroutines
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // sdp
    implementation(libs.sdp.android)

    // ssp
    implementation("com.intuit.ssp:ssp-android:1.1.1")

    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")

    implementation("com.google.android.material:material:1.12.0")

//co-routine
    implementation("com.jakewharton.retrofit:retrofit2-kotlin-coroutines-adapter:0.9.2")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.0")
//ktx
    implementation("androidx.activity:activity-ktx:1.3.1")
// Lifecycle components
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.3.1")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.3.1")
    implementation("androidx.lifecycle:lifecycle-common-java8:2.3.1")

    implementation("androidx.fragment:fragment-ktx:1.7.1")

    implementation("com.google.guava:guava:31.1-android")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")


}

configurations.all {
    exclude(group = "com.google.ai.edge.litert", module = "litert-api")
    exclude(group = "com.google.ai.edge.litert", module = "litert-runtime")
    exclude(group = "com.google.ai.edge.litert", module = "litert-gpu-api")
}