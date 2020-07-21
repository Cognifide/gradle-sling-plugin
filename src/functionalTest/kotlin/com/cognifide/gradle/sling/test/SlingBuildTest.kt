package com.cognifide.gradle.sling.test

import org.gradle.testkit.runner.GradleRunner
import java.io.File

open class SlingBuildTest {

    fun prepareProject(path: String, definition: File.() -> Unit) = File("build/functionalTest/$path").apply {
        deleteRecursively()
        mkdirs()
        definition()
    }

    fun File.file(path: String, text: String) {
        resolve(path).apply { parentFile.mkdirs() }.writeText(text.trimIndent())
    }

    fun File.buildSrc(text: String) = file("buildSrc/build.gradle.kts", text)

    fun File.settingsGradle(text: String) = file("settings.gradle.kts", text)

    fun File.buildGradle(text: String) = file("build.gradle.kts", text)

    fun File.gradleProperties(text: String) = file("gradle.properties", text)

    fun runBuild(projectDir: File, vararg arguments: String, asserter: SlingBuildResult.() -> Unit) {
        runBuild(projectDir, { withArguments(*arguments, "-i", "-S") }, asserter)
    }

    fun runBuild(projectDir: File, runnerOptions: GradleRunner.() -> Unit, asserter: SlingBuildResult.() -> Unit) {
        SlingBuildResult(runBuild(projectDir, runnerOptions), projectDir).apply(asserter)
    }

    fun runBuild(projectDir: File, options: GradleRunner.() -> Unit) = GradleRunner.create().run {
        forwardOutput()
        withPluginClasspath()
        withProjectDir(projectDir)
        apply(options)
        build()
    }

    // === Samples  ===

    fun File.helloServiceJava(rootPath: String = "") {
        file(rootPath(rootPath, "src/main/java/com/company/example/sling/HelloService.java"), """
            package com.company.example.sling;
            
            import org.osgi.service.component.annotations.Activate;
            import org.osgi.service.component.annotations.Component;
            import org.osgi.service.component.annotations.Deactivate;
            import org.slf4j.Logger;
            import org.slf4j.LoggerFactory;
            
            @Component(immediate = true, service = HelloService.class)
            class HelloService {
                               
                private static final Logger LOG = LoggerFactory.getLogger(HelloService.class);
                
                @Activate
                protected void activate() {
                    LOG.info("Hello world!");
                }
                
                @Deactivate
                protected void deactivate() {
                    LOG.info("Good bye world!");
                }
            }
        """)

        file(rootPath(rootPath, "src/test/java/com/company/example/sling/HelloServiceTest.java"), """
            package com.company.example.sling;
            
            import static org.junit.jupiter.api.Assertions.*;
            
            import org.junit.jupiter.api.Test;
            import org.junit.jupiter.api.extension.ExtendWith;
            import org.apache.sling.testing.mock.sling.junit5.SlingContext;
            import org.apache.sling.testing.mock.sling.junit5.SlingContextExtension;
            
            @ExtendWith(SlingContextExtension.class)
            class HelloServiceTest {
                
                private final SlingContext context = new SlingContext();
                    
                @Test
                void shouldUseService() {
                    context.registerInjectActivateService(new HelloService());
                    assertNotNull(context.getService(HelloService.class));
                }
            }
        """)
    }

    fun rootPath(rootPath: String, path: String) = rootPath.takeIf { it.isNotBlank() }?.let { "$it/$path" } ?: path
}
