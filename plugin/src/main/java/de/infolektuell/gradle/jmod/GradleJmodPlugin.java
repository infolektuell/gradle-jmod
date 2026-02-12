package de.infolektuell.gradle.jmod;

import de.infolektuell.gradle.jmod.extensions.JmodExtension;
import de.infolektuell.gradle.jmod.tasks.*;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.type.ArtifactTypeDefinition;
import org.gradle.api.attributes.*;
import org.gradle.api.attributes.java.*;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.Directory;
import org.gradle.api.plugins.JavaApplication;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.JavaExec;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.testing.Test;
import org.gradle.jvm.toolchain.JavaLanguageVersion;
import org.gradle.jvm.toolchain.JavaLauncher;
import org.gradle.jvm.toolchain.JavaToolchainService;
import org.gradle.language.jvm.tasks.ProcessResources;
import org.jspecify.annotations.NonNull;

import javax.inject.Inject;
import java.util.Objects;

@SuppressWarnings("UnstableApiUsage")
public abstract class GradleJmodPlugin implements Plugin<@NonNull Project> {
    public static final String PLUGIN_NAME = "de.infolektuell.jmod";
    public static abstract class jmodDisambiguationRule implements AttributeDisambiguationRule<@NonNull LibraryElements> {
        private final LibraryElements preference;
        @Inject public jmodDisambiguationRule(LibraryElements preference) {
            this.preference = preference;
        }
        @Override
        public void execute(MultipleCandidatesDetails<@NonNull LibraryElements> details) {
            details.closestMatch(preference);
        }
    }
    public static abstract class JmodCompatibilityRule implements AttributeCompatibilityRule<@NonNull LibraryElements> {
        @Override
        public void execute(CompatibilityCheckDetails<@NonNull LibraryElements> details) {
            if (Objects.isNull(details.getConsumerValue()) || Objects.isNull(details.getProducerValue())) return;
            if (details.getConsumerValue().getName().equals(LibraryElements.DYNAMIC_LIB)) {
                if (details.getProducerValue().getName().equals("jmod") || details.getProducerValue().getName().equals(LibraryElements.JAR)) details.compatible();
            }
            if (details.getConsumerValue().getName().equals("jmod")) {
                if (details.getProducerValue().getName().equals(LibraryElements.JAR)) details.compatible();
            }
        }
    }

