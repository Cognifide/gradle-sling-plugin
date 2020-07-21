package com.cognifide.gradle.sling.pkg.tasks

import com.cognifide.gradle.sling.SlingTask
import com.cognifide.gradle.sling.sling
import com.cognifide.gradle.sling.bundle.BundlePlugin
import com.cognifide.gradle.sling.common.instance.service.pkg.Package
import com.cognifide.gradle.sling.common.pkg.PackageFileFilter
import com.cognifide.gradle.sling.common.pkg.vault.FilterFile
import com.cognifide.gradle.sling.common.pkg.vault.FilterType
import com.cognifide.gradle.sling.common.pkg.vault.VaultDefinition
import com.cognifide.gradle.sling.pkg.tasks.compose.*
import com.cognifide.gradle.common.tasks.ZipTask
import com.cognifide.gradle.common.utils.using
import org.apache.commons.lang3.StringUtils
import org.gradle.api.file.CopySpec
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.*
import org.gradle.api.tasks.bundling.Jar

@Suppress("TooManyFunctions")
open class PackageCompose : ZipTask(), SlingTask {

    final override val sling = project.sling

    @Internal
    val contentDir = sling.obj.dir { convention(sling.packageOptions.contentDir) }

    @Internal
    val jcrRootDir = sling.obj.relativeDir(contentDir, Package.JCR_ROOT)

    @Internal
    val metaDir = sling.obj.relativeDir(contentDir, Package.META_PATH)

    /**
     * Content path for OSGi bundle jars being placed in Vault package.
     */
    @Input
    val bundlePath = sling.obj.string { convention(sling.packageOptions.installPath) }

    /**
     * Controls running tests for built bundles before placing them at [bundlePath].
     */
    @Input
    val bundleTest = sling.obj.boolean {
        convention(true)
        sling.prop.boolean("package.bundleTest")?.let { set(it) }
    }

    /**
     * Allows to customize install path or run mode of built bundle to be installed.
     * Affects only project having both package and bundle plugins applied.
     */
    fun bundleBuilt(options: BundleInstalledBuilt.() -> Unit) {
        this.bundleBuiltOptions = options
    }

    private var bundleBuiltOptions: BundleInstalledBuilt.() -> Unit = {}

    /**
     * Content path for CRX sub-packages being placed in Vault package being built.
     */
    @Input
    val nestedPath = sling.obj.string { convention(sling.packageOptions.storagePath) }

    /**
     * Controls validating built packages before placing them at [nestedPath].
     */
    @Input
    val nestedValidation = sling.obj.boolean {
        convention(true)
        sling.prop.boolean("package.nestedValidation")?.let { set(it) }
    }

    /**
     * Defines properties being used to generate Vault package metadata files.
     */
    @Nested
    val vaultDefinition = VaultDefinition(sling)

    fun vaultDefinition(options: VaultDefinition.() -> Unit) {
        vaultDefinition.apply(options)
    }

    @Internal
    val vaultDir = sling.obj.relativeDir(contentDir, Package.VLT_PATH)

    @Internal
    val vaultHooksDir = sling.obj.relativeDir(contentDir, Package.VLT_HOOKS_PATH)

    @Internal
    val vaultFilterOriginFile = sling.obj.relativeFile(metaDir, "${Package.VLT_DIR}/${FilterFile.ORIGIN_NAME}")

    @Internal
    val vaultFilterFile = sling.obj.relativeFile(vaultDir, FilterFile.BUILD_NAME)

    @Internal
    val vaultFilters = sling.obj.boolean { convention(true) }

    @Internal
    val vaultNodeTypesFile = sling.obj.relativeFile(vaultDir, Package.VLT_NODETYPES_FILE)

    @Nested
    val bundlesInstalled = sling.obj.list<BundleInstalled> { convention(listOf()) }

    @Nested
    val packagesNested = sling.obj.list<PackageNested> { convention(listOf()) }

