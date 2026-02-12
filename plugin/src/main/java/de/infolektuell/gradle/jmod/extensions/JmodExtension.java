package de.infolektuell.gradle.jmod.extensions;

import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.tasks.SourceSet;

import javax.inject.Inject;

public abstract class JmodExtension {
    public static final String EXTENSION_NAME = "jmod";

    private final SourceSet sourceSet;

    @Inject
    public JmodExtension(SourceSet sourceSet) {
        this.sourceSet = sourceSet;
    }

    public abstract DirectoryProperty getHeaderFiles();
    public abstract DirectoryProperty getLegalNotices();
    public abstract DirectoryProperty getLibs();
    public abstract ConfigurableFileCollection getLinkPath();
    public abstract ConfigurableFileCollection getLibraryPath();

    public String getLinkOnlyConfigurationName() { return sourceSet.getName().equals("main") ? "linkOnly" : sourceSet.getName() + "LinkOnly"; }
    public String getLinkPathConfigurationName() { return sourceSet.getName().equals("main") ? "linkPath" : sourceSet.getName() + "LinkPath"; }
    public String getLibraryPathConfigurationName() { return sourceSet.getName().equals("main") ? "libraryPath" : sourceSet.getName() + "LibraryPath"; }
    public String getApiJmodElementsConfigurationName() { return sourceSet.getName().equals("main") ? "apiJmodElements" : sourceSet.getName() + "ApiJmodElements"; }
    public String getRuntimeJmodElementsConfigurationName() { return sourceSet.getName().equals("main") ? "runtimeJmodElements" : sourceSet.getName() + "RuntimeJmodElements"; }
}
