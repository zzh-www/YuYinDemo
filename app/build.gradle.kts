plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.kapt")
}

android {
    namespace = "com.yuyin.demo"

    defaultConfig {
        applicationId = "com.yuyin.demo"
        versionCode = 1
        versionName = "1.0"
        minSdk = 33
        targetSdk = 33
        compileSdk = 33
        buildToolsVersion = "30.0.3"

        viewBinding {
            enable = true
        }

        signingConfigs {
            this.maybeCreate("release").run {
                storeFile = file("yuyin.keystore")
                storePassword = "yuyindemo"
                keyAlias = "yuyin"
                keyPassword = "yuyindemo"
            }
        }

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        externalNativeBuild {
            cmake {
                targets("wenet", "decoder_main")
                cppFlags(
                    "-std=c++14",
                    "-DC10_USE_GLOG",
                    "-DC10_USE_MINIMAL_GLOG",
                    "-DANDROID",
                    "-Wno-c++11-narrowing",
                    "-fexceptions",
                    "-DANDROID_STL=c++_shared"
                )
            }
        }

        ndkVersion = "21.1.6352462"
        ndk {
//            abiFilters.add("armeabi-v7a")
            abiFilters.add("arm64-v8a")
//            abiFilters.add("x86")
//            abiFilters.add("x86_64")
        }

        packagingOptions {
            jniLibs.pickFirsts.add("lib/arm64-v8a/libc++_shared.so") // 增加其他架构支持需要添加pickFirst 选项
        }
    }

    testOptions {
        execution = "ANDROIDX_TEST_ORCHESTRATOR"
    }


    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlin {
        jvmToolchain(8)
    }

    viewBinding { enable = true }
}

dependencies {

    // Required -- JUnit 4 framework
    testImplementation("junit:junit:4.13.2")
    // Optional -- Robolectric environment
    androidTestImplementation("androidx.test:core:1.5.0")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("androidx.test:rules:1.5.0")
    androidTestUtil("androidx.test:orchestrator:1.4.2")
    // Optional -- Mockito framework
    testImplementation("org.mockito:mockito-core:1.10.19")
    // Optional -- Hamcrest library
    androidTestImplementation("org.hamcrest:hamcrest-library:2.2")
    // Optional -- UI testing with Espresso
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    // Optional -- UI testing with UI Automator
    androidTestImplementation("androidx.test.uiautomator:uiautomator:2.2.0")


    implementation("androidx.core:core-ktx:1.10.0")
    // Kotlin
    implementation("androidx.navigation:navigation-fragment-ktx:2.5.3")
    implementation("androidx.navigation:navigation-ui-ktx:2.5.3")

    // Feature module Support
    implementation("androidx.navigation:navigation-dynamic-features-fragment:2.5.3")


    // ViewModelScope
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.1")

    // 导入activity与fragment扩展语法
    implementation("androidx.activity:activity-ktx:1.7.0")
    implementation("androidx.fragment:fragment-ktx:1.5.6")

    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.8.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    implementation("androidx.recyclerview:recyclerview:1.3.0")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("androidx.legacy:legacy-support-v4:1.0.0")
    // 协程
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")
    implementation("com.squareup.moshi:moshi-kotlin:1.14.0")
    kapt("com.squareup.moshi:moshi-kotlin-codegen:1.14.0")
    kaptTest("com.squareup.moshi:moshi-kotlin-codegen:1.14.0")
    implementation("com.github.princekin-f:EasyFloat:2.0.4") {
        isTransitive = false
    }
    implementation("pub.devrel:easypermissions:3.0.0") {
        isTransitive = false
    }
    // 注册 extractForNativeBuild configurations
    val extractForNativeBuild by configurations.creating

    implementation("org.pytorch:pytorch_android:1.10.0") {
        isTransitive = false
    }
    implementation("com.github.pengzhendong:wenet-openfst-android:1.0.2") {
        isTransitive = false
    }
    // 使用 extractForNativeBuild 这只是为了可以顺利编译wennet
    extractForNativeBuild("org.pytorch:pytorch_android:1.10.0") {
        isTransitive = false
    }
    extractForNativeBuild("com.github.pengzhendong:wenet-openfst-android:1.0.2") {
        isTransitive = false
    }
    // xlog 框架
    implementation("com.elvishew:xlog:1.11.0") {
        isTransitive = false
    }
}

// 解压native包具体实现 extractForNativeBuild
project.afterEvaluate {
    val files = configurations["extractForNativeBuild"].files
    files.forEach { println(it.name) }
    files.filter { it.name.contains("pytorch_android") or it.name.contains("wenet-openfst-android") }
        .forEach {
            val file = it.absoluteFile
            copy {
                from(zipTree(file))
                into("${buildDir}/${file.name}")
                include("headers/**", "jni/**")
            }
            println("extract: " + it.name)
            println("dir: $buildDir")
        }
}