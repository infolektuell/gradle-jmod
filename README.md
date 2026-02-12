# Gradle Jmod Plugin

This Gradle plugin adds capabilities for jmod file handling to the Java plugin.

- create jmod archives from compiled classes, resources, native libs and more. The jmod is exposed to be consumable by other projects.
- Add jmod dependencies: either as files, or another project that generates and exposes a jmod.

## Quick Start

```kts
plugins {
    `java-library`
    id("de.infolektuell.jmod") version "x.y.z"
}

repositories {
    mavenCentral()
}

java {
    toolchain {
      // The jpackage plugin uses the configured toolchain by default  
      languageVersion = JavaLanguageVersion.of(25)
    }
}

application {
  mainClass = "org.example.App"
}
```

Now run this Gradle task to produce a jmod:

```sh
gradlew createJmod
```

The jmod archive should appear under `build/libs/<project name>.jmod`.

## License

[MIT License](LICENSE.txt)
