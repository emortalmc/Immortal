
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.jetbrains.kotlin.jvm") version "1.6.10"
    kotlin("plugin.serialization") version "1.6.10"
    id("com.github.johnrengelman.shadow") version "7.1.2"

    `maven-publish`
    java
}

repositories {
    mavenCentral()

    maven(url = "https://jitpack.io")
    maven(url = "https://repo.spongepowered.org/maven")
}

dependencies {
    //compileOnly(kotlin("stdlib"))
    //compileOnly(kotlin("reflect"))

    compileOnly("net.luckperms:api:5.4")

    compileOnly("com.github.Minestom:Minestom:5469fef417")
    compileOnly("com.github.EmortalMC:Acquaintance:6987f0b3f2")
    api("com.github.emortaldev:Kstom:08da0d2c49")
    api("com.github.emortaldev:Particable:fadfbe0213")

    api("com.github.EmortalMC:Rayfast:ad94541f92")

    compileOnly("org.tinylog:tinylog-api-kotlin:2.4.1")
    compileOnly("org.redisson:redisson:3.17.0")
    compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.0")
    compileOnly("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.2")
}

tasks {
    processResources {
        filesMatching("extension.json") {
            expand(project.properties)
        }
    }

    named<ShadowJar>("shadowJar") {
        archiveBaseName.set(project.name)
        mergeServiceFiles()
        //minimize()
        dependencies {
            //exclude(dependency("com.github.emortaldev:Particable"))
            //exclude(dependency("com.github.emortaldev:Kstom"))
        }
    }

    build { dependsOn(shadowJar) }
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