plugins {
    alias(libs.plugins.fabric.loom)
    id("maven-publish")
}

base {
    archivesName = "zenith-client"
    group = properties["maven_group"] as String

    version = "${providers.gradleProperty("zenith_version").get()}+mc1.21.11"
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
        name = "Terraformers"
        url = uri("https://maven.terraformersmc.com")
    }
    maven {
        name = "ViaVersion"
        url = uri("https://repo.viaversion.com")
    }
    mavenCentral()
    maven("https://maven.xpple.dev/maven2")

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

val modInclude: Configuration by configurations.creating
val jij: Configuration by configurations.creating

configurations {
    // include mods
    modImplementation.configure {
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
    mappings(loom.officialMojangMappings())
    modImplementation(libs.fabric.loader)

    val fapiVersion = libs.versions.fabric.api.get()
    modInclude(fabricApi.module("fabric-api-base", fapiVersion))
    modInclude(fabricApi.module("fabric-resource-loader-v1", fapiVersion))

    // Compat fixes
    modCompileOnly(fabricApi.module("fabric-renderer-indigo", fapiVersion))
    modCompileOnly(libs.sodium) { isTransitive = false }
    modCompileOnly(libs.lithium) { isTransitive = false }
    modCompileOnly(libs.iris) { isTransitive = false }
    modCompileOnly(libs.viafabricplus) { isTransitive = false }
    modCompileOnly(libs.viafabricplus.api) { isTransitive = false }

    modCompileOnly(libs.baritone)
    modCompileOnly(libs.modmenu)

    jij("dev.xpple:cubiomes:0.0.0-2026.6.29.1782739186+b12a5325c8a195fa99711ed2052a4fc9cac077a8") { isTransitive = false }
    implementation("org.slf4j:slf4j-api:2.0.17")
    testImplementation(platform("org.junit:junit-bom:5.13.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // Libraries (JAR-in-JAR)
    jij(libs.orbit)
    jij(libs.starscript)
    jij(libs.discord.ipc)
    jij(libs.reflections)
    jij(libs.netty.handler.proxy) { isTransitive = false }
    jij(libs.netty.codec.socks) { isTransitive = false }
    jij(libs.waybackauthlib)
    jij(libs.minecraft.auth)
}

sourceSets {
    val launcher by creating {
        java {
            srcDir("src/launcher/java")
        }
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25

    if (System.getenv("CI")?.toBoolean() == true) {
        withSourcesJar()
        withJavadocJar()
    }
}

// Handle transitive dependencies for jar-in-jar
// Based on implementation from BaseProject by FlorianMichael/EnZaXD
// Source: https://github.com/FlorianMichael/BaseProject/blob/main/src/main/kotlin/de/florianmichael/baseproject/Fabric.kt
// Licensed under Apache License 2.0
afterEvaluate {
    val jijConfig = configurations.findByName("jij") ?: return@afterEvaluate

    // Dependencies to exclude from jar-in-jar
    val excluded = setOf(
        "org.slf4j",    // Logging provided by Minecraft
        "jsr305"        // Compile time annotations only
    )

    jijConfig.incoming.resolutionResult.allDependencies.forEach { dep ->
        val requested = dep.requested.displayName

        if (excluded.any { requested.contains(it) }) return@forEach

        val compileOnlyDep = dependencies.create(requested) {
            isTransitive = false
        }

        val implDep = dependencies.create(compileOnlyDep)

        dependencies.add("compileOnlyApi", compileOnlyDep)
        dependencies.add("implementation", implDep)
        dependencies.add("include", compileOnlyDep)
    }
}

loom {
    accessWidenerPath = file("src/main/resources/meteor-client.accesswidener")
}

tasks {
    processResources {
        val buildNumber = providers.gradleProperty("build_number").getOrElse("")
        val commit = providers.gradleProperty("commit").getOrElse("")

        val propertyMap = mapOf(
            "version" to project.version,
            "build_number" to buildNumber,
            "commit" to commit,
            "minecraft_version" to libs.versions.minecraft.get(),
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
        from("NOTICE.md")
        from("third-party") { into("META-INF/third-party") }
        inputs.property("archivesName", project.base.archivesName.get())

        from("LICENSE") {
            rename { "${it}_${inputs.properties["archivesName"]}" }
        }

        // Include launcher classes
        from(sourceSets["launcher"].output)

        manifest {
            attributes["Main-Class"] = "meteordevelopment.meteorclient.Main"
        }
    }

    withType<JavaCompile>().configureEach {
        options.compilerArgs.addAll(
            listOf(
                "-Xlint:deprecation",
                "-Xlint:unchecked", "-Xmaxerrs", "300"
            )
        )
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

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifactId = "meteor-client"

            version = "${libs.versions.minecraft.get()}-SNAPSHOT"
        }
    }

    repositories {
        maven("https://maven.meteordev.org/snapshots") {
            name = "meteor-maven"

            credentials {
                username = System.getenv("MAVEN_METEOR_ALIAS")
                password = System.getenv("MAVEN_METEOR_TOKEN")
            }

            authentication {
                create<BasicAuthentication>("basic")
            }
        }
    }
}

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

tasks.withType<Test>().configureEach { useJUnitPlatform() }

val sourceDistribution = tasks.register<Zip>("sourceDistribution") {
    archiveBaseName.set("zenith-client")
    archiveVersion.set(project.version.toString())
    archiveClassifier.set("source-project")
    destinationDirectory.set(layout.buildDirectory.dir("distributions"))
    into("Zenith-Client-1.21.11") {
        from("src") { into("src") }
        from("gradle") { into("gradle") }
        from("third-party") { into("third-party") }
        from("docs") { into("docs") }
        from("examples") { into("examples"); exclude("**/build/**", "**/.gradle/**") }
        from("build.gradle.kts", "settings.gradle.kts", "gradle.properties", "gradlew", "gradlew.bat", "README.md", "NOTICE.md", "LICENSE")
    }
}
tasks.named("build") { dependsOn(sourceDistribution) }

// Optional production-runtime check addon. Never included in the client distribution.
val smokeJar = tasks.register<Jar>("smokeJar") {
    archiveClassifier.set("smoke-dev")
    from(smokeClient.output)
}
tasks.register<net.fabricmc.loom.task.RemapJarTask>("remapSmokeJar") {
    dependsOn(smokeJar)
    inputFile.set(smokeJar.flatMap { it.archiveFile })
    archiveClassifier.set("smoke")
    addNestedDependencies.set(false)
}
