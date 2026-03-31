import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("java")
    alias(libs.plugins.shadow)
}

group = "pl.miloszgilga"
version = getEnv("VERSION", "latest")

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.logback.classic)
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
