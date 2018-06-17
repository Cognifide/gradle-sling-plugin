package com.cognifide.gradle.sling.instance

import com.cognifide.gradle.sling.base.BasePlugin
import com.cognifide.gradle.sling.pkg.ComposeTask
import com.cognifide.gradle.sling.pkg.PackagePlugin
import com.cognifide.gradle.sling.pkg.deploy.DeployTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.language.base.plugins.LifecycleBasePlugin

/**
 * Separate plugin which provides tasks for managing local instances.
 * Most often should be applied only to one project in build.
 * Applying it multiple times to same configuration could case confusing errors like Sling started multiple times.
 */
class InstancePlugin : Plugin<Project> {

    override fun apply(project: Project) {
        with(project, {
            setupDependentPlugins()
            setupTasks()
        })
    }

    private fun Project.setupDependentPlugins() {
        plugins.apply(BasePlugin::class.java)
    }

    private fun Project.setupTasks() {
        val clean = tasks.getByName(LifecycleBasePlugin.CLEAN_TASK_NAME)

        val resolve = tasks.create(ResolveTask.NAME, ResolveTask::class.java)
        val create = tasks.create(CreateTask.NAME, CreateTask::class.java)
        val destroy = tasks.create(DestroyTask.NAME, DestroyTask::class.java)
        val up = tasks.create(UpTask.NAME, UpTask::class.java)
        val down = tasks.create(DownTask.NAME, DownTask::class.java)
        val reload = tasks.create(ReloadTask.NAME, ReloadTask::class.java)
        val satisfy = tasks.create(SatisfyTask.NAME, SatisfyTask::class.java)
        val await = tasks.create(AwaitTask.NAME, AwaitTask::class.java)
        val collect = tasks.create(CollectTask.NAME, CollectTask::class.java)
        val setup = tasks.create(SetupTask.NAME, SetupTask::class.java)

        create.dependsOn(resolve).mustRunAfter(clean)
        up.dependsOn(create).mustRunAfter(clean)
        reload.mustRunAfter(satisfy)
        destroy.mustRunAfter(down)
        resolve.mustRunAfter(clean)
        satisfy.dependsOn(resolve).mustRunAfter(create, up)
        collect.mustRunAfter(satisfy)
        setup.dependsOn(create, up, satisfy, await)

        plugins.withId(PackagePlugin.ID, {
            val deploy = tasks.getByName(DeployTask.NAME)

            setup.dependsOn(deploy)
            deploy.mustRunAfter(create, up, satisfy)
            reload.mustRunAfter(deploy)
            await.mustRunAfter(deploy)
        })

        gradle.afterProject { subproject ->
            if (subproject.plugins.hasPlugin(PackagePlugin.ID)) {
                collect.dependsOn(subproject.tasks.getByName(ComposeTask.NAME))
            }
        }
    }

    companion object {
        const val ID = "com.cognifide.sling.instance"

        const val FILES_PATH = "local-instance"
    }

}