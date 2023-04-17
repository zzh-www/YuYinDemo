plugins {
    id("com.android.application") version "8.0.0" apply false
    id("com.android.library") version "8.0.0" apply false
    id("org.jetbrains.kotlin.android") version "1.8.20" apply false
    id("com.google.devtools.ksp") version "1.8.20-1.0.10" apply false
}


tasks.withType<Delete> {
    delete(rootProject.buildDir)
}