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
    //compileOnly(kotlin("stdlib"))
    //compileOnly(kotlin("reflect"))

    compileOnly("net.luckperms:api:5.4")

    compileOnly("com.github.Minestom:Minestom:1a013728fd")
    testImplementation("com.github.Minestom:Minestom:1a013728fd")

//    compileOnly("com.github.EmortalMC:Acquaintance:6987f0b3f2")
    api("com.github.EmortalMC:KStom:50b2b882fa")
    api("com.github.emortaldev:Particable:f7212f39fb")

    api("com.github.EmortalMC:Rayfast:684e854a48")

    api("org.tinylog:tinylog-api-kotlin:2.5.0")
    compileOnly("redis.clients:jedis:4.3.1")
    compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    compileOnly("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")


    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
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
            exclude(dependency("com.tinylog:tinylog-api-kotlin"))
            //exclude(dependency("com.github.emortaldev:Particable"))
            //exclude(dependency("com.github.emortaldev:Kstom"))
        }
    }

    build { dependsOn(shadowJar) }

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