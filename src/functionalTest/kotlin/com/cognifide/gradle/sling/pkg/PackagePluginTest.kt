package com.cognifide.gradle.sling.pkg
import com.cognifide.gradle.sling.test.SlingBuildTest
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import java.io.File

@Suppress("LongMethod", "MaxLineLength")
class PackagePluginTest : SlingBuildTest() {

    @Test
    fun `should build package using minimal configuration`() {
        val projectDir = prepareProject("package-minimal") {
            settingsGradle("")

            buildGradle("""
                plugins {
                    id("com.cognifide.sling.package")
                }
                
                group = "com.company.example"
                version = "1.0.0"
                """)

            file("src/main/content/jcr_root/apps/example/.content.xml", """
                <?xml version="1.0" encoding="UTF-8"?>
                <jcr:root xmlns:sling="http://sling.apache.org/jcr/sling/1.0" xmlns:jcr="http://www.jcp.org/jcr/1.0"
                    jcr:primaryType="sling:Folder"/>
                """)

            file("src/main/content/META-INF/vault/filter.xml", """
                <?xml version="1.0" encoding="UTF-8"?>
                <workspaceFilter version="1.0">
                    <filter root="/apps/example"/>
                </workspaceFilter>
                """)
        }

        runBuild(projectDir, "packageCompose", "-Poffline") {
            assertTask(":packageCompose")

            val pkg = file("build/packageCompose/package-minimal-1.0.0.zip")

            assertPackage(pkg)

            assertZipEntry(pkg, "jcr_root/apps/example/.content.xml")

            assertZipEntryEquals(pkg, "META-INF/vault/filter.xml", """
                <?xml version="1.0" encoding="UTF-8"?>
                <workspaceFilter version="1.0">
                  <filter root="/apps/example"/>
                  
                </workspaceFilter>
            """)

            assertZipEntryEquals(pkg, "META-INF/vault/properties.xml", """
                <?xml version="1.0" encoding="UTF-8" standalone="no"?>
                <!DOCTYPE properties SYSTEM "http://java.sun.com/dtd/properties.dtd">
                <properties>
                    
                    <entry key="group">com.company.example</entry>
                    <entry key="name">package-minimal</entry>
                    <entry key="version">1.0.0</entry>
                    
                    <entry key="createdBy">${System.getProperty("user.name")}</entry>
                    
                    <entry key="acHandling">merge_preserve</entry>
                    <entry key="requiresRoot">false</entry>
                    
                </properties>
            """)

            assertZipEntryEquals(pkg, "META-INF/MANIFEST.MF", """
                Manifest-Version: 1.0
                Content-Package-Roots: /apps/example
                Content-Package-Type: mixed
                Content-Package-Id: com.company.example:package-minimal:1.0.0
            """)
        }

        runBuild(projectDir, "packageValidate", "-Poffline") {
            assertTask(":packageCompose", TaskOutcome.UP_TO_DATE)
            assertTask(":packageValidate")
        }
    }

    @Test
    fun `should build assembly package with content and bundles merged`() {
        val projectDir = prepareProject("package-assembly") {
            settingsGradle("""
                rootProject.name = "example"
                
                include("ui.apps")
                include("ui.content") 
                include("assembly")
            """)

            gradleProperties("""
                version=1.0.0
            """)

            file("assembly/build.gradle.kts", """
                plugins {
                    id("com.cognifide.sling.package")
                    id("maven-publish")
                }
                
                group = "com.company.example.sling"
                
                tasks {
                    packageCompose {
                        mergePackageProject(":ui.apps")
                        mergePackageProject(":ui.content")
                    }
                }

                publishing {
                    repositories {
                        maven(rootProject.file("build/repository"))
                    }
                
                    publications {
                        create<MavenPublication>("maven") {
                            artifact(common.publicationArtifact("packageCompose"))
                        }
                    }
                }
            """)

            uiApps()
            uiContent()
        }

        runBuild(projectDir, ":assembly:packageCompose", "-Poffline") {
            assertTask(":assembly:packageCompose")

            val pkg = file("assembly/build/packageCompose/example-assembly-1.0.0.zip")

            assertPackage(pkg)

            // Check if bundle was build in sub-project
            assertBundle("ui.apps/build/libs/example-ui.apps-1.0.0.jar")
            assertZipEntryEquals("ui.apps/build/libs/example-ui.apps-1.0.0.jar", "OSGI-INF/com.company.example.sling.HelloService.xml", """
                <?xml version="1.0" encoding="UTF-8"?>
                <scr:component xmlns:scr="http://www.osgi.org/xmlns/scr/v1.3.0" name="com.company.example.sling.HelloService" immediate="true" activate="activate" deactivate="deactivate">
                  <service>
                    <provide interface="com.company.example.sling.HelloService"/>
                  </service>
                  <implementation class="com.company.example.sling.HelloService"/>
                </scr:component>
            """)

            // Check assembled package
            assertZipEntryEquals(pkg, "META-INF/vault/filter.xml", """
                <?xml version="1.0" encoding="UTF-8"?>
                <workspaceFilter version="1.0">
                  <filter root="/apps/example/ui.apps"/>
                  <filter root="/content/example"/>
                  
                </workspaceFilter>
            """)

            assertZipEntryEquals(pkg, "META-INF/vault/properties.xml", """
                <?xml version="1.0" encoding="UTF-8" standalone="no"?>
                <!DOCTYPE properties SYSTEM "http://java.sun.com/dtd/properties.dtd">
                <properties>
                    
                    <entry key="group">com.company.example.sling</entry>
                    <entry key="name">example-assembly</entry>
                    <entry key="version">1.0.0</entry>
                    
                    <entry key="createdBy">${System.getProperty("user.name")}</entry>
                    
                    <entry key="acHandling">merge_preserve</entry>
                    <entry key="requiresRoot">false</entry>
                    <entry key="ui.apps.merged">test</entry>
                    
                </properties>
            """)

            assertZipEntry(pkg, "jcr_root/content/example/.content.xml")
            assertZipEntry(pkg, "jcr_root/apps/example/ui.apps/install/example-ui.apps-1.0.0.jar")
            assertZipEntryMatching(pkg, "META-INF/vault/nodetypes.cnd", """
                <'apps'='http://apps.com/apps/1.0'>
                <'content'='http://content.com/content/1.0'>
                *
                [apps:Folder] > nt:folder
                  - * (undefined) multiple
                  - * (undefined)
                  + * (nt:base) = apps:Folder version
                *
                [content:Folder] > nt:folder
                  - * (undefined) multiple
                  - * (undefined)
                  + * (nt:base) = content:Folder version
            """)
        }

        runBuild(projectDir, ":assembly:packageValidate", "-Poffline") {
            assertTask(":assembly:packageCompose", TaskOutcome.UP_TO_DATE)
            assertTask(":assembly:packageValidate")
        }

        runBuild(projectDir, ":assembly:publish", "-Poffline") {
            val mavenDir = projectDir.resolve("build/repository/com/company/example/sling/assembly/1.0.0")
            assertFileExists(mavenDir.resolve("assembly-1.0.0.zip"))
            assertFileExists(mavenDir.resolve("assembly-1.0.0.pom"))
        }
    }

    @Test
    fun `should build package with nested bundle and subpackages from Maven repository`() {
        val projectDir = prepareProject("package-nesting-repository") {
            settingsGradle("")

            buildGradle("""
                plugins {
                    id("com.cognifide.sling.package")
                }
                
                group = "com.company.example"
                version = "1.0.0"
                
                tasks {
                    packageCompose {
                        installBundle("org.jsoup:jsoup:1.10.2")
                        installBundle("com.neva.felix:search-webconsole-plugin:1.3.0") { runMode.set("master") }
                        
                        // TODO add nesting package samples
                    }
                }
                """)

            defaultPlanJson()
        }

        runBuild(projectDir, "packageCompose", "-Poffline") {
            assertTask(":packageCompose")

            val pkg = file("build/packageCompose/package-nesting-repository-1.0.0.zip")

            assertPackage(pkg)

            assertZipEntry(pkg, "jcr_root/apps/package-nesting-repository/install/jsoup-1.10.2.jar")
            assertZipEntry(pkg, "jcr_root/apps/package-nesting-repository/install.master/search-webconsole-plugin-1.3.0.jar")

            assertZipEntryEquals(pkg, "META-INF/vault/filter.xml", """
                <?xml version="1.0" encoding="UTF-8"?>
                <workspaceFilter version="1.0">
                  <filter root="/apps/package-nesting-repository/install/jsoup-1.10.2.jar"/>
                  <filter root="/apps/package-nesting-repository/install.master/search-webconsole-plugin-1.3.0.jar"/>
                  
                </workspaceFilter>
            """)
        }

        runBuild(projectDir, "packageValidate", "-Poffline") {
            assertTask(":packageCompose", TaskOutcome.UP_TO_DATE)
            assertTask(":packageValidate")
        }
    }

