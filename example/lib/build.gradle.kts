plugins {
    `java-library`
    id("de.infolektuell.jextract") version "1.2.0"
    id("de.infolektuell.jmod")
}

repositories {
    // Use Maven Central for resolving dependencies.
    mavenCentral()
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

dependencies {
    implementation(libs.guava)

    // Use JUnit Jupiter for testing.
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.named<Test>("test") {
    // Use JUnit Platform for unit tests.
    useJUnitPlatform()
    jvmArgs("--enable-native-access=ALL-UNNAMED")
}

jextract.libraries {
    // The native BASS audio library.
    val bass by registering {
        // output = layout.projectDirectory.dir("bassBindings")
        header = layout.projectDirectory.file("src/main/public/bass.h")
        headerClassName = "Bass"
        targetPackage = "com.un4seen.bass"
        // Make your public headers folder searchable for Jextract
        includes.add(layout.projectDirectory.dir("src/main/public"))
        // For large headers it is good practice to generate only the symbols you need.
        whitelist {
            // We only want to access the BASS version
            functions.add("BASS_GetVersion")
        }
        useSystemLoadLibrary = true
        libraries.add("bass")
    }
    sourceSets.named("main") {
            jextract.libraries.addLater(bass)
            jmod.libs = findLibraries("bass").get()
    }
}

fun findLibraries(name: String): Provider<Directory> {
    return providers.systemProperty("os.name").map { os ->
        val osPart = if (os.contains("windows", true)) "windows"
        else if (os.contains("mac", true)) "macos"
        else "linux"
        layout.projectDirectory.dir("src/main/lib/${name}/${osPart}/x64")
    }
}
