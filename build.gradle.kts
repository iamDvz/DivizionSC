buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("org.ow2.asm:asm:9.7.1")
        classpath("org.ow2.asm:asm-commons:9.7.1")
    }
}

plugins {
    java
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = project.properties["project.group"] as String
version = project.properties["project.version"] as String
description = project.properties["project.description"] as String

// Путь с кириллицей — только здесь (UTF-8 literal), не в gradle.properties
val localPluginsDirPath = findProperty("local.plugins.dir") as String?
    ?: "/Volumes/Minecraft/Локальные сервера/1.21.11 VFX/plugins"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
    maven(url = "https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:${project.properties["dependency.paper"]}")
    implementation("com.elmakers.mine.bukkit:EffectLib:${project.properties["dependency.effectlib"]}")
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.processResources {
    filesMatching("plugin.yml") {
        expand(project.properties)
    }
}

tasks.shadowJar {
    archiveClassifier.set("")
    archiveFileName.set("DivizionSC-${version}.jar")
    relocate("de.slikey", "ru.iamdvz.divizionsc.libs.slikey")
    mergeServiceFiles()
}

tasks.jar {
    enabled = false
}

val pluginApi by configurations.creating {
    isCanBeConsumed = true
    isCanBeResolved = false
}

tasks.register<Jar>("pluginApiJar") {
    archiveClassifier.set("api")
    from(sourceSets.main.get().output)
}

artifacts {
    add(pluginApi.name, tasks.named("pluginApiJar"))
}

tasks.register<Delete>("cleanServerPlugins") {
    onlyIf { file(localPluginsDirPath).isDirectory }
    delete(fileTree(localPluginsDirPath) {
        include("DivizionSC-*.jar")
        include("DivizionSC.jar")
    })
}

tasks.register<Copy>("copyPlugin") {
    onlyIf { file(localPluginsDirPath).isDirectory }
    dependsOn("cleanServerPlugins", "shadowJar")
    from(tasks.named("shadowJar"))
    into(localPluginsDirPath)
    rename { "DivizionSC.jar" }
}

tasks.build {
    dependsOn(tasks.shadowJar)
    dependsOn(":DSC_MEG:build")
    if (file(localPluginsDirPath).isDirectory) {
        finalizedBy("copyPlugin")
    }
}