    private var definitions = mutableListOf<() -> Unit>()

    override fun projectsEvaluated() {
        super.projectsEvaluated()
        (definitions + definition).forEach { it() }
    }

    fun fromDefaults() {
        withBundleBuilt()
        withVaultFilters(vaultFilterOriginFile)

        fromMeta(metaDir)
        fromRoot(jcrRootDir)
        fromBundlesInstalled(bundlesInstalled)
        fromPackagesNested(packagesNested)
        fromVaultHooks(vaultHooksDir)
    }

    fun fromMeta(metaDir: Any) {
        into(Package.META_PATH) { spec ->
            spec.from(metaDir)
            fileFilterDelegate(spec)
        }
    }

    fun fromRoot(dir: Any) {
        into(Package.JCR_ROOT) { spec ->
            spec.from(dir)
            fileFilterDelegate(spec)
        }
    }

    fun fromBundlesInstalled(bundles: ListProperty<BundleInstalled>) = bundles.get().forEach { fromArchive(it) }

    fun fromPackagesNested(pkgs: ListProperty<PackageNested>) = pkgs.get().forEach { fromArchive(it) }

    private fun fromArchive(archive: RepositoryArchive) {
        val dirPath = archive.dirPath.map { path ->
            if (archive is BundleInstalled) {
                var installPath = StringUtils.appendIfMissing(path, "/install")
                if (archive.runMode.isPresent) {
                    installPath = "$path.${archive.runMode.get()}"
                }
                installPath
            } else {
                path
            }
        }

        if (archive.vaultFilter.get()) {
            when (archive.vaultFilterType.get()) {
                FilterType.FILE -> vaultDefinition.filter("${dirPath.get()}/${archive.fileName.get()}") { type = FilterType.FILE }
                FilterType.DIR -> vaultDefinition.filter(dirPath.get()) { type = FilterType.DIR }
                else -> Unit
            }
        }

        into("${Package.JCR_ROOT}/${dirPath.get()}") { spec ->
            spec.from(archive.file)
            fileFilterDelegate(spec)
        }
    }

    fun fromVaultHooks(dir: Any) {
        into(Package.VLT_HOOKS_PATH) { spec ->
            spec.from(dir)
            fileFilterDelegate(spec)
        }
    }

    fun withBundleBuilt() {
        if (!project.plugins.hasPlugin(BundlePlugin::class.java)) {
            return
        }

        val jar = project.tasks.named(JavaPlugin.JAR_TASK_NAME, Jar::class.java)
        dependsOn(jar)
        bundlesInstalled.add(BundleInstalledBuilt(this, jar).apply(bundleBuiltOptions))

        if (bundleTest.get()) {
            dependsOn(project.tasks.named(JavaPlugin.TEST_TASK_NAME))
        }
    }

    fun withBundlesInstalled(others: ListProperty<BundleInstalled>) {
        bundlesInstalled.addAll(others)
    }

    fun withPackagesNested(others: ListProperty<PackageNested>) {
        packagesNested.addAll(others)
    }

    fun withVaultFilters(file: RegularFileProperty) {
        vaultDefinition.filters(file)
    }

    fun withVaultNodeTypes(file: RegularFileProperty) {
        vaultDefinition.nodeTypes(file)
    }

    fun withVaultDefinition(other: VaultDefinition) {
        vaultDefinition.properties.putAll(other.properties)
        vaultDefinition.nodeTypeLibs.addAll(other.nodeTypeLibs)
        vaultDefinition.nodeTypeLines.addAll(other.nodeTypeLines)
    }

    fun mergePackageProject(projectPath: String) = mergePackage("$projectPath:$NAME")

    fun mergePackage(taskPath: String) = mergePackage(common.tasks.pathed(taskPath))

    fun mergePackage(task: TaskProvider<PackageCompose>) {
        dependsOn(task)
        definitions.add { task.get().merging(this) }
    }

