import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.gradle.kotlin.dsl.`kotlin-dsl`

plugins {
    embeddedKotlin("jvm")
    embeddedKotlin("plugin.serialization")
    id("com.github.johnrengelman.shadow") version "8.1.1"
    `java-library`
}

repositories {
    mavenCentral()
    mavenLocal()

    maven("https://jitpack.io")
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    val paperVersion = "1.20-R0.1-SNAPSHOT"
    val rushyApiVersion = "2.1.0"
    val commandApiVersion = "9.1.0"

    compileOnly(kotlin("stdlib"))

    "io.papermc.paper:paper-api:$paperVersion".let {
        compileOnly(it)
        testImplementation(it)
    }

    compileOnly("com.github.Rushyverse:api:$rushyApiVersion")

    // CommandAPI framework
    compileOnly("dev.jorel:commandapi-bukkit-core:$commandApiVersion")
    compileOnly("dev.jorel:commandapi-bukkit-kotlin:$commandApiVersion")

    testImplementation(kotlin("test"))
}

val javaVersion get() = JavaVersion.VERSION_17
val javaVersionString get() = javaVersion.toString()
val javaVersionInt get() = javaVersionString.toInt()

kotlin {
    jvmToolchain(javaVersionInt)

    sourceSets {
        all {
            languageSettings {
                optIn("kotlin.RequiresOptIn")
                optIn("kotlin.ExperimentalStdlibApi")
                optIn("kotlin.contracts.ExperimentalContracts")
                optIn("kotlinx.coroutines.ExperimentalCoroutinesApi")
            }
        }
    }
}

tasks {
    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = javaVersionString
    }

    withType<JavaCompile> {
        sourceCompatibility = javaVersionString
        targetCompatibility = javaVersionString
    }

    test {
        useJUnitPlatform()
    }

    shadowJar {
        archiveClassifier.set("")
    }
}