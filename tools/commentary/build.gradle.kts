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
    implementation(libs.jsoup)
    testImplementation(libs.junit)
}

/**
 * Толкования с bible.optina.ru в corpus/ (в git не хранится):
 * ./gradlew :tools:commentary:fetchOptina -Pbook=Мф -Pcode=new:mf -Pchapters=1-28
 */
val fetchOptina by tasks.registering(JavaExec::class) {
    dependsOn(":app:generateBibleDb")
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("bible.commentary.tool.OptinaKt")
    args(
        rootProject.layout.projectDirectory.file("app/build/generated/bibleDb/bible.db").asFile.path,
        layout.projectDirectory.dir("corpus").asFile.path,
        project.findProperty("book")?.toString() ?: "",
        project.findProperty("code")?.toString() ?: "",
        project.findProperty("chapters")?.toString() ?: "",
    )
}

/** Проверка глав: ./gradlew -q :tools:commentary:checkNotes -Pbook=Мф -Pchapter=5 */
val checkNotes by tasks.registering(JavaExec::class) {
    dependsOn(":app:generateBibleDb")
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("bible.commentary.tool.CheckKt")
    val dir = layout.projectDirectory
    args(
        rootProject.layout.projectDirectory.file("app/build/generated/bibleDb/bible.db").asFile.path,
        dir.dir("notes").asFile.path,
        dir.file("sources.json").asFile.path,
        dir.dir("corpus").asFile.path,
        project.findProperty("book")?.toString() ?: "",
        project.findProperty("chapter")?.toString() ?: "",
    )
}

/** Сохранить страницу в корпус: ./gradlew -q :tools:commentary:savePage -Purl=https://… -Pout=corpus/Мф/005/extra/x.md */
val savePage by tasks.registering(JavaExec::class) {
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("bible.commentary.tool.SavePageKt")
    workingDir = layout.projectDirectory.asFile
    args(project.findProperty("url")?.toString() ?: "", project.findProperty("out")?.toString() ?: "")
}
