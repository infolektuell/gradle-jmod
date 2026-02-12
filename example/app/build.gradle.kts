plugins {
    application
    id("de.infolektuell.jmod")
    id("de.infolektuell.jpackage") version "0.4.1"
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
    implementation(project(":lib"))

    // Use JUnit Jupiter for testing.
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

application {
    mainModule = "de.infolektuell.bass.app"
    mainClass = "de.infolektuell.bass.app.Main"
    applicationDefaultJvmArgs = listOf("--enable-native-access=de.infolektuell.bass.main")
}
