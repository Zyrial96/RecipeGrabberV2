plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.compose)
}

val releaseSigningNames = listOf("RECIPEGRABBER_KEYSTORE_FILE", "RECIPEGRABBER_STORE_PASSWORD", "RECIPEGRABBER_KEY_ALIAS", "RECIPEGRABBER_KEY_PASSWORD")
val releaseSigningValues = releaseSigningNames.map { providers.environmentVariable(it).orNull }
val hasReleaseSigning = releaseSigningValues.all { !it.isNullOrBlank() }
if ((releaseSigningValues.any { it != null } || providers.gradleProperty("requireReleaseSigning").orNull == "true") && !hasReleaseSigning) {
    throw GradleException("Complete private release signing configuration is required")
}

android {
    namespace = "com.recipegrabber"
    compileSdk = 35
    testBuildType = if (providers.gradleProperty("qaTests").orNull == "true") "audit" else "debug"

    defaultConfig {
        applicationId = "com.recipegrabber"
        minSdk = 31
        targetSdk = 35
        versionCode = 1
        versionName = "2.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
        }
    }

    if (hasReleaseSigning) {
        signingConfigs.create("privateRelease") {
            storeFile = file(releaseSigningValues[0]!!)
            storePassword = releaseSigningValues[1]
            keyAlias = releaseSigningValues[2]
            keyPassword = releaseSigningValues[3]
            storeType = "PKCS12"
        }
    }

    buildTypes {
        create("audit") {
            initWith(getByName("debug"))
            applicationIdSuffix = ".qa"
            versionNameSuffix = "-qa"
            matchingFallbacks += listOf("debug")
        }
        release {
            if (hasReleaseSigning) signingConfig = signingConfigs.getByName("privateRelease")
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        create("releaseCheck") {
            initWith(getByName("release"))
            applicationIdSuffix = ".releasecheck"
            versionNameSuffix = "-releasecheck"
            matchingFallbacks += listOf("release")
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
            excludes += "/META-INF/INDEX.LIST"
            excludes += "/META-INF/DEPENDENCIES"
        }
    }

    testOptions {
        unitTests.all {
            it.useJUnitPlatform()
        }
    }

    sourceSets.getByName("androidTest").assets.srcDir("$projectDir/schemas")
}

configurations.named("auditImplementation") {
    extendsFrom(configurations.getByName("debugImplementation"))
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.material3.adaptive)
    implementation(libs.androidx.material3.adaptive.layout)
    implementation(libs.androidx.material3.adaptive.navigation)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    implementation(libs.androidx.navigation.compose)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.datastore.preferences)

    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.hilt.navigation.compose)

    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    implementation(libs.google.identity.services)

    implementation("com.google.android.gms:play-services-auth:21.2.0")
    implementation("com.google.api-client:google-api-client-android:2.4.1")
    implementation("com.google.apis:google-api-services-drive:v3-rev20240521-2.0.0")

    implementation(libs.coil.compose)

    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")

    testImplementation(libs.junit)
    testImplementation(libs.junit.params)
    testImplementation(libs.mockk)
    testImplementation(libs.coroutines.test)
    testRuntimeOnly(libs.junit.engine)

    testImplementation(libs.androidx.room.runtime)
    testImplementation(libs.androidx.room.ktx)
    kspTest(libs.androidx.room.compiler)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
}
