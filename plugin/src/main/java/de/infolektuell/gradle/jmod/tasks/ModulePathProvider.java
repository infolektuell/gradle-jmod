package de.infolektuell.gradle.jmod.tasks;

import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.InputFiles;
import org.gradle.process.CommandLineArgumentProvider;

import java.util.ArrayList;
import java.util.List;

public abstract class ModulePathProvider implements CommandLineArgumentProvider {
    @InputFiles
    @Classpath
    public abstract ConfigurableFileCollection getModulePath();

    @Override
    public Iterable<String> asArguments() {
        final List<String> args = new ArrayList<>();
        if (getModulePath().isEmpty()) return args;
        args.add("--module-path");
        args.add(getModulePath().getAsPath());
        return args;
    }
}
