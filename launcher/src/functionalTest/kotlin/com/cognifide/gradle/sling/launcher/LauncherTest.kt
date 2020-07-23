package com.cognifide.gradle.sling.launcher

import org.buildobjects.process.ProcBuilder
import org.junit.jupiter.api.Test
import java.io.File
import java.util.concurrent.TimeUnit

class LauncherTest {

    @Test
    fun shouldRunProperly() = test("tasks") { launch("tasks") }

    @Test
    fun shouldDisplayInstanceStatus() = test("instance-status") {
        launch("instanceStatus", "-Pinstance.local-master.httpUrl=http://localhost:8502") // some unavailable instance
    }

    private fun test(name: String, callback: File.() -> Unit) {
        File("build/functionalTest/$name").apply {
            deleteRecursively()
            mkdirs()
            callback()
        }
    }

    private fun File.launch(vararg args: String) {
        ProcBuilder("java", "-jar", File("build/libs/gsp.jar").absolutePath, *args).apply {
            withWorkingDirectory(this@launch)
            withOutputStream(System.out)
            withErrorStream(System.err)
            withTimeoutMillis(TimeUnit.SECONDS.toMillis(120))
            run()
        }
    }
}