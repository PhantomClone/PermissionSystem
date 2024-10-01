import dev.s7a.gradle.minecraft.server.tasks.LaunchMinecraftServerTask
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.StandardCopyOption

plugins {
    id("java")
    id("dev.s7a.gradle.minecraft.server") version "3.0.0"
    id("com.gradleup.shadow") version "8.3.0"
    id("com.avast.gradle.docker-compose") version "0.17.8"
}

group = "me.phantomclone.permissionsystem"
version = "1.0-SNAPSHOT"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
    maven {
        url = uri("https://repo.dmulloy2.net/repository/public/")
    }
}

dependencies {
    implementation("de.chojo.sadu", "sadu", "1.3.1")
    implementation("org.postgresql:postgresql:42.7.2")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.18.0")


    compileOnly("com.comphenix.protocol:ProtocolLib:5.3.0-SNAPSHOT")
    compileOnly("io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT")

    compileOnly("org.projectlombok:lombok:1.18.34")
    annotationProcessor("org.projectlombok:lombok:1.18.34")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.11.0")
    testImplementation("org.mockito:mockito-core:5.13.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.13.0")

    testImplementation("io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.11.0")
}

tasks.withType<JavaCompile> {
    options.release.set(21)
    options.encoding = "UTF-8"
}

tasks {
    processResources {
        inputs.property("version", project.version)

        filesMatching("plugin.yml") {
            expand("version" to project.version)
        }
    }

    shadowJar {
        relocate("de.chojo.sadu", "me.phantomclone.permissionsystem.sadu")
        archiveFileName.set("${project.name}-${project.version}.jar")
    }
}

tasks.named<Test>("test") {
    useJUnitPlatform()

    maxHeapSize = "1G"

    testLogging {
        events("passed")
    }
}

task<LaunchMinecraftServerTask>("testPlugin") {
    dependsOn("shadowJar")

    doFirst {
        copy {
            from(layout.buildDirectory.dir("libs/${project.name}-${project.version}.jar"))
            into(layout.buildDirectory.dir("MinecraftServer/plugins"))
        }

        val pluginsDir = layout.buildDirectory.dir("MinecraftServer/plugins").get().asFile

        val protocolLibUri = URI("https://ci.dmulloy2.net/job/ProtocolLib/732/artifact/build/libs/ProtocolLib.jar")
        val protocolLibFile = File(pluginsDir, "ProtocolLib.jar").toPath()

        if (!Files.exists(protocolLibFile)) {
            println("ProtocolLib missing. Start download...")

            val client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build()

            val request = HttpRequest.newBuilder()
                .uri(protocolLibUri)
                .GET()
                .build()

            val response = client.send(request, HttpResponse.BodyHandlers.ofInputStream())

            if (response.statusCode() in 200..399) {
                Files.copy(response.body(), protocolLibFile, StandardCopyOption.REPLACE_EXISTING)
            } else {
                throw RuntimeException("Error while downloading ProtocolLib: HTTP code ${response.statusCode()}")
            }
        }
    }

    jarUrl.set(LaunchMinecraftServerTask.JarUrl.Paper("1.21.1"))
    agreeEula.set(true)
}

dockerCompose {
    useComposeFiles.add("src/test/resources/compose.yaml")
    isRequiredBy(tasks.test)
    isRequiredBy(tasks.named("testPlugin"))
}