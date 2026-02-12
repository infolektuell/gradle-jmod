package de.infolektuell.gradle.jmod.tasks;

import org.gradle.api.GradleException;
import org.gradle.api.artifacts.transform.*;
import org.gradle.api.file.FileSystemLocation;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.jspecify.annotations.NonNull;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@CacheableTransform
public abstract class JmodExtractAction implements TransformAction<JmodExtractAction.@NonNull Parameters> {
    public interface Prefix {
        String CLASSES = "classes";
        String LIB = "lib";
    }

    public interface Parameters extends TransformParameters {
        @Input
        Property<@NonNull String> getPrefix();
    }

    @InputArtifact
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract Provider<@NonNull FileSystemLocation> getInputArtifact();

    @Override
    public void transform(@NonNull TransformOutputs outputs) {
        final File inFile = getInputArtifact().get().getAsFile();
        if (!inFile.getName().endsWith(".jmod")) return;
        if (!inFile.exists()) return;
        final Path prefixPath = Path.of(getParameters().getPrefix().get());
        final Path outDir = outputs.dir(getInputArtifact().get().getAsFile().getName().replaceAll("\\.jmod$", "")).toPath();
        try (ZipFile zipFile = new ZipFile(inFile)) {
            final Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                final ZipEntry e = entries.nextElement();
                final Path namePath = Path.of(e.getName());
                if (!namePath.startsWith(prefixPath)) continue;
                final Path outPath = outDir.resolve(namePath.subpath(1, namePath.getNameCount()));
                final var s = zipFile.getInputStream(e);
                Files.copy(s, outPath);
            }
        } catch (Exception ignored) {
            throw new GradleException(String.format("Failed to extract files from %s", inFile.getName()));
        }
    }
}