    public void apply(Project project) {
        project.getPluginManager().withPlugin("java", libraryPlugin -> {
            final LibraryElements jmodLibraryElements = project.getObjects().named(LibraryElements.class, "jmod");
            final LibraryElements dynamicLibLibraryElements = project.getObjects().named(LibraryElements.class, LibraryElements.DYNAMIC_LIB);
            project.getDependencies().getAttributesSchema().attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, a -> {
                a.getCompatibilityRules().add(JmodCompatibilityRule.class);
                a.getDisambiguationRules().add(jmodDisambiguationRule.class, rule -> rule.params(jmodLibraryElements));
            });
            project.getDependencies().registerTransform(JmodExtractAction.class, action -> {
                action.getParameters().getPrefix().set(JmodExtractAction.Prefix.CLASSES);
                action.getFrom().attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, "jmod");
                action.getTo().attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, ArtifactTypeDefinition.JVM_CLASS_DIRECTORY);
            });
            project.getDependencies().registerTransform(JmodExtractAction.class, action -> {
                action.getParameters().getPrefix().set(JmodExtractAction.Prefix.LIB);
                action.getFrom().attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, "jmod");
                action.getTo().attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, ArtifactTypeDefinition.DIRECTORY_TYPE);
            });
            project.getDependencies().registerTransform(JmodExtractAction.class, action -> {
                action.getParameters().getPrefix().set(JmodExtractAction.Prefix.LIB);
                action.getFrom().attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, ArtifactTypeDefinition.JAR_TYPE);
                action.getTo().attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, ArtifactTypeDefinition.DIRECTORY_TYPE);
            });
            final JavaPluginExtension javaExtension = project.getExtensions().getByType(JavaPluginExtension.class);
            javaExtension.getSourceSets().configureEach(s -> {
                final var jmodExtension = project.getObjects().newInstance(JmodExtension.class, s);
                final Directory baseDir = project.getLayout().getProjectDirectory().dir("src/" + s.getName());
                jmodExtension.getHeaderFiles().convention(baseDir.dir("headers"));
                jmodExtension.getLegalNotices().convention(baseDir.dir("legal"));
                jmodExtension.getLibs().convention(baseDir.dir("lib"));
                project.getConfigurations().dependencyScope(jmodExtension.getLinkOnlyConfigurationName());
                final var libraryPathConfiguration = project.getConfigurations().resolvable(jmodExtension.getLibraryPathConfigurationName(), config -> {
                    config.extendsFrom(project.getConfigurations().getByName(s.getImplementationConfigurationName()));
                    config.extendsFrom(project.getConfigurations().getByName(jmodExtension.getLinkOnlyConfigurationName()));
                    config.attributes(a -> {
                        a.attribute(Category.CATEGORY_ATTRIBUTE, project.getObjects().named(Category.class, Category.LIBRARY));
                        a.attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, dynamicLibLibraryElements);
                        a.attribute(Usage.USAGE_ATTRIBUTE, project.getObjects().named(Usage.class, "java-runtime"));
                        a.attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, ArtifactTypeDefinition.DIRECTORY_TYPE);
                    });
                });
                jmodExtension.getLibraryPath().from(jmodExtension.getLibs(), libraryPathConfiguration);
                s.getExtensions().add(JmodExtension.EXTENSION_NAME, jmodExtension);
            });

            javaExtension.getSourceSets().named("main", s -> {
                final JmodExtension jmodExtension = s.getExtensions().getByType(JmodExtension.class);
                final var linkPathConfiguration = project.getConfigurations().resolvable(jmodExtension.getLinkPathConfigurationName(), config -> {
                    config.extendsFrom(project.getConfigurations().getByName(s.getImplementationConfigurationName()));
                    config.extendsFrom(project.getConfigurations().getByName(jmodExtension.getLinkOnlyConfigurationName()));
                    config.attributes(a -> {
                        a.attribute(Bundling.BUNDLING_ATTRIBUTE, project.getObjects().named(Bundling.class, Bundling.EXTERNAL));
                        a.attribute(Category.CATEGORY_ATTRIBUTE, project.getObjects().named(Category.class, Category.LIBRARY));
                        a.attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, jmodLibraryElements);
                        a.attribute(Usage.USAGE_ATTRIBUTE, project.getObjects().named(Usage.class, Usage.JAVA_RUNTIME));
                        a.attributeProvider(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, javaExtension.getToolchain().getLanguageVersion().map(JavaLanguageVersion::asInt));
                    });
                });
                jmodExtension.getLinkPath().from(s.getJava().getClassesDirectory(), linkPathConfiguration);

                final TaskProvider<@NonNull JmodCreateTask> createJmodTask = project.getTasks().register(s.getTaskName("create", "jmod"), JmodCreateTask.class, task -> {
                    task.setGroup("build");
                    task.setDescription("Converts a jar into a jmod file");
                    task.getMetadata().convention(getJavaToolchainService().launcherFor(javaExtension.getToolchain()).map(JavaLauncher::getMetadata));
                    task.getClasspath().from(s.getJava().getClassesDirectory(), project.getTasks().named(s.getProcessResourcesTaskName(), ProcessResources.class));
                    task.getHeaderFiles().from(jmodExtension.getHeaderFiles());
                    task.getLegalNotices().from(jmodExtension.getLegalNotices());
                    task.getLibs().from(jmodExtension.getLibs());
                    task.getArchiveFile().convention(project.getLayout().getBuildDirectory().file("libs/" + project.getName() + ".jmod"));
                });

                project.getConfigurations().consumable(jmodExtension.getApiJmodElementsConfigurationName(), config -> {
                    config.setDescription("API elements for the 'main' feature in 'jmod' format.");
                    config.attributes(a -> {
                        a.attribute(Bundling.BUNDLING_ATTRIBUTE, project.getObjects().named(Bundling.class, Bundling.EXTERNAL));
                        a.attribute(Category.CATEGORY_ATTRIBUTE, project.getObjects().named(Category.class, Category.LIBRARY));
                        a.attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, jmodLibraryElements);
                        a.attribute(Usage.USAGE_ATTRIBUTE, project.getObjects().named(Usage.class, Usage.JAVA_API));
                        a.attributeProvider(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, javaExtension.getToolchain().getLanguageVersion().map(JavaLanguageVersion::asInt));
                    });
                    config.getOutgoing().artifact(createJmodTask);
                    config.getOutgoing().getVariants().register("classes", classes -> {
                        classes.getDescription().set("    Directories containing compiled class files for main.");
                        classes.getAttributes().attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, project.getObjects().named(LibraryElements.class, LibraryElements.CLASSES));
                        classes.artifact(s.getJava().getClassesDirectory(), artifact -> artifact.setType(ArtifactTypeDefinition.JVM_CLASS_DIRECTORY));
                    });
                    config.getOutgoing().getVariants().register("headers", headers -> {
                        headers.getDescription().set("The header files that are included in the jmod file");
                        headers.getAttributes().attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, project.getObjects().named(LibraryElements.class, LibraryElements.HEADERS_CPLUSPLUS));
                        headers.artifact(jmodExtension.getHeaderFiles(), artifact -> artifact.setType(ArtifactTypeDefinition.DIRECTORY_TYPE));
                    });
                });
                project.getConfigurations().consumable(jmodExtension.getRuntimeJmodElementsConfigurationName(), config -> {
                    config.setDescription("Runtime elements for the 'main' feature in 'jmod' format.");
                    config.attributes(a -> {
                        a.attribute(Bundling.BUNDLING_ATTRIBUTE, project.getObjects().named(Bundling.class, Bundling.EXTERNAL));
                        a.attribute(Category.CATEGORY_ATTRIBUTE, project.getObjects().named(Category.class, Category.LIBRARY));
                        a.attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, jmodLibraryElements);
                        a.attribute(Usage.USAGE_ATTRIBUTE, project.getObjects().named(Usage.class, Usage.JAVA_RUNTIME));
                        a.attributeProvider(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, javaExtension.getToolchain().getLanguageVersion().map(JavaLanguageVersion::asInt));
                    });
                    config.getOutgoing().artifact(createJmodTask);
                    config.getOutgoing().getVariants().register("classes", classes -> {
                        classes.getDescription().set("    Directories containing compiled class files for main.");
                        classes.getAttributes().attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, project.getObjects().named(LibraryElements.class, LibraryElements.CLASSES));
                        classes.artifact(s.getJava().getClassesDirectory(), artifact -> artifact.setType(ArtifactTypeDefinition.JVM_CLASS_DIRECTORY));
                    });
                    config.getOutgoing().getVariants().register("resources", resources -> {
                        resources.getDescription().set("    Directories containing assembled resource files for main.");
                        resources.getAttributes().attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, project.getObjects().named(LibraryElements.class, LibraryElements.RESOURCES));
                        resources.artifact(project.getTasks().named(s.getProcessResourcesTaskName(), ProcessResources.class), artifact -> artifact.setType(ArtifactTypeDefinition.JVM_RESOURCES_DIRECTORY));
                    });
                    config.getOutgoing().getVariants().register("libs", libs -> {
                        libs.getDescription().set("The native libraries that are included in the jmod file");
                        libs.getAttributes().attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, dynamicLibLibraryElements);
                        libs.artifact(jmodExtension.getLibs(), artifact -> artifact.setType(ArtifactTypeDefinition.DIRECTORY_TYPE));
                    });
                });

                project.getPluginManager().withPlugin("application", applicationPlugin -> {
                    createJmodTask.configure(task -> {
                        final JavaApplication application = project.getExtensions().getByType(JavaApplication.class);
                        task.getMainClass().convention(application.getMainClass());
                    });
                    project.getTasks().named("run", JavaExec.class, task -> {
                        final var libraryPathProvider = project.getObjects().newInstance(LibraryPathProvider.class);
                        libraryPathProvider.getLibraryPath().from(jmodExtension.getLibraryPath());
                        task.getJvmArgumentProviders().add(libraryPathProvider);
                    });
                });
            });

            javaExtension.getSourceSets().named("test", s -> {
                final JmodExtension jmodExtension = s.getExtensions().getByType(JmodExtension.class);
                final Provider<@NonNull ConfigurableFileCollection> mainLibraryPath = javaExtension.getSourceSets().named("main").map(x -> x.getExtensions().getByType(JmodExtension.class).getLibraryPath());
                jmodExtension.getLibraryPath().from(mainLibraryPath);
                project.getTasks().withType(Test.class, task -> {
                    final LibraryPathProvider libraryPathProvider = project.getObjects().newInstance(LibraryPathProvider.class);
                    libraryPathProvider.getLibraryPath().from(jmodExtension.getLibraryPath());
                    task.getJvmArgumentProviders().add(libraryPathProvider);
                });
            });
            });
    }

    @Inject
    protected abstract JavaToolchainService getJavaToolchainService();
}
