import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.google.android.libraries.mapsplatform.secrets.gradle.plugin)
    alias(libs.plugins.aboutlibraries.plugin)
    alias(libs.plugins.google.services.plugin)
    alias(libs.plugins.crashlytics.plugin)
    alias(libs.plugins.perf.plugin)
}

android {
    namespace = "com.rpmstudio.texttospeech"
    compileSdk = 35
    ndkVersion = "28.0.13004108"
    val appName = "AI Voiceover"

    defaultConfig {
        applicationId = "com.rpmstudio.texttospeech"
        minSdk = 24
        targetSdk = 35
        versionCode = 250416036
        versionName = "aivo-1.0.36"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        setProperty("archivesBaseName", "$appName $versionName (Build $versionCode)")
        buildConfigField("String", "BUILD_DATE", "\"${getBuildDate()}\"")
        buildConfigField("String", "APP_KEY", "\"748fb952e70f11c38d71534b881b4308a10b66468ee1b5d8\"")
    }

    buildTypes {
        release {
            buildConfigField("String", "BUILD_DATE", "\"${getBuildDate()}\"")
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"
            )
            ndk.debugSymbolLevel = "FULL"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_19
        targetCompatibility = JavaVersion.VERSION_19
    }
    kotlinOptions {
        jvmTarget = "19"
    }
    buildFeatures {
        buildConfig = true
        viewBinding = true
        compose = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

fun getBuildDate(): String {
    val date = Date.from(Calendar.getInstance().toInstant())
    val formatter = SimpleDateFormat("dd.MM.yyy HH:mm")
    return formatter.format(date)
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.generativeai)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.legacy.support.v4)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.preference)
    implementation(libs.review.ktx)
    implementation(libs.app.update.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation(libs.gson)
//    implementation(libs.okhttp3)
//    implementation(libs.io.coil)
    implementation(libs.io.coil.compose)
    implementation(libs.io.coil.network.okhttp)
    implementation(libs.keyboardvisibility)

    implementation(libs.androidx.media)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.dotsindicator)
    implementation(libs.taptargetview)
    implementation(libs.ratingdialog)
    implementation(libs.lottie)

    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.materialIconExtended)
    implementation(libs.androidx.material)
    implementation(libs.androidx.material3)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    androidTestImplementation(libs.androidx.ui.test.junit4)
    implementation(libs.androidx.activity.compose)

    implementation(libs.aboutlibraries)
    implementation(libs.aboutlibraries.core)
    implementation(libs.aboutlibraries.compose.core)
    implementation(libs.aboutlibraries.compose.m3)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.appcheck)
    implementation(libs.firebase.perf)
    implementation(libs.firebase.messaging)

    implementation(libs.appodeal)
    implementation(libs.play.services.ads)
    implementation(libs.androidx.lifecycle.process)

    implementation (libs.integrity)

}

