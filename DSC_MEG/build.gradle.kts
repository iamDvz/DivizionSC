plugins {
    java
}

group = project.properties["project.group"] as String
version = project.properties["project.version"] as String
description = "DSC_MEG — ModelEngine VFX bridge for DivizionSC"

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
    maven(url = "https://mvn.lumine.io/repository/maven-public/")
}

dependencies {
    compileOnly(project(":", configuration = "pluginApi"))
    compileOnly("io.papermc.paper:paper-api:${project.properties["dependency.paper"]}")
    compileOnly("com.ticxo.modelengine:ModelEngine:${project.properties["dependency.modelengine"]}")
    compileOnly("org.joml:joml:${project.properties["dependency.joml"]}")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    dependsOn(":jar")
}

tasks.processResources {
    filesMatching("plugin.yml") {
        expand(project.properties)
    }
}

tasks.withType<Jar> {
    archiveFileName.set("DSC_MEG-${version}.jar")
}

tasks.register<Delete>("cleanServerPlugins") {
    doNotTrackState("External server plugins directory")
    onlyIf { file(localPluginsDirPath).isDirectory }
    delete(fileTree(localPluginsDirPath) {
        include("DSC_MEG-*.jar")
        include("DSC_MEG.jar")
    })
}

tasks.register<Copy>("copyPlugin") {
    doNotTrackState("External server plugins directory")
    onlyIf { file(localPluginsDirPath).isDirectory }
    dependsOn("cleanServerPlugins", "jar")
    from(tasks.named("jar"))
    into(localPluginsDirPath)
    rename { "DSC_MEG.jar" }
}

tasks.named("build") {
    if (file(localPluginsDirPath).isDirectory) {
        finalizedBy("copyPlugin")
    }
}
