import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    alias(libs.plugins.java)
    alias(libs.plugins.shadow)
}

group = "pl.miloszgilga"
version = getEnv("VERSION", "latest")

repositories {
    mavenCentral()
    maven {
        url = uri("https://www.jitpack.io") // for moquette broker
    }
}

dependencies {
    implementation(libs.jmdns)
    implementation(libs.logback.classic)
    implementation(libs.moquette.broker)
}

tasks.withType<ShadowJar> {
    archiveFileName.set("${project.name}.jar")
    destinationDirectory.set(layout.projectDirectory.dir(".bin"))
    manifest {
        attributes(
            mapOf("Main-Class" to "pl.miloszgilga.RelayServerApplication")
        )
    }
}

fun getEnv(name: String, defValue: String = ""): String {
    return System.getenv("RELAY_SERVER_$name") ?: defValue
}
