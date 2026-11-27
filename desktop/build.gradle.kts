import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm") version "2.2.10"
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.10"
    id("org.jetbrains.compose") version "1.9.0"
}

group = "com.example"
version = "1.0.0"


sourceSets {
    main { kotlin.srcDir("../shared/src/main/kotlin") }
    test { kotlin.srcDir("../shared/src/test/kotlin") }
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
    // As entidades compartilhadas usam as anotações do Room; no desktop elas são só metadados.
    implementation("androidx.room:room-common:2.7.0")
    implementation("com.squareup.moshi:moshi-kotlin:1.15.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.10.2")
    testImplementation(kotlin("test-junit"))
}

compose.desktop {
    application {
        mainClass = "com.example.desktop.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Deb, TargetFormat.Msi, TargetFormat.Exe)
            packageName = "EducLib"
            packageVersion = project.version.toString()
            description = "Biblioteca escolar: acervo, leitores, empréstimos e mapa 3D das estantes"
            vendor = "EducLib"
            copyright = "EducLib contributors — GPL-3.0-or-later"
            licenseFile.set(project.file("../LICENSE"))
            modules("java.naming", "java.sql")
            linux {
                packageName = "educlib"
                debPackageVersion = project.version.toString()
                appCategory = "Education"
                menuGroup = "Education"
                shortcut = true
                iconFile.set(project.file("icons/educlib.png"))
            }
            windows {
                menuGroup = "EducLib"
                shortcut = true
                dirChooser = true
                perUserInstall = true
                // Fixo para sempre: permite que versões novas atualizem a instalação existente.
                upgradeUuid = "6f4d0c57-2b1e-4d0a-9a4f-6c9b1e2d7a31"
                iconFile.set(project.file("icons/educlib.ico"))
            }
        }
    }
}
