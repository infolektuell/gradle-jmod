package de.infolektuell.gradle.jmod;

import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertTrue;

class GradleJmodPluginFunctionalTest {
    private final Path projectDir = Paths.get("..", "example");

    @Test
    void canBuild() {
        var runner = GradleRunner.create();
        runner.forwardOutput();
        runner.withPluginClasspath();
        runner.withProjectDir(projectDir.toFile());
        runner.withArguments("build");
        var result = runner.build();
        assertTrue(result.getOutput().contains("BUILD SUCCESSFUL"));
    }

    @Test
    void canCreateJmod() {
        var runner = GradleRunner.create();
        runner.forwardOutput();
        runner.withPluginClasspath();
        runner.withProjectDir(projectDir.toFile());
        runner.withArguments("createJmod");
        var result = runner.build();
        assertTrue(result.getOutput().contains("BUILD SUCCESSFUL"));
    }
}
