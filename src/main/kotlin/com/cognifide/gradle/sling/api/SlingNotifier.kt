package com.cognifide.gradle.sling.api

import com.cognifide.gradle.sling.internal.notifier.DorkboxNotifier
import com.cognifide.gradle.sling.internal.notifier.JcGayNotifier
import com.cognifide.gradle.sling.internal.notifier.Notifier
import dorkbox.notify.Notify
import fr.jcgay.notification.Application
import fr.jcgay.notification.Notification
import org.apache.commons.lang3.exception.ExceptionUtils
import org.gradle.api.Project
import org.gradle.api.logging.LogLevel
import java.util.concurrent.TimeUnit

class SlingNotifier private constructor(private val project: Project) {

    private val config by lazy { SlingConfig.of(project) }

    private val notifier: Notifier by lazy { config.notificationConfig(this@SlingNotifier) }

    fun log(title: String) {
        log(title, "")
    }

    fun log(title: String, message: String) {
        log(title, message, LogLevel.INFO)
    }

    fun log(title: String, message: String, level: LogLevel) {
        project.logger.log(level, if (message.isNotBlank()) {
            "${title.removeSuffix(".")}. $message"
        } else {
            title
        })
    }

    fun default(title: String) {
        default(title, "")
    }

    fun default(title: String, message: String) {
        default(title, message, LogLevel.INFO)
    }

    fun default(title: String, message: String, level: LogLevel) {
        if (config.notificationEnabled) {
            notify(title, message, level)
        }

        log(title, message, level)
    }

    fun notify(title: String) {
        notify(title, "")
    }

    fun notify(title: String, text: String) {
        notify(title, text, LogLevel.INFO)
    }

    fun notify(title: String, text: String, level: LogLevel) {
        notifier.notify(title, text, level)
    }

    fun dorkbox(): Notifier {
        return dorkbox { darkStyle().hideAfter(TimeUnit.SECONDS.toMillis(5).toInt()) }
    }

    fun dorkbox(configurer: Notify.() -> Unit): Notifier {
        return DorkboxNotifier(project, configurer)
    }

    fun jcgay(): JcGayNotifier {
        return jcgay({ timeout(TimeUnit.SECONDS.toMillis(5)) }, {})
    }

    fun jcgay(appBuilder: Application.Builder.() -> Unit, messageBuilder: Notification.Builder.() -> Unit): JcGayNotifier {
        return JcGayNotifier(project, appBuilder, messageBuilder)
    }

    fun custom(notifier: (title: String, text: String, level: LogLevel) -> Unit): Notifier {
        return object : Notifier {
            override fun notify(title: String, text: String, level: LogLevel) {
                notifier(title, text, level)
            }
        }
    }

    fun factory(): Notifier {
        val name = project.properties["sling.notification.config"] ?: "dorkbox"

        return when (name) {
            "dorkbox" -> dorkbox()
            "jcgay" -> jcgay()
            else -> throw SlingException("Unsupported notifier: '$name'")
        }
    }

    companion object {

        const val IMAGE_PATH = "/com/cognifide/gradle/sling/META-INF/vault/definition/thumbnail.png"

        const val EXT_INSTANCE_PROP = "slingNotifier"

        /**
         * Get project specific notifier (config can vary)
         */
        fun of(project: Project): SlingNotifier {
            val props = project.extensions.extraProperties
            if (!props.has(EXT_INSTANCE_PROP)) {
                props.set(EXT_INSTANCE_PROP, setup(project))
            }

            return props.get(EXT_INSTANCE_PROP) as SlingNotifier
        }

        /**
         * Register once (for root project only) listener for notifying about build errors.
         */
        private fun setup(project: Project): SlingNotifier {
            val notifier = SlingNotifier(project)

            if (project == project.rootProject) {
                project.gradle.buildFinished {
                    if (it.failure != null) {
                        val exception = ExceptionUtils.getRootCause(it.failure)
                        val message = exception?.message ?: "no error message"

                        notifier.default("Build failure", message, LogLevel.ERROR)
                    }
                }
            }

            return notifier
        }

    }

}