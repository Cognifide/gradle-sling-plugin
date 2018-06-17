package com.cognifide.gradle.sling.base.vlt

import org.apache.jackrabbit.vault.cli.VaultFsApp
import org.apache.jackrabbit.vault.util.console.ExecutionContext
import org.apache.jackrabbit.vault.util.console.commands.CmdConsole
import org.gradle.api.Project

class VltApp(val project: Project) : VaultFsApp() {

    companion object {
        const val CURRENT_WORKING_DIR = "user.dir"
    }

    private val executionContext by lazy {
        val result = VltExecutionContext(this)
        result.installCommand(CmdConsole())
        result
    }

    override fun getDefaultContext(): ExecutionContext {
        return executionContext
    }

    fun execute(command: String, workingPath: String) {
        execute(command.split(" "), workingPath)
    }

    /**
     * TODO This could be potentially improved by overriding few methods of base class
     * @see VaultFsApp.init
     */
    @Synchronized
    fun execute(args: List<String>, workingPath: String) {
        val cwd = System.getProperty(CURRENT_WORKING_DIR)

        System.setProperty(CURRENT_WORKING_DIR, workingPath)
        run(args.toTypedArray())
        System.setProperty(CURRENT_WORKING_DIR, cwd)
    }
}
