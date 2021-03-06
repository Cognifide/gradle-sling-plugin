package com.cognifide.gradle.sling.common.instance.local

/**
 * Indicates backup file source.
 */
enum class BackupType {
    /**
     * File located at local file system (created by instance backup task).
     */
    LOCAL,

    /**
     * File downloaded from remote server (via file transfer).
     */
    REMOTE;

    val dirName get() = name.toLowerCase()
}
