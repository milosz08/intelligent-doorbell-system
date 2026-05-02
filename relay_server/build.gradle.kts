import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    alias(libs.plugins.java)
    alias(libs.plugins.shadow)
    alias(libs.plugins.application)
}

group = "pl.miloszgilga"
version = getEnv("VERSION", "latest")

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://www.jitpack.io") // for moquette broker
    }
}

dependencies {
    implementation(libs.gson)
    implementation(libs.jmdns)
    implementation(libs.logback.classic)
    implementation(libs.moquette.broker)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform)
    testImplementation(libs.mockito)
    testImplementation(libs.mockito.jupyter)
    testImplementation(libs.mqtt.paho)
}

application {
    mainClass.set("pl.miloszgilga.ids.RelayServerApplication")
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events(TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.FAILED)
        showStandardStreams = true
    }
    // suppress JDK 21+ warnings regarding dynamic agent loading (used by mockito)
    // -Xshare:off: disables class data sharing
    jvmArgs("-XX:+EnableDynamicAgentLoading", "-Xshare:off")
}

tasks.withType<ShadowJar> {
    archiveFileName.set("${project.name}.jar")
    destinationDirectory.set(layout.projectDirectory.dir(".bin"))
    manifest {
        attributes(
            mapOf("Main-Class" to "pl.miloszgilga.ids.RelayServerApplication")
        )
    }
}

fun getEnv(name: String, defValue: String = ""): String {
    return System.getenv("RELAY_SERVER_$name") ?: defValue
}
