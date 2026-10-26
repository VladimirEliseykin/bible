plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    application
}
kotlin { jvmToolchain(21) }
application { mainClass.set("bible.commentary.tool.MainKt") }
dependencies {
    implementation(project(":shared:commentary"))
    implementation(libs.sqlite.jdbc)
    testImplementation(libs.junit)
}

/** Собирает пакет объяснений в build/pack: ./gradlew :tools:commentary:buildPack -PpackVersion=1 */
val buildPack by tasks.registering(JavaExec::class) {
    dependsOn(":app:generateBibleDb")
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("bible.commentary.tool.MainKt")
    val root = rootProject.layout.projectDirectory
    args(
        root.file("app/build/generated/bibleDb/bible.db").asFile.path,
        layout.projectDirectory.dir("notes").asFile.path,
        layout.projectDirectory.file("sources.json").asFile.path,
        layout.projectDirectory.file("sources/CSlElizabeth-CS.json").asFile.path,
        layout.buildDirectory.dir("pack").get().asFile.path,
        (project.findProperty("packVersion") ?: "1").toString(),
    )
}
