package com.cognifide.gradle.aem.common.instance.local

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.common.file.resolver.FileResolver
import java.io.File

class InstallResolver(private val aem: AemExtension) {

    private val common = aem.common

    private val fileResolver = FileResolver(common).apply {
        aem.prop.file("localInstance.install.downloadDir")?.let { downloadDir.set(it) }
        aem.prop.list("localInstance.install.urls")?.forEachIndexed { index, url ->
            val no = index + 1
            val fileName = url.substringAfterLast("/").substringBeforeLast(".")

            group("cmd.$no.$fileName") { get(url) }
        }
    }

    fun files(configurer: FileResolver.() -> Unit) {
        fileResolver.apply(configurer)
    }

    val files: List<File> get() = fileResolver.allFiles
}
