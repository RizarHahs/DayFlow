
import java.util.Properties

        plugins {
            alias(libs.plugins.android.application)
            alias(libs.plugins.kotlin.android)
            alias(libs.plugins.kotlin.compose)
            alias(libs.plugins.kotlin.serialization)
            alias(libs.plugins.ksp)
            alias(libs.plugins.hilt)
        }

// Load release signing credentials
val keystoreProperties = Properties()
val keystorePropertiesFile = rootProject.file("keystore.properties")

require(keystorePropertiesFile.exists()) {
    "keystore.properties file not found!"
}

keystorePropertiesFile.inputStream().use { stream ->
    keystoreProperties.load(stream)
}

// Read signing properties safely
val storeFilePath: String =
    keystoreProperties.getProperty("storeFile")
        ?: error("storeFile is missing from keystore.properties")

val storePasswordValue: String =
    keystoreProperties.getProperty("storePassword")
        ?: error("storePassword is missing from keystore.properties")

val keyAliasValue: String =
    keystoreProperties.getProperty("keyAlias")
        ?: error("keyAlias is missing from keystore.properties")

val keyPasswordValue: String =
    keystoreProperties.getProperty("keyPassword")
        ?: error("keyPassword is missing from keystore.properties")

android {
    namespace = "com.dayflow.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.dayflow.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 6
        versionName = "0.6.0"
    }

    signingConfigs {
        create("release") {
            storeFile = file(storeFilePath)
            storePassword = storePasswordValue
            keyAlias = keyAliasValue
            keyPassword = keyPasswordValue
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }
}

// Room schema configuration
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.icons.extended)
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    ksp(libs.hilt.compiler)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    implementation(libs.datastore.preferences)
    implementation(libs.serialization.json)
    ksp(libs.room.compiler)
    debugImplementation(libs.compose.ui.tooling)
    testImplementation(libs.junit)
}
