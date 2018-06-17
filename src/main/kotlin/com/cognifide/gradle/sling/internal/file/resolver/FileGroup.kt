package com.cognifide.gradle.sling.internal.file.resolver

import java.io.File

open class FileGroup(val resolver: FileResolver, val name: String) {

    protected val _resolutions = mutableListOf<FileResolution>()

    val resolutions: List<FileResolution>
        get() = _resolutions.toList()

    val files: List<File>
        get() = _resolutions.map { it.file }


    val dirs: List<File>
        get() = _resolutions.map { it.dir }

    protected open fun createResolution(id: String, resolver: (FileResolution) -> File) = FileResolution(this, id, resolver)

    fun resolve(id: String, resolver: (FileResolution) -> File) {
        _resolutions += createResolution(id, resolver)
    }

}