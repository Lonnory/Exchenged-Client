import com.android.build.api.variant.FilterConfiguration.FilterType.*
import java.util.Properties
import java.io.ByteArrayOutputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.ksp)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.exchenged.client"
    compileSdk = 36
    ndkVersion = "27.0.12077973"

    defaultConfig {
        val VERSION_NAME:String by project
        val VERSION_CODE:String by project
        applicationId = "com.exchenged.client"
        minSdk = 28
        targetSdk = 36
        versionCode = VERSION_CODE.toInt()
        versionName = VERSION_NAME

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    base {
        archivesName.set("ExchengedClient")
    }
    signingConfigs {
        create("release") {
            val keystoreFile = project.file("xrayfa.jks")
            if(keystoreFile.exists()) {
                storeFile = keystoreFile
                storePassword = System.getenv("KEYSTORE_PASSWORD") ?: ""
                keyAlias = System.getenv("KEY_ALIAS") ?: ""
                keyPassword = System.getenv("KEY_PASSWORD") ?: ""
            }else {
                println("keystore file not found , building unsigned release apk")
            }
        }
    }

    //Remove DependencyInfoBlock for F-Droid
    dependenciesInfo {
        // Disables dependency metadata when building APKs.
        includeInApk = false
        // Disables dependency metadata when building Android App Bundles.
        includeInBundle = false
    }

    buildTypes {
        release {
            val keystoreFile = project.file("xrayfa.jks")
            if (keystoreFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
            isDebuggable = false
            isJniDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    packagingOptions.jniLibs.useLegacyPackaging = true

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }


    splits {
        abi {
            isEnable = true
            reset()
            include("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
            isUniversalApk = true
        }
    }

    val abiCodes = mapOf("armeabi-v7a" to 1, "arm64-v8a" to 2, "x86" to 3, "x86_64" to 4)

    androidComponents  {
        onVariants { variant ->
            variant.outputs.forEach { output->
                val name = output.filters.find { it.filterType == ABI } ?.identifier


                val baseAbiCode = abiCodes[name] ?: 0

                output.versionCode.set((baseAbiCode + 1000 * output.versionCode.get()))
            }
        }
    }
}



val xrayLibDir = rootProject.file("AndroidLibXrayLite")
val aarOutput = xrayLibDir.resolve("libv2ray.aar")

val libsDir = file("libs")

val goExecutable: String by lazy {
    val localProperties = Properties()
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use { stream ->
            localProperties.load(stream)
        }
    }
    localProperties.getProperty("go.executable") ?: "go"
}

fun Exec.setupGoCommand(vararg args: String) {
    val executablePath = goExecutable
    
    val javaHome = System.getProperty("java.home")
    val goPath = System.getenv("GOPATH") ?: "${System.getProperty("user.home")}/go"
    val pathSeparator = File.pathSeparator
    
    val goFile = File(executablePath)
    val goBin = goFile.parentFile?.absolutePath
    val javaBin = File(javaHome, "bin").absolutePath
    val gomobileBin = File(goPath, "bin").absolutePath
    
    val currentPath = System.getenv("PATH") ?: ""
    val paths = mutableListOf<String>()
    if (goBin != null) paths.add(goBin)
    paths.add(javaBin)
    paths.add(gomobileBin)
    paths.add(currentPath)
    
    environment("PATH", paths.filter { it.isNotEmpty() }.joinToString(pathSeparator))
    environment("GOPATH", goPath)

    // Set Android SDK and NDK environment variables
    try {
        val androidExtension = project.extensions.getByType(com.android.build.gradle.BaseExtension::class.java)
        environment("ANDROID_HOME", androidExtension.sdkDirectory.absolutePath)
        environment("ANDROID_NDK_HOME", androidExtension.ndkDirectory.absolutePath)
    } catch (e: Exception) {
        // Fallback or ignore if not available
    }

    commandLine(executablePath, *args)
    
    doFirst {
        try {
            project.exec {
                commandLine(executablePath, "version")
                standardOutput = ByteArrayOutputStream()
                errorOutput = ByteArrayOutputStream()
            }
        } catch (e: Exception) {
            throw GradleException(
                "Go executable not found: '$executablePath'. \n" +
                "Please install Go 1.21+ (https://go.dev/dl/) and ensure it is in your PATH. \n" +
                "Alternatively, you can specify the path to the go executable in your 'local.properties' file:\n" +
                "go.executable=C:\\\\path\\\\to\\\\go.exe"
            )
        }
    }
}

tasks.register<Exec>("buildGoMobile") {
    workingDir = xrayLibDir
    setupGoCommand("install", "golang.org/x/mobile/cmd/gomobile@latest")
}

tasks.register<Exec>("initGoMobile") {
    dependsOn("buildGoMobile")
    workingDir = xrayLibDir
    setupGoCommand("run", "golang.org/x/mobile/cmd/gomobile@latest", "init")
}
tasks.register<Exec>("goMod") {
    dependsOn("initGoMobile")
    workingDir = xrayLibDir
    setupGoCommand("mod", "tidy", "-v")
}


tasks.register<Exec>("bindXrayLib") {
    dependsOn("goMod")
    workingDir = xrayLibDir
    environment("GOFLAGS", "-buildvcs=false")
    environment("CGO_LDFLAGS", "-Wl,--build-id=none")

    val currentPath = xrayLibDir.absolutePath
    environment("CGO_CFLAGS", "-ffile-prefix-map=$currentPath=.")
    environment("CGO_CXXFLAGS", "-ffile-prefix-map=$currentPath=.")
    setupGoCommand(
        "run", "golang.org/x/mobile/cmd/gomobile@latest",
        "bind",
        "-v",
        "-trimpath",
        "-androidapi", "21",
        "-ldflags", "-s -w",
        "./"
    )
    outputs.file(aarOutput)
}

tasks.register<Copy>("copyXrayLib") {
    dependsOn("bindXrayLib")
    from(aarOutput)
    into(libsDir)
}

tasks.named("preBuild") {
    // personal compile can use it,but at server use script
    dependsOn("copyXrayLib")
}


dependencies {

    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.aar"))))
    implementation(project(":tun2socks"))
    implementation(project(":common"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.compose.adaptive)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.ktx)
    implementation(libs.dagger)
    ksp(libs.dagger.compiler)
    implementation (libs.dagger.android)
    ksp(libs.dagger.android.processor)
    // Zxing
    implementation(libs.zxing.core)
    // CameraX Essential
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.compose)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)

    implementation(libs.gson)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.material3.adaptive.navigation3)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)

    implementation(libs.javax.annotation.api)
    implementation(libs.maxmind.geoip2)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.work.runtime.ktx)

    implementation (libs.okhttp)
    implementation (libs.logging.interceptor)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}