package com.cognifide.gradle.sling.base.vlt

import org.apache.commons.cli2.CommandLine
import org.apache.jackrabbit.vault.util.console.CliCommand
import org.apache.jackrabbit.vault.cli.VltExecutionContext as BaseExecutionContext

class VltExecutionContext(app: VltApp) : BaseExecutionContext(app) {

    override fun execute(commandLine: CommandLine): Boolean {
        commands.filterIsInstance<CliCommand>().forEach { command ->
            try {
                if (doExecute(command, commandLine)) {
                    return true
                }
            } catch (e: Exception) {
                throw VltException("Error while executing command: $command")
            }
        }

        return false
    }

}
