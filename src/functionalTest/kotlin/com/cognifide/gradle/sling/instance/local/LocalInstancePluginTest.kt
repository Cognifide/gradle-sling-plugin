package com.cognifide.gradle.sling.instance.local

import com.cognifide.gradle.sling.test.SlingBuildTest
import org.gradle.internal.impldep.org.testng.AssertJUnit.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

@Suppress("LongMethod", "MaxLineLength")
class LocalInstancePluginTest : SlingBuildTest() {

    @Test
    fun `should apply plugin correctly`() {
        val projectDir = prepareProject("local-instance-minimal") {
            settingsGradle("")

            buildGradle("""
                plugins {
                    id("com.cognifide.sling.instance.local")
                }
                """)
        }

        runBuild(projectDir, "tasks", "-Poffline") {
            assertTask(":tasks")
        }
    }

    @Test
    fun `should resolve instance files properly`() {
        val projectDir = prepareProject("instance-resolve") {
            settingsGradle("")

            file("src/sling/files/org.apache.sling.starter-11.jar", "")

            gradleProperties("""
                localInstance.starter.jarUrl=org.apache.sling:org.apache.sling.starter:11
            """)

            buildGradle("""
                plugins {
                    id("com.cognifide.sling.instance.local")
                }
                
                sling {
                    instance {
                        provisioner {
                            deployPackage("com.neva.felix:search-webconsole-plugin:1.3.0")
                        }
                    }
                }
                """)
        }

        runBuild(projectDir, "instanceResolve", "-Poffline") {
            assertTask(":instanceResolve")

            assertFileExists("build/instance/starter/org.apache.sling_org.apache.sling.starter_11.jar")
            assertPackage("build/package/wrapper/search-webconsole-plugin-1.3.0.zip")
        }
    }

    @Test
    @Disabled // TODO fix it!
    fun `should setup and backup local sling instance`() {
        val projectDir = prepareProject("local-instance-setup-and-backup") {
            gradleProperties("""
                instance.local-master.httpUrl=http://localhost:8088
                instance.local-master.type=local
                instance.local-master.jvmOpts=-server -Xmx2048m -XX:MaxPermSize=512M -Djava.awt.headless=true

                localInstance.backup.localDir=$BACKUP_DIR/local
                localInstance.backup.uploadUrl=$BACKUP_DIR/upload
                """)

            settingsGradle("")

            buildGradle("""
                plugins {
                    id("com.cognifide.sling.instance.local")
                }
                
                sling {
                    instance {
                        provisioner {
                            deployPackage("com.neva.felix:search-webconsole-plugin:1.3.0")
                        }
                    }
                }
                """)
        }

        try {
            runBuild(projectDir, "instanceStatus") {
                assertTask(":instanceStatus")
            }

            runBuild(projectDir, "instanceSetup") {
                assertTask(":instanceSetup")
                assertFileExists(file(".gradle/sling/localInstance/instance/master"))
            }

            runBuild(projectDir, "instanceStatus") {
                assertTask(":instanceStatus")
            }

            runBuild(projectDir, "instanceDown") {
                assertTask(":instanceDown")
            }

            runBuild(projectDir, "instanceBackup") {
                assertTask(":instanceBackup")

                val localBackupDir = "$BACKUP_DIR/local"
                assertFileExists(localBackupDir)
                val localBackups = files(localBackupDir, "**/*.backup.zip")
                assertEquals("Backup file should end with *.backup.zip suffix!",
                        1, localBackups.count())

                val remoteBackupDir = "$BACKUP_DIR/upload"
                assertFileExists(remoteBackupDir)
                val remoteBackups = files(remoteBackupDir, "**/*.backup.zip")
                assertEquals("Backup file should end with *.backup.zip suffix!",
                        1, remoteBackups.count())

                val localBackup = localBackups.first()
                val remoteBackup = remoteBackups.first()
                assertEquals("Local & remote backup names does not match!",
                        localBackup.name, remoteBackup.name)
                assertEquals("Local & remote backup size does not match!",
                        localBackup.length(), remoteBackup.length())
            }

            runBuild(projectDir, "instanceDestroy", "-Pforce") {
                assertTask(":instanceDown")
                assertTask(":instanceDestroy")
                assertFileNotExists(".gradle/sling/localInstance/instance/master")
            }

            runBuild(projectDir, "instanceUp") {
                assertTask(":instanceCreate")
                assertTask(":instanceUp")
                assertFileExists(".gradle/sling/localInstance/instance/master")
            }

            runBuild(projectDir, "instanceDestroy", "-Pforce") {
                assertTask(":instanceDestroy")
            }
        } finally {
            runBuild(projectDir, "instanceKill") {
                assertTask(":instanceKill")
            }
        }
    }

    companion object {
        const val BACKUP_DIR = ".gradle/sling/localInstance/backup"
    }
}
