package com.cognifide.gradle.sling.common.instance.action

import com.cognifide.gradle.sling.SlingExtension
import com.cognifide.gradle.sling.common.instance.Instance
import com.cognifide.gradle.sling.common.instance.check.*
import com.cognifide.gradle.sling.common.instance.names
import java.util.concurrent.TimeUnit

/**
 * Awaits for unavailable local instances.
 */
class AwaitDownAction(sling: SlingExtension) : DefaultAction(sling) {

    private var timeoutOptions: TimeoutCheck.() -> Unit = {
        unavailableTime.apply {
            convention(TimeUnit.MINUTES.toMillis(1))
            sling.prop.long("instance.awaitDown.timeout.unavailableTime")?.let { set(it) }
        }
        stateTime.apply {
            convention(TimeUnit.MINUTES.toMillis(2))
            sling.prop.long("instance.awaitDown.timeout.stateTime")?.let { set(it) }
        }
        constantTime.apply {
            convention(TimeUnit.MINUTES.toMillis(10))
            sling.prop.long("instance.awaitDown.timeout.constantTime")?.let { set(it) }
        }
    }

    fun timeout(options: TimeoutCheck.() -> Unit) {
        timeoutOptions = options
    }

    private var unavailableOptions: UnavailableCheck.() -> Unit = {
        utilisationTime.apply {
            convention(TimeUnit.SECONDS.toMillis(10))
            sling.prop.long("instance.awaitDown.unavailable.utilizationTime")?.let { set(it) }
        }
    }

    fun unavailable(options: UnavailableCheck.() -> Unit) {
        unavailableOptions = options
    }

    private var unchangedOptions: UnchangedCheck.() -> Unit = {
        awaitTime.apply {
            convention(TimeUnit.SECONDS.toMillis(3))
            sling.prop.long("instance.awaitDown.unchanged.awaitTime")?.let { set(it) }
        }
    }

    fun unchanged(options: UnchangedCheck.() -> Unit) {
        unchangedOptions = options
    }

    private val runner = CheckRunner(sling).apply {
        delay.apply {
            convention(TimeUnit.SECONDS.toMillis(1))
            sling.prop.long("instance.awaitDown.delay")?.let { set(it) }
        }
        verbose.apply {
            convention(true)
            sling.prop.boolean("instance.awaitDown.verbose")?.let { set(it) }
        }
        logInstantly.apply {
            sling.prop.boolean("instance.awaitDown.logInstantly")?.let { set(it) }
        }

        checks {
            listOf(
                    timeout(timeoutOptions),
                    unavailable(unavailableOptions),
                    unchanged(unchangedOptions)
            )
        }
    }

    override fun perform(instances: Collection<Instance>) {
        if (instances.isEmpty()) {
            logger.info("No instances to await down.")
            return
        }

        logger.info("Awaiting instance(s) down: ${instances.names}")

        runner.check(instances)
    }
}
