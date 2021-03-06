package com.cognifide.gradle.sling.instance.tasks

import com.cognifide.gradle.sling.common.instance.action.AwaitDownAction
import com.cognifide.gradle.sling.common.instance.names
import com.cognifide.gradle.sling.common.tasks.LocalInstanceTask
import org.gradle.api.tasks.TaskAction

open class InstanceDown : LocalInstanceTask() {

    private var awaitDownOptions: AwaitDownAction.() -> Unit = {}

    fun awaitDown(options: AwaitDownAction.() -> Unit) {
        this.awaitDownOptions = options
    }

    @TaskAction
    fun down() {
        localInstanceManager.base.examinePrerequisites(instances.get())

        val downInstances = localInstanceManager.down(instances.get(), awaitDownOptions)
        if (downInstances.isNotEmpty()) {
            common.notifier.notify("Instance(s) down", "Which: ${downInstances.names}")
        }
    }

    init {
        description = "Turns off local Sling instance(s)."
    }

    companion object {
        const val NAME = "instanceDown"
    }
}
