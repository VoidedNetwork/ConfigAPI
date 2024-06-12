import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("java")
    id("maven-publish")
    id("io.freefair.lombok") version "8.4"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

object Project {
    const val NAME = "ConfigAPI"
    const val GROUP = "gg.voided"
    const val VERSION = "1.0"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("dev.dejvokep:boosted-yaml:1.3.5")
}

tasks {
    named<ShadowJar>("shadowJar") {
        relocate("dev.dejvokep", "gg.voided.api.config.libs.dev.dejvokep")
    }

    register<Copy>("copy") {
        from(named("shadowJar"))
        rename("(.*)-all.jar", "${Project.NAME}-${Project.VERSION}.jar")
        into(file("jars"))
    }

    register("delete") {
        doLast { file("jars").deleteRecursively() }
    }
}

publishing {
    publications {
        create<MavenPublication>("release") {
            artifactId = Project.NAME
            groupId = Project.GROUP
            version = Project.VERSION

            from(components["java"])
        }
    }
}