import net.ltgt.gradle.errorprone.errorprone

plugins {
    alias(libs.plugins.fabric.loom)
    alias(libs.plugins.errorprone)
}

val archivesBaseName = providers.gradleProperty("archives_base_name").get()
val mavenGroup = providers.gradleProperty("maven_group").get()
val runErrorProne = providers.gradleProperty("errorprone").isPresent

base {
    archivesName = archivesBaseName
    group = mavenGroup

    version = "${providers.gradleProperty("zenith_version").get()}+mc${libs.versions.minecraft.get()}"
}

repositories {
    maven {
        name = "meteor-maven"
        url = uri("https://maven.meteordev.org/releases")
    }
    maven {
        name = "meteor-maven-snapshots"
        url = uri("https://maven.meteordev.org/snapshots")
    }
    maven {
        name = "ViaVersion"
        url = uri("https://repo.viaversion.com")
    }
    mavenCentral()
    maven {
        name = "xpple"
        url = uri("https://maven.xpple.dev/maven2")
        content { includeGroup("dev.xpple") }
    }

    exclusiveContent {
        forRepository {
            maven {
                name = "modrinth"
                url = uri("https://api.modrinth.com/maven")
            }
        }
        filter {
            includeGroup("maven.modrinth")
        }
    }
}

val modInclude = configurations.create("modInclude")
val jij = configurations.create("jij")
val launcher = sourceSets.create("launcher") {
    java.srcDir("src/launcher/java")
}

configurations {
    // include mods
    implementation.configure {
        extendsFrom(modInclude)
    }
    include.configure {
        extendsFrom(modInclude)
    }

    // include libraries (jar-in-jar)
    implementation.configure {
        extendsFrom(jij)
    }
    include.configure {
        extendsFrom(jij)
    }
}

dependencies {
    // Fabric
    minecraft(libs.minecraft)
    implementation(libs.fabric.loader)
    // Minecraft 26.2 supplies SLF4J 2; Reflections otherwise introduces its old 1.7 API in dev runs.
    implementation("org.slf4j:slf4j-api:2.0.17")

    val fapiVersion = libs.versions.fabric.api.get()
    modInclude(fabricApi.module("fabric-api-base", fapiVersion))
    modInclude(fabricApi.module("fabric-resource-loader-v1", fapiVersion))

    // Compat fixes
    compileOnly(fabricApi.module("fabric-renderer-indigo", fapiVersion))
    compileOnly(libs.sodium) { isTransitive = false }
    compileOnly(libs.lithium) { isTransitive = false }
    compileOnly(libs.iris) { isTransitive = false }
    compileOnly(libs.viafabricplus) { isTransitive = false }
    compileOnly(libs.viafabricplus.api) { isTransitive = false }

    compileOnly(libs.baritone)
    compileOnly(libs.modmenu)

    // Libraries (JAR-in-JAR)
    jij(libs.orbit)
    jij(libs.starscript)
    jij(libs.discord.ipc)
    jij(libs.reflections)
    jij(libs.netty.handler.proxy) { isTransitive = false }
    jij(libs.netty.codec.socks) { isTransitive = false }
    jij(libs.waybackauthlib)
    jij(libs.minecraft.auth) {
        exclude("com.google.code.gson")
        exclude("com.google.errorprone")
    }
    // Nora Tweaks world-generation utilities; preserve the replaceable LGPL library JAR.
    jij("dev.xpple:cubiomes:0.0.0-2026.6.29.1782739186+b12a5325c8a195fa99711ed2052a4fc9cac077a8") {
        isTransitive = false
    }

    testImplementation(platform("org.junit:junit-bom:5.13.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // Error Prone
    errorprone(libs.errorprone.core)
    errorprone(libs.nullaway)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(libs.versions.jdk.get().toInt()))
    }

    withSourcesJar()
}

// Handle transitive dependencies for jar-in-jar
// Based on implementation from BaseProject by florianreuth/EnZaXD
// Source: https://github.com/florianreuth/BaseProject/blob/main/src/main/kotlin/de/florianreuth/baseproject/Fabric.kt
// Licensed under Apache License 2.0
val jijExcluded = setOf("org.slf4j", "jsr305")
listOf("api", "implementation", "include").forEach { configName ->
    configurations.named(configName).configure {
        defaultDependencies {
            configurations.getByName("jij").incoming.resolutionResult.allComponents
                .mapNotNull { it.id as? ModuleComponentIdentifier }
                .forEach { id ->
                    val notation = "${id.group}:${id.module}:${id.version}"
                    if (jijExcluded.none { notation.contains(it) }) {
                        add(project.dependencies.create(notation) {
                            isTransitive = false
                        })
                    }
                }
        }
    }
}

loom {
    accessWidenerPath = file("src/main/resources/meteor-client.classtweaker")
}

