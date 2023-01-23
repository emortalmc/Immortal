
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.7.22"
    kotlin("plugin.serialization") version "1.7.22"
    id("com.github.johnrengelman.shadow") version "7.1.2"

    `maven-publish`
    java
}

repositories {
    mavenCentral()

    maven("https://jitpack.io")
}

dependencies {
    implementation("com.github.Minestom:Minestom:eb06ba8664")
    testImplementation("com.github.Minestom:Minestom:eb06ba8664")

    // Util
//    api("com.github.EmortalMC:KStom:50b2b882fa")
    api("com.github.emortaldev:Particable:f7212f39fb")
    api("com.github.EmortalMC:Rayfast:684e854a48")
    api("net.kyori:adventure-text-minimessage:4.12.0")

    // Logging
    implementation("org.jline:jline:3.21.0")
    implementation("org.jline:jline-terminal-jansi:3.21.0")
    api("org.tinylog:tinylog-api-kotlin:2.6.0")
    implementation("org.tinylog:tinylog-impl:2.5.0")

    // DB
    api("redis.clients:jedis:4.3.1")
    api("org.litote.kmongo:kmongo-coroutine-serialization:4.8.0")
    api("org.litote.kmongo:kmongo-id:4.8.0")

    // Kotlin
    testImplementation(kotlin("test"))
    compileOnly("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.6.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))
}

tasks {

    named<ShadowJar>("shadowJar") {
        archiveBaseName.set("immortal")

        mergeServiceFiles()
        //minimize()
        dependencies {
            //exclude(dependency("com.github.emortaldev:Particable"))
            //exclude(dependency("com.github.emortaldev:Kstom"))
        }
    }

    build {
        dependsOn(shadowJar)
    }

    test {
        useJUnitPlatform()
    }
}

val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions.jvmTarget = JavaVersion.VERSION_17.toString()

compileKotlin.kotlinOptions {
    freeCompilerArgs = listOf("-Xinline-classes")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = project.properties["group"] as? String?
            artifactId = project.name
            version = project.properties["version"] as? String?

            from(components["java"])
        }
    }
}