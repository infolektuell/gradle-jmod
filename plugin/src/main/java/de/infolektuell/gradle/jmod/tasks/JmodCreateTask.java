package de.infolektuell.gradle.jmod.tasks;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;
import org.gradle.jvm.toolchain.JavaInstallationMetadata;
import org.gradle.process.ExecOperations;
import org.jspecify.annotations.NonNull;

import javax.inject.Inject;
import java.io.File;
import java.nio.file.Files;
import java.util.regex.Pattern;

public abstract class JmodCreateTask extends DefaultTask {
    @Inject
    protected abstract ExecOperations getExecOperations();

    @Nested
    public abstract Property<@NonNull JavaInstallationMetadata> getMetadata();

    @Classpath
    @InputFiles
    public abstract ConfigurableFileCollection getClasspath();

    @Optional
    @Input
    public abstract Property<@NonNull String> getCompress();

    @Optional
    @Input
    public abstract ListProperty<@NonNull String> getExcludes();

    @InputFiles
    public abstract ConfigurableFileCollection getHeaderFiles();

    @InputFiles
    public abstract ConfigurableFileCollection getLegalNotices();

    @InputFiles
    public abstract ConfigurableFileCollection getLibs();

    @InputFiles
    public abstract ConfigurableFileCollection getManPages();

    @Classpath
    @InputFiles
    public abstract ConfigurableFileCollection getModulePath();

    @Optional
    @Input
    public abstract Property<@NonNull String> getMainClass();

    @Optional
    @Input
    public abstract Property<@NonNull String> getModuleVersion();

    @Optional
    @Input
    public abstract Property<@NonNull String> getTargetPlatform();

    @OutputFile
    public abstract RegularFileProperty getArchiveFile();

    @TaskAction
    protected void create() {
        final var executable = getMetadata().get().getInstallationPath().getAsFileTree().matching(m -> m.include("**/bin/jmod", "**/bin/jmod.exe")).getSingleFile();
        final var archive = getArchiveFile().get().getAsFile().toPath();
        try {
            Files.deleteIfExists(archive);
        } catch (Exception ignored) {
            throw new GradleException("Couldn't delete jmod file " + archive);
        }

        getExecOperations().exec(spec -> {
            spec.executable(executable);
            spec.args("create");
            final FileCollection classpath = getClasspath().filter(File::exists);
            if (!classpath.isEmpty()) spec.args("--class-path", classpath.getAsPath());
            if (getCompress().isPresent()) spec.args("--compress", getCompress().get());
            final var excludes = String.join(",", getExcludes().get());
            if (!excludes.isEmpty()) spec.args("--exclude", excludes);
            final FileCollection headers = getHeaderFiles().filter(File::exists);
            if (!headers.isEmpty()) spec.args("--header-files", headers.getAsPath());
            final FileCollection legal = getLegalNotices().filter(File::exists);
            if (!legal.isEmpty()) spec.args("--legal-notices", legal.getAsPath());
            final FileCollection libs = getLibs().filter(File::exists);
            if (!libs.isEmpty()) spec.args("--libs", libs.getAsPath());
            if (getMainClass().isPresent()) spec.args("--main-class", getMainClass().get());
            if (!getManPages().isEmpty()) spec.args("--man-pages", getManPages().getAsPath());
            final var modulePath = getModulePath().filter(File::exists);
            if (!modulePath.isEmpty()) spec.args("--module-path", modulePath.getAsPath());
            if (getModuleVersion().isPresent()) {
                final var pattern = Pattern.compile("^[0-9]+(\\.[0-9]+){0,2}$");
                final var matcher = pattern.matcher(getModuleVersion().get());
                if (matcher.matches()) spec.args("--module-version", getModuleVersion().get());
            }
            if (getTargetPlatform().isPresent()) spec.args("--target-platform", getTargetPlatform().get());
            spec.args(getArchiveFile().get());
        });
    }
}
