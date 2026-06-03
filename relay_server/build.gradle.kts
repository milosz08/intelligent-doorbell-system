import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.tasks.testing.logging.TestLogEvent
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

plugins {
    alias(libs.plugins.java)
    alias(libs.plugins.shadow)
    alias(libs.plugins.application)
    alias(libs.plugins.build.config)
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
    implementation(libs.bcrypt)
    implementation(libs.gson)
    implementation(libs.hikari.cp)
    implementation(libs.jakarta.activation.api)
    implementation(libs.jetty.servlet)
    implementation(libs.jetty.ws.server)
    implementation(libs.jersey.core)
    implementation(libs.jersey.hk2)
    implementation(libs.jersey.gson)
    implementation(libs.jmdns)
    implementation(libs.jul.to.slf4j)
    implementation(libs.logback.classic)
    implementation(libs.moquette.broker)
    implementation(libs.sqlite.jdbc)
    implementation(libs.thymeleaf)
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

tasks.withType<JavaExec> {
    systemProperty("logback.configurationFile", "src/main/resources/logback-dev.xml")
    systemProperty("file.encoding", "UTF-8")
}

buildConfig {
    className("AppBuildConfig")
    packageName("pl.miloszgilga.ids")

    val commitLong = project.findProperty("commitHash")?.toString() ?: "unknown"
    val commitShort = if (commitLong.length >= 7) commitLong.take(7) else commitLong
    
    val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss 'UTC'").withZone(ZoneOffset.UTC)
    val buildDate = formatter.format(Instant.now())

    buildConfigField("String", "COMPILATION_LONG_HASH", "\"$commitLong\"")
    buildConfigField("String", "COMPILATION_HASH", "\"$commitShort\"")
    buildConfigField("String", "BUILD_TIME", "\"$buildDate\"")
}

fun getEnv(name: String, defValue: String = ""): String {
    return System.getenv("RELAY_SERVER_$name") ?: defValue
}
