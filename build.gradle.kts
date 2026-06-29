plugins {
    java
    id("com.gradleup.shadow") version "8.3.5"
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
    maven(url = "https://maven.elmakers.com/repository/")
    maven(url = "https://repo.extendedclip.com/releases/")
    maven(url = "https://maven.enginehub.org/repo/")
    maven(url = "https://jitpack.io")
}

dependencies {
    val paper = project.properties["dependency.paper"]
    val effectlib = project.properties["dependency.effectlib"]
    val elytrium = project.properties["dependency.elytrium"]
    val hikari = project.properties["dependency.hikari"]
    val sqlite = project.properties["dependency.sqlite"]
    val mariadb = project.properties["dependency.mariadb"]
    val exp4j = project.properties["dependency.exp4j"]

    compileOnly("io.papermc.paper:paper-api:$paper")
    // EffectLib — elMakers repo, не Maven Central → shade в JAR (relocate de.slikey.*).
    compileOnly("com.elmakers.mine.bukkit:EffectLib:$effectlib")
    shadow("com.elmakers.mine.bukkit:EffectLib:$effectlib")

    // Soft-depend интеграции — только compileOnly (плагины на сервере, не встраиваем).
    compileOnly("me.clip:placeholderapi:${project.properties["dependency.placeholderapi"]}")
    compileOnly("com.github.MilkBowl:VaultAPI:${project.properties["dependency.vault"]}") {
        exclude(group = "org.bukkit", module = "bukkit")
    }
    compileOnly("com.sk89q.worldguard:worldguard-bukkit:${project.properties["dependency.worldguard"]}")

    // Библиотеки времени выполнения грузятся Paper через plugin.yml `libraries` (Maven Central).
    // implementation (а не compileOnly) — версии должны совпадать с plugin.yml; jar их не встраивает.
    implementation("net.elytrium:serializer:$elytrium")
    implementation("com.zaxxer:HikariCP:$hikari")
    implementation("org.xerial:sqlite-jdbc:$sqlite")
    implementation("org.mariadb.jdbc:mariadb-java-client:$mariadb")
    implementation("net.objecthunter:exp4j:$exp4j")

    testImplementation("io.papermc.paper:paper-api:$paper")
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.processResources {
    val tokens = mapOf(
        "version" to version.toString(),
        "elytriumVersion" to project.properties["dependency.elytrium"],
        "hikariVersion" to project.properties["dependency.hikari"],
        "sqliteVersion" to project.properties["dependency.sqlite"],
        "mariadbVersion" to project.properties["dependency.mariadb"],
        "exp4jVersion" to project.properties["dependency.exp4j"]
    )
    inputs.properties(tokens)
    filesMatching("plugin.yml") {
        expand(tokens)
    }
}

tasks.jar {
    enabled = false
}

tasks.shadowJar {
    archiveClassifier.set("")
    archiveFileName.set("DivizionSC-${version}.jar")
    configurations = listOf(project.configurations.named("shadow").get())
    relocate("de.slikey.effectlib", "ru.iamdvz.divizionsc.lib.effectlib")
    mergeServiceFiles()
}

tasks.assemble {
    dependsOn(tasks.shadowJar)
}

// Конфигурация для аддонов (DSC_MM/DSC_MEG) — компиляция против API основного плагина.
val pluginApi by configurations.creating {
    isCanBeConsumed = true
    isCanBeResolved = false
}

artifacts {
    add(pluginApi.name, tasks.named("shadowJar"))
}

tasks.register<Delete>("cleanServerPlugins") {
    doNotTrackState("External server plugins directory")
    onlyIf { file(localPluginsDirPath).isDirectory }
    delete(fileTree(localPluginsDirPath) {
        include("DivizionSC-*.jar")
        include("DivizionSC.jar")
    })
}

tasks.register<Copy>("copyPlugin") {
    doNotTrackState("External server plugins directory")
    onlyIf { file(localPluginsDirPath).isDirectory }
    dependsOn("cleanServerPlugins", "shadowJar")
    from(tasks.named("shadowJar"))
    into(localPluginsDirPath)
    rename { "DivizionSC.jar" }
}

val vscodeExtensionDir = rootDir.parentFile.resolve("DivizionSC-vscode")
val dscLanguageJson = vscodeExtensionDir.resolve("generated/dsc-language.json")
val dscExportClasses = layout.buildDirectory.dir("dsc-export-classes")

tasks.register<JavaCompile>("compileDscLanguageExporter") {
    group = "build"
    description = "Compile DSC language exporter (no Paper API)"
    source = fileTree("src/main/java") {
        include(
            "ru/iamdvz/divizionsc/def/effect/EffectVerbs.java",
            "ru/iamdvz/divizionsc/def/loader/dsl/DscBlockKind.java",
            "ru/iamdvz/divizionsc/def/loader/dsl/DscSyntax.java",
            "ru/iamdvz/divizionsc/def/loader/dsl/DscLanguageCatalog.java",
            "ru/iamdvz/divizionsc/def/loader/dsl/DscLanguageExporter.java"
        )
    }
    destinationDirectory.set(dscExportClasses)
    classpath = files()
    options.encoding = "UTF-8"
}

tasks.register<JavaExec>("exportDscLanguage") {
    group = "build"
    description = "Export DSC language metadata for VS Code extension"
    dependsOn("compileDscLanguageExporter")
    classpath = files(dscExportClasses)
    mainClass.set("ru.iamdvz.divizionsc.def.loader.dsl.DscLanguageExporter")
    args(dscLanguageJson.absolutePath, version.toString())
    inputs.files(
        fileTree("src/main/java/ru/iamdvz/divizionsc/def") {
            include("**/EffectVerbs.java", "**/DscBlockKind.java", "**/DscSyntax.java", "**/DscLanguageCatalog.java", "**/DscLanguageExporter.java")
        }
    )
    outputs.file(dscLanguageJson)
    onlyIf { vscodeExtensionDir.isDirectory }
}

val installVsCodeExtensionFlag = findProperty("install.vscode.extension")?.toString()?.toBoolean() ?: true

tasks.register("installVsCodeExtension") {
    group = "build"
    description = "Export catalog and install DivizionSC DSC extension into Cursor/VS Code"
    dependsOn("exportDscLanguage")
    onlyIf { installVsCodeExtensionFlag && vscodeExtensionDir.isDirectory }

    doLast {
        val pkg = vscodeExtensionDir.resolve("package.json")
        if (pkg.isFile) {
            val text = pkg.readText()
            val updated = text.replace(Regex("\"version\"\\s*:\\s*\"[^\"]+\""), "\"version\": \"$version\"")
            if (text != updated) {
                pkg.writeText(updated)
            }
        }

        val folderName = "iamdvz.divizionsc-dsc-$version"
        val home = File(System.getProperty("user.home"))
        listOf(home.resolve(".cursor/extensions"), home.resolve(".vscode/extensions")).forEach { root ->
            if (!root.isDirectory) {
                return@forEach
            }
            root.listFiles()?.filter {
                it.isDirectory
                        && it.name.startsWith("iamdvz.divizionsc-dsc-")
                        && it.name != folderName
            }?.forEach { old ->
                old.deleteRecursively()
                logger.lifecycle("DivizionSC DSC: removed old extension ${old.name}")
            }
            val target = root.resolve(folderName)
            copy {
                from(vscodeExtensionDir)
                into(target)
                exclude("node_modules/**", ".git/**", "**/*.vsix", ".vscode/**")
            }
            logger.lifecycle("DivizionSC DSC: installed to ${target.absolutePath}")
        }

        logger.lifecycle(
            "DivizionSC DSC extension updated. Reload editor: Cmd+Shift+P → Developer: Reload Window"
        )
    }
}

tasks.build {
    dependsOn(":DSC_MEG:build")
    dependsOn(":DSC_MM:build")
    if (installVsCodeExtensionFlag && vscodeExtensionDir.isDirectory) {
        finalizedBy("installVsCodeExtension")
    }
    if (file(localPluginsDirPath).isDirectory) {
        finalizedBy("copyPlugin")
    }
}
