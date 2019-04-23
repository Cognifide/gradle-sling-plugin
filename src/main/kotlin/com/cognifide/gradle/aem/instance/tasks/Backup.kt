package com.cognifide.gradle.aem.instance.tasks

import com.cognifide.gradle.aem.common.Formats
import com.cognifide.gradle.aem.common.tasks.ZipTask
import com.cognifide.gradle.aem.instance.InstanceException
import com.cognifide.gradle.aem.instance.names
import java.io.File
import org.gradle.api.execution.TaskExecutionGraph
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.bundling.ZipEntryCompression

open class Backup : ZipTask() {

    init {
        description = "Turns off local instance(s), archives to ZIP file, then turns on again."

        archiveBaseName.convention(project.provider { "${project.rootProject.name}-${Formats.dateFileName()}" })
        archiveClassifier.convention("backup")

        duplicatesStrategy = DuplicatesStrategy.FAIL
        entryCompression = ZipEntryCompression.STORED
    }

    @get:Internal
    val available: List<File>
        get() {
            return (destinationDirectory.asFile.get().listFiles {
                _, name -> name.endsWith("-$archiveClassifier.$archiveExtension")
            } ?: arrayOf()).ifEmpty { arrayOf() }.toList()
        }

    override fun taskGraphReady(graph: TaskExecutionGraph) {
        if (!graph.hasTask(this)) {
            return
        }

        val uncreatedInstances = aem.localInstances.filter { !it.created }
        if (uncreatedInstances.isNotEmpty()) {
            throw InstanceException("Cannot create backup of local instances, because there are instances not yet created: ${uncreatedInstances.names}")
        }
    }

    override fun projectEvaluated() {
        from(aem.config.localInstanceOptions.root)
    }

    companion object {
        const val NAME = "aemBackup"
    }
}