plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}
kotlin { jvmToolchain(21) }
application { mainClass.set("bible.dbgen.MainKt") }
dependencies {
    implementation(libs.sqlite.jdbc)
    implementation(libs.lucene.analysis.common)
    testImplementation(libs.junit)
}