    fun nestPackage(dependencyNotation: Any, options: PackageNestedResolved.() -> Unit = {}) {
        definitions.add { packagesNested.add(PackageNestedResolved(this, dependencyNotation).apply(options)) }
    }

    fun nestPackageProject(projectPath: String, options: PackageNestedBuilt.() -> Unit = {}) {
        nestPackageBuilt("$projectPath:$NAME", options)
        if (nestedValidation.get()) {
            dependsOn("$projectPath:${PackageValidate.NAME}")
        }
    }

    fun nestPackageBuilt(taskPath: String, options: PackageNestedBuilt.() -> Unit = {}) {
        nestPackageBuilt(common.tasks.pathed(taskPath), options)
    }

    fun nestPackageBuilt(task: TaskProvider<PackageCompose>, options: PackageNestedBuilt.() -> Unit = {}) {
        dependsOn(task)
        definitions.add { packagesNested.add(PackageNestedBuilt(this, task).apply(options)) }
    }

    fun installBundle(dependencyNotation: Any, options: BundleInstalledResolved.() -> Unit = {}) {
        definitions.add { bundlesInstalled.add(BundleInstalledResolved(this, dependencyNotation).apply(options)) }
    }

    fun installBundleProject(projectPath: String, options: BundleInstalledBuilt.() -> Unit = {}) {
        installBundleBuilt("$projectPath:${JavaPlugin.JAR_TASK_NAME}", options)
        if (bundleTest.get()) {
            dependsOn("$projectPath:${JavaPlugin.TEST_TASK_NAME}")
        }
    }

    fun installBundleBuilt(taskPath: String, options: BundleInstalledBuilt.() -> Unit = {}) {
        installBundleBuilt(common.tasks.pathed(taskPath), options)
    }

    fun installBundleBuilt(task: TaskProvider<Jar>, options: BundleInstalledBuilt.() -> Unit = {}) {
        dependsOn(task)
        definitions.add { bundlesInstalled.add(BundleInstalledBuilt(this, task).apply(options)) }
    }

    private var definition: () -> Unit = {
        fromDefaults()
    }

    /**
     * Override default behavior for composing this package.
     */
    fun definition(definition: () -> Unit) {
        this.definition = definition
    }

    /**
     * Clear default behavior for composing this package.
     * After calling this method, particular 'from*()' methods need to be called.
     */
    fun noDefaults() = definition {}

    private var merging: (PackageCompose) -> Unit = { other ->
        other.withVaultFilters(vaultFilterFile)
        other.withVaultNodeTypes(vaultNodeTypesFile)
        other.withVaultDefinition(vaultDefinition)
        other.withBundlesInstalled(bundlesInstalled)
        other.withPackagesNested(packagesNested)

        other.fromRoot(jcrRootDir)
        other.fromVaultHooks(vaultHooksDir)
    }

    /**
     * Override default behavior for merging this package into assembly package.
     */
    fun merging(action: (PackageCompose) -> Unit) {
        this.merging = action
    }

    /**
     * Add some extra behavior when merging this package into assembly package.
     */
    fun merged(action: (PackageCompose) -> Unit) {
        val defaultAction = this.merging
        this.merging = { other ->
            defaultAction(other)
            action(other)
        }
    }

    @Nested
    val fileFilter = PackageFileFilter(this)

    fun fileFilter(configurer: PackageFileFilter.() -> Unit) = fileFilter.using(configurer)

    @Internal
    var fileFilterDelegate: ((CopySpec) -> Unit) = { fileFilter.filter(it) }

    init {
        group = SlingTask.GROUP
        description = "Composes Vault package from JCR content and built or resolved OSGi bundles"
        archiveBaseName.set(sling.commonOptions.baseName)
        destinationDirectory.set(project.layout.buildDirectory.dir(name))
        duplicatesStrategy = DuplicatesStrategy.WARN
    }

    companion object {
        const val NAME = "packageCompose"
    }
}