fun toMinecraftCompat(version: String): String {
    // Stable release
    val stable = Regex("""^(\d{2})\.([1-9]\d*)(?:\.(\d+))?$""")

    stable.matchEntire(version)?.let {
        val (year, drop, _) = it.destructured
        return "~$year.$drop"
    }

    // Prerelease
    val pre = Regex("""^(\d{2})\.([1-9]\d*)-pre[-.](\d+)$""")
    pre.matchEntire(version)?.let {
        return version.replace("-pre-", "-pre.")
    }

    // Release Candidate
    val rc = Regex("""^(\d{2})\.([1-9]\d*)-rc[-.](\d+)$""")
    rc.matchEntire(version)?.let {
        return version.replace("-rc-", "-rc.")
    }

    // fallback
    return version
}

tasks {
    processResources {
        val buildNumber = providers.gradleProperty("build_number").getOrElse("")
        val commit = providers.gradleProperty("commit").getOrElse("")

        val propertyMap = mapOf(
            "version" to project.version,
            "build_number" to buildNumber,
            "commit" to commit,
            "jdk_version" to libs.versions.jdk.get(),
            "minecraft_version" to toMinecraftCompat(libs.versions.minecraft.get()),
            "loader_version" to libs.versions.fabric.loader.get()
        )

        inputs.properties(propertyMap)
        filesMatching("fabric.mod.json") {
            expand(propertyMap)
        }
    }

    // Compile launcher with Java 8 for backwards compatibility
    named<JavaCompile>("compileLauncherJava").configure {
        sourceCompatibility = JavaVersion.VERSION_1_8.toString()
        targetCompatibility = JavaVersion.VERSION_1_8.toString()
        options.compilerArgs.add("-Xlint:-options")
    }

    jar {
        inputs.property("archivesName", archivesBaseName)

        from("LICENSE") {
            rename { "${it}_$archivesBaseName" }
        }
        from("NOTICE.md")
        from("third-party") { into("META-INF/third-party"); exclude("**/*-source.zip", "**/*-sources.jar") }

        // Include launcher classes
        from(launcher.output)

        manifest {
            attributes["Main-Class"] = "meteordevelopment.meteorclient.Main"
        }
    }

    withType<JavaCompile>().configureEach {
        options.compilerArgs.addAll(
            listOf(
                "-Xlint:deprecation",
                "-Xlint:unchecked"
            )
        )

        options.errorprone.enabled.set(runErrorProne)

        if (runErrorProne) {
            options.errorprone {
                check("NullAway", net.ltgt.gradle.errorprone.CheckSeverity.ERROR)
                option("NullAway:AnnotatedPackages", "meteordevelopment.meteorclient")
                option("NullAway:JSpecifyMode", "true")
                // Event handlers are discovered reflectively by Orbit.
                option("UnusedMethod:ExcludedAnnotations", "meteordevelopment.orbit.EventHandler")
            }
        }
    }

    javadoc {
        with(options as StandardJavadocDocletOptions) {
            addStringOption("Xdoclint:none", "-quiet")
            addStringOption("encoding", "UTF-8")
            addStringOption("charSet", "UTF-8")
        }
    }

    build {
        if (System.getenv("CI")?.toBoolean() == true) {
            dependsOn("javadocJar")
        }
    }
}


tasks.test { useJUnitPlatform() }
tasks.named<Jar>("sourcesJar") {
    from("LICENSE", "NOTICE.md", "README.md")
    from("third-party") { into("third-party") }
}

// A complete buildable source archive accompanies the GPL distribution.
val sourceDistribution = tasks.register<Zip>("sourceDistribution") {
    archiveBaseName.set("zenith-client")
    archiveVersion.set(project.version.toString())
    archiveClassifier.set("source-project")
    destinationDirectory.set(layout.buildDirectory.dir("distributions"))
    into("Zenith-Client-2.0") {
        from("src") { into("src") }
        from("gradle") { into("gradle") }
        from("scripts") { into("scripts") }
        from("docs") { into("docs") }
        from("third-party") { into("third-party") }
        from("examples") { into("examples"); exclude("**/build/**", "**/.gradle/**") }
        from("build.gradle.kts", "settings.gradle.kts", "gradle.properties", "gradlew", "gradlew.bat",
            "LICENSE", "NOTICE.md", "README.md", ".gitignore", ".editorconfig")
    }
}
tasks.named("build") { dependsOn(sourceDistribution) }

// Optional graphical integration harness; excluded from the distributable JAR.
val smokeClient by sourceSets.creating
configurations[smokeClient.implementationConfigurationName].extendsFrom(configurations.implementation.get())
configurations[smokeClient.runtimeOnlyConfigurationName].extendsFrom(configurations.runtimeOnly.get())
smokeClient.compileClasspath += sourceSets.main.get().compileClasspath + sourceSets.main.get().output
smokeClient.runtimeClasspath += sourceSets.main.get().runtimeClasspath + sourceSets.main.get().output
loom {
    mods {
        maybeCreate("meteor-client").sourceSet(sourceSets.main.get())
        if (providers.gradleProperty("zenithSmoke").isPresent) {
            register("zenith-smoke") { sourceSet(smokeClient) }
        }
    }
    runs {
        register("smokeClient") {
            client()
            source(smokeClient)
            runDir("run-smoke")
        }
    }
}
