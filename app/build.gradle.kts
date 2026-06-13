import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { localProperties.load(it) }
}

android {
    namespace = "com.thingspath"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.thingspath"
        minSdk = 24
        targetSdk = 35
        versionCode = 2
        versionName = "1.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        buildConfigField("String", "R2_ACCOUNT_ID", "\"${localProperties.getProperty("R2_ACCOUNT_ID", "")}\"")
        buildConfigField("String", "R2_ACCESS_KEY_ID", "\"${localProperties.getProperty("R2_ACCESS_KEY_ID", "")}\"")
        buildConfigField("String", "R2_SECRET_ACCESS_KEY", "\"${localProperties.getProperty("R2_SECRET_ACCESS_KEY", "")}\"")
        buildConfigField("String", "R2_BUCKET_NAME", "\"${localProperties.getProperty("R2_BUCKET_NAME", "")}\"")
        buildConfigField("String", "R2_PUBLIC_URL", "\"${localProperties.getProperty("R2_PUBLIC_URL", "")}\"")
        buildConfigField("String", "D1_ACCOUNT_ID", "\"${localProperties.getProperty("D1_ACCOUNT_ID", "")}\"")
        buildConfigField("String", "D1_DATABASE_ID", "\"${localProperties.getProperty("D1_DATABASE_ID", "")}\"")
        buildConfigField("String", "D1_API_TOKEN", "\"${localProperties.getProperty("D1_API_TOKEN", "")}\"")
    }

    // Release signing: read keystore from local.properties (local dev) OR
    // environment variables (CI), falling back to debug signing when neither
    // is present so fresh clones can still build.
    // Why: previously release reused debug signing — every release build shared
    // the debug certificate, which can't be used for Play Store uploads and
    // triggers "signature conflict" warnings on overwrite installs.
    fun signingProp(key: String): String =
        System.getenv(key)?.takeIf { it.isNotEmpty() }
            ?: localProperties.getProperty(key, "")

    val releaseSigningConfig = run {
        val keystorePath = signingProp("KEYSTORE_PATH")
        val storePassword = signingProp("KEYSTORE_PASSWORD")
        val keyAlias = signingProp("KEY_ALIAS")
        val keyPassword = signingProp("KEY_PASSWORD")
        if (keystorePath.isNotEmpty() && file(keystorePath).exists()) {
            signingConfigs.create("release") {
                storeFile = file(keystorePath)
                this.storePassword = storePassword
                this.keyAlias = keyAlias
                this.keyPassword = keyPassword
            }
        } else {
            null
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = releaseSigningConfig ?: signingConfigs.getByName("debug")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    applicationVariants.all {
        outputs.all {
            (this as com.android.build.gradle.internal.api.BaseVariantOutputImpl).outputFileName = "thingspath.apk"
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
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    testOptions {
        unitTests {
            all {
                it.jvmArgs = (it.jvmArgs ?: emptyList()) + listOf(
                    "--add-opens=java.base/java.lang.invoke=ALL-UNNAMED",
                    "--add-opens=java.base/java.lang.reflect=ALL-UNNAMED",
                    "--add-opens=java.base/java.lang=ALL-UNNAMED",
                    "--add-opens=java.base/sun.misc=ALL-UNNAMED"
                )
            }
        }
    }

    lint {
        // AGP 8.7 lintVitalAnalyze crashes with Kotlin 2.1 (internal bug)
        checkReleaseBuilds = false
        abortOnError = false
    }
}

dependencies {
    // Core Android
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.0")
    implementation("androidx.activity:activity-compose:1.10.1")

    // Jetpack Compose
    implementation(platform("androidx.compose:compose-bom:2025.05.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.9.0")

    // Hilt/Dagger - Dependency Injection
    implementation("com.google.dagger:hilt-android:2.55")
    ksp("com.google.dagger:hilt-compiler:2.55")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // Coil for image loading
    implementation("io.coil-kt:coil-compose:2.7.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")

    // ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.0")

    // Gson
    implementation("com.google.code.gson:gson:2.12.1")

    // Retrofit
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Room - Local SQLite database
    implementation("androidx.room:room-runtime:2.7.1")
    implementation("androidx.room:room-ktx:2.7.1")
    ksp("androidx.room:room-compiler:2.7.1")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.1.4")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    testImplementation("io.mockk:mockk:1.14.9")
    testImplementation("app.cash.turbine:turbine:1.2.1")
    testImplementation("androidx.test:core:1.6.1")
    testImplementation("androidx.test.ext:junit:1.2.1")
    testImplementation("org.robolectric:robolectric:4.14.1")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2025.05.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
