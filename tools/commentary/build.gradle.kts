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
