package com.cognifide.gradle.sling.test

import com.cognifide.gradle.sling.internal.file.FileOperations
import org.gradle.testkit.runner.GradleRunner
import org.gradle.util.GFileUtils
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.io.File

abstract class SlingTest {

    @Rule
    @JvmField
    var tmpDir = TemporaryFolder()

    fun buildTask(rootProjectDir: String, taskName: String, callback: SlingBuild.() -> Unit) {
        build(rootProjectDir, { withArguments(taskName, "-i", "-S") }, {
            assertTaskOutcome(taskName)
            callback()
        })
    }

    fun buildTasks(rootProjectDir: String, taskName: String, callback: SlingBuild.() -> Unit) {
        build(rootProjectDir, { withArguments(taskName, "-i", "-S") }, {
            assertTaskOutcomes(taskName)
            callback()
        })
    }

    fun build(rootProjectDir: String, configurer: GradleRunner.() -> Unit, callback: SlingBuild.() -> Unit) {
        val projectDir = File(tmpDir.newFolder(), rootProjectDir)

        GFileUtils.mkdirs(projectDir)
        FileOperations.copyResources("test/$rootProjectDir", projectDir)

        val runner = GradleRunner.create()
                .withProjectDir(projectDir)
                .apply(configurer)
        val result = runner.build()

        callback(SlingBuild(result, projectDir))
    }

    fun readFile(fileName: String): String {
        return javaClass.getResourceAsStream(fileName).bufferedReader().use { it.readText() }
    }

    fun readFile(file: File): String {
        return file.bufferedReader().use { it.readText() }
    }

}