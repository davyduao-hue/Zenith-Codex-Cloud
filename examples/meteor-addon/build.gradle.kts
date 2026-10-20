import java.util.Properties

plugins {
    id("net.fabricmc.fabric-loom") version "1.17.21"
}

version = "1.0.0"
group = "dev.zenith.example"

repositories {
    maven("https://maven.meteordev.org/releases")
    mavenCentral()
}

val zenithRoot = rootDir.resolve("../..")
val zenithProperties = Properties().apply {
    zenithRoot.resolve("gradle.properties").inputStream().use { load(it) }
}
val zenithJar = providers.gradleProperty("zenithJar").map { file(it) }.getOrElse(
    zenithRoot.resolve("build/libs/zenith-client-${zenithProperties.getProperty("zenith_version")}+mc26.2.jar")
)
check(zenithJar.isFile) { "Build Zenith first with gradlew.bat build, or pass -PzenithJar=<path-to-client.jar>." }

dependencies {
    minecraft("com.mojang:minecraft:26.2")
    implementation("net.fabricmc:fabric-loader:0.19.3")
    compileOnly(files(zenithJar))
    compileOnly("meteordevelopment:orbit:0.2.4")
}

java { toolchain.languageVersion.set(JavaLanguageVersion.of(25)) }
tasks.processResources {
    inputs.property("version",project.version)
    filesMatching("fabric.mod.json") { expand("version" to project.version) }
}
tasks.jar { from(zenithRoot.resolve("LICENSE")) }