    @Test
    fun `should build package with nested bundle and sub-package built by sub-projects`() {
        val projectDir = prepareProject("package-nesting-built") {
            settingsGradle("""
                rootProject.name = "example"
                
                include("ui.apps")
                include("ui.content") 
            """)

            gradleProperties("""
                version=1.0.0
            """)

            buildGradle("""
                plugins {
                    id("com.cognifide.sling.package")
                }
                
                group = "com.company.example"
                
                tasks {
                    packageCompose {
                        nestPackageProject(":ui.content")
                        installBundleProject(":ui.apps")
                    }
                }
                """)

            defaultPlanJson()

            uiApps()
            uiContent()
        }

        runBuild(projectDir, "packageCompose", "-Poffline") {
            assertTask(":packageCompose")
            assertTask(":ui.apps:jar")
            assertTask(":ui.apps:test")
            assertTask(":ui.content:packageCompose")
            assertTask(":ui.content:packageValidate")

            val pkg = file("build/packageCompose/example-1.0.0.zip")

            assertPackage(pkg)

            assertZipEntry(pkg, "jcr_root/apps/example/ui.apps/install/example-ui.apps-1.0.0.jar")
            assertZipEntry(pkg, "jcr_root/etc/packages/com.company.example/example-ui.content-1.0.0.zip")

            assertZipEntryEquals(pkg, "META-INF/vault/filter.xml", """
                <?xml version="1.0" encoding="UTF-8"?>
                <workspaceFilter version="1.0">
                  <filter root="/apps/example/ui.apps/install/example-ui.apps-1.0.0.jar"/>
                  <filter root="/etc/packages/com.company.example/example-ui.content-1.0.0.zip"/>
                  
                </workspaceFilter>
            """)
        }
    }

    private fun File.uiApps() {
        file("ui.apps/build.gradle.kts", """
            plugins {
                id("com.cognifide.sling.bundle")
                id("com.cognifide.sling.package")
            }
            
            group = "com.company.example.sling"
            
            dependencies {
                compileOnly("org.slf4j:slf4j-api:1.5.10")
                compileOnly("org.osgi:osgi.cmpn:6.0.0")
                
                testImplementation("org.junit.jupiter:junit-jupiter:5.5.2")
                testImplementation("org.apache.sling:org.apache.sling.testing.sling-mock.junit5:2.5.0")
            }
            
            tasks {
                packageCompose {
                    vaultDefinition {
                        // TODO: property("installhook.actool.class", "biz.netcentric.cq.tools.actool.installhook.AcToolInstallHook")
                    }
                    merged { assembly ->
                        assembly.vaultDefinition.property("ui.apps.merged", "test")
                    }
                }
            }
            """)

        file("ui.apps/src/main/content/META-INF/vault/nodetypes.cnd", """
            <'apps'='http://apps.com/apps/1.0'>

            [apps:Folder] > nt:folder
              - * (undefined) multiple
              - * (undefined)
              + * (nt:base) = apps:Folder version
        """)

        file("ui.apps/src/main/content/META-INF/vault/filter.xml", """
            <?xml version="1.0" encoding="UTF-8"?>
            <workspaceFilter version="1.0">
                <filter root="/apps/example/ui.apps"/>
            </workspaceFilter>
        """)

        helloServiceJava("ui.apps")
    }

    private fun File.uiContent() {
        file("ui.content/build.gradle.kts", """
            plugins {
                id("com.cognifide.sling.package")
            }
            
            group = "com.company.example"
            
            tasks {
                packageCompose {
                    vaultDefinition {
                        // TODO property("installhook.aecu.class", "de.valtech.aecu.core.installhook.AecuInstallHook")
                    }
                }
            }
        """)

        file("ui.content/src/main/content/jcr_root/content/example/.content.xml", """
            <?xml version="1.0" encoding="UTF-8"?>
            <jcr:root xmlns:sling="http://sling.apache.org/jcr/sling/1.0" xmlns:jcr="http://www.jcp.org/jcr/1.0"
                jcr:primaryType="sling:Folder"/>
        """)

        file("ui.content/src/main/content/META-INF/vault/filter.xml", """
            <?xml version="1.0" encoding="UTF-8"?>
            <workspaceFilter version="1.0">
                <filter root="/content/example"/>
            </workspaceFilter>
        """)

        file("ui.content/src/main/content/META-INF/vault/nodetypes.cnd", """
            <'content'='http://content.com/content/1.0'>

            [content:Folder] > nt:folder
              - * (undefined) multiple
              - * (undefined)
              + * (nt:base) = content:Folder version
        """)
    }

    private fun File.defaultPlanJson() {
        file("src/sling/package/OAKPAL_OPEAR/default-plan.json", """
                {
                  "checklists": [
                    "net.adamcin.oakpal.core/basic"
                  ],
                  "installHookPolicy": "SKIP",
                  "checks": [
                    {
                      "name": "basic/subpackages",
                      "config": {
                        "denyAll": false
                      }
                    }
                  ]
                } 
            """)
    }
}
