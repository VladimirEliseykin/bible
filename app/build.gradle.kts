import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.baselineprofile)
}
evaluationDependsOn(":tools:dbgen")
android {
    namespace = "bible.app"
    compileSdk = 36
    defaultConfig {
        applicationId = "bible.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    val releaseKeystore = rootProject.file("keystore.properties")
    signingConfigs {
        if (releaseKeystore.exists()) {
            val props = Properties().apply { releaseKeystore.inputStream().use { load(it) } }
            create("release") {
                storeFile = file(props.getProperty("storeFile"))
                storePassword = props.getProperty("storePassword")
                keyAlias = props.getProperty("keyAlias")
                keyPassword = props.getProperty("keyPassword")
            }
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            // Свой ключ из keystore.properties (не в git); без него — debug-подпись для локальной проверки.
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures { compose = true }
}
kotlin { jvmToolchain(21) }
dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.navigation.compose)
    implementation(libs.datastore.preferences)
    implementation(libs.coroutines.android)
    implementation(libs.profileinstaller)
    baselineProfile(project(":baselineprofile"))
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)
    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.coroutines.test)
}

val bibleDbDir = layout.buildDirectory.dir("generated/bibleDb")
val generateBibleDb by tasks.registering(JavaExec::class) {
    val dbgen = project(":tools:dbgen")
    classpath = dbgen.extensions.getByType<SourceSetContainer>()["main"].runtimeClasspath
    mainClass.set("bible.dbgen.MainKt")
    val src = rootProject.layout.projectDirectory.dir("tools/dbgen/source/rst78")
    inputs.dir(src)
    inputs.files(classpath)
    outputs.dir(bibleDbDir)
    args(src.asFile.path, bibleDbDir.get().file("bible.db").asFile.path)
}
android.sourceSets["main"].assets.srcDir(bibleDbDir)
tasks.named("preBuild") { dependsOn(generateBibleDb) }
