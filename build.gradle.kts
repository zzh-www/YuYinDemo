plugins {
    id("com.android.application") version "7.4.2" apply false
    id("com.android.library") version "7.4.2" apply false
    id("org.jetbrains.kotlin.android") version "1.8.20" apply false
}

dependencies {
    // 注册 extractForNativeBuild configurations
    val extractForNativeBuild by configurations.creating
    // 使用 extractForNativeBuild
    extractForNativeBuild("org.pytorch:pytorch_android:1.10.0") {
        isTransitive = false
    }
    extractForNativeBuild("com.github.pengzhendong:wenet-openfst-android:1.0.2") {
        isTransitive = false
    }
}


tasks.withType<Delete> {
    delete(rootProject.buildDir)
}