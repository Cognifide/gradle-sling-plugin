package com.cognifide.gradle.sling.internal.file.downloader

import com.cognifide.gradle.sling.internal.Patterns
import com.cognifide.gradle.sling.internal.file.FileException
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import java.io.File
import java.net.URL

class UrlFileDownloader(val project: Project) {

    val logger: Logger = project.logger

    companion object {
        fun handles(sourceUrl: String): Boolean {
            return !sourceUrl.isNullOrBlank() && Patterns.wildcard(sourceUrl, "*://*")
        }
    }

    fun download(sourceUrl: String, targetFile: File) {
        try {
            val connection = URL(sourceUrl).openConnection()
            connection.useCaches = false

            val downloader = ProgressFileDownloader(project)
            downloader.headerSourceTarget(sourceUrl, targetFile)
            downloader.size = connection.contentLengthLong

            downloader.download(connection.getInputStream(), targetFile)
        } catch (e: Exception) {
            throw FileException("Cannot download URL '$sourceUrl' to file '$targetFile'.", e)
        }
    }

}