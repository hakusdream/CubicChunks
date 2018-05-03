import com.github.jengelman.gradle.plugins.shadow.ShadowPlugin
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import net.minecraftforge.gradle.user.ReobfTaskFactory
import net.minecraftforge.gradle.user.TaskSingleReobf
import net.minecraftforge.gradle.user.patcherUser.forge.ForgeExtension
import net.minecraftforge.gradle.user.patcherUser.forge.ForgePlugin
import nl.javadude.gradle.plugins.license.LicenseExtension
import nl.javadude.gradle.plugins.license.LicensePlugin
import org.ajoberstar.grgit.Grgit
import org.ajoberstar.grgit.operation.DescribeOp
import org.gradle.api.internal.HasConvention
import org.gradle.api.internal.artifacts.dependencies.DefaultProjectDependency
import org.spongepowered.asm.gradle.plugins.MixinExtension
import org.spongepowered.asm.gradle.plugins.MixinGradlePlugin
import kotlin.apply

// Gradle repositories and dependencies
buildscript {
    repositories {
        mavenCentral()
        jcenter()
        maven {
            setUrl("http://files.minecraftforge.net/maven")
        }
        maven {
            setUrl("http://repo.spongepowered.org/maven")
        }
        maven {
            setUrl("https://plugins.gradle.org/m2/")
        }
    }
    dependencies {
        classpath("org.ajoberstar:grgit:2.0.0-milestone.1")
        classpath("org.spongepowered:mixingradle:0.4-SNAPSHOT")
        classpath("gradle.plugin.nl.javadude.gradle.plugins:license-gradle-plugin:0.13.1")
        classpath("net.minecraftforge.gradle:ForgeGradle:2.3-SNAPSHOT")
        classpath("com.github.jengelman.gradle.plugins:shadow:2.0.3")
    }
}

plugins {
    base
    java
    id("com.dorongold.task-tree") version "1.3"
}

apply {
    plugin<ForgePlugin>()
    plugin<LicensePlugin>()
    plugin<ShadowPlugin>()
}

//it can't be named forgeVersion because ForgeExtension has property named forgeVersion
val theForgeVersion by project
val theMappingsVersion by project
val malisisCoreVersion by project
val malisisCoreMinVersion by project

val licenseYear by project
val projectName by project

val versionSuffix by project
val versionMinorFreeze by project

val minecraft = the<ForgeExtension>()

dependencies {
    val compile by configurations

    childProjects.forEach { t, _ ->
        compile(project(t))
    }
}

version = getModVersion()

fun getMcVersion(): String {
    if (minecraft.version == null) {
        return (theForgeVersion as String).split("-")[0]
    }
    return minecraft.version
}

//returns version string according to this: http://mcforge.readthedocs.org/en/latest/conventions/versioning/
//format: MCVERSION-MAJORMOD.MAJORAPI.MINOR.PATCH(-final/rcX/betaX)
//rcX and betaX are not implemented yet
fun getModVersion(): String {
    return try {
        val git = Grgit.open()
        val describe = DescribeOp(git.repository).call()
        val branch = getGitBranch(git)
        val snapshotSuffix = if (project.hasProperty("doRelease")) "" else "-SNAPSHOT"
        getModVersion(describe, branch) + snapshotSuffix;
    } catch (ex: RuntimeException) {
        logger.error("Unknown error when accessing git repository! Are you sure the git repository exists?", ex)
        String.format("%s-%s.%s.%s%s%s", getMcVersion(), "9999", "9999", "9999", "", "NOVERSION")
    }
}

fun getGitBranch(git: Grgit): String {
    var branch: String = git.branch.current.name
    if (branch.equals("HEAD")) {
        branch = when {
            System.getenv("TRAVIS_BRANCH")?.isEmpty() == false -> // travis
                System.getenv("TRAVIS_BRANCH")
            System.getenv("GIT_BRANCH")?.isEmpty() == false -> // jenkins
                System.getenv("GIT_BRANCH")
            System.getenv("BRANCH_NAME")?.isEmpty() == false -> // ??? another jenkins alternative?
                System.getenv("BRANCH_NAME")
            else -> throw RuntimeException("Found HEAD branch! This is most likely caused by detached head state! Will assume unknown version!")
        }
    }

    if (branch.startsWith("origin/")) {
        branch = branch.substring("origin/".length)
    }
    return branch
}

fun getModVersion(describe: String, branch: String): String {
    if (branch.startsWith("MC_")) {
        val branchMcVersion = branch.substring("MC_".length)
        if (branchMcVersion != getMcVersion()) {
            logger.warn("Branch version different than project MC version! MC version: " +
                    getMcVersion() + ", branch: " + branch + ", branch version: " + branchMcVersion)
        }
    }

    //branches "master" and "MC_something" are not appended to version sreing, everything else is
    //only builds from "master" and "MC_version" branches will actually use the correct versioning
    //but it allows to distinguish between builds from different branches even if version number is the same
    val branchSuffix = if (branch == "master" || branch.startsWith("MC_")) "" else ("-" + branch.replace("[^a-zA-Z0-9.-]", "_"))

    val baseVersionRegex = "v[0-9]+\\.[0-9]+"
    val unknownVersion = String.format("%s-UNKNOWN_VERSION%s%s", getMcVersion(), versionSuffix, branchSuffix)
    if (!describe.contains('-')) {
        //is it the "vX.Y" format?
        if (describe.matches(Regex(baseVersionRegex))) {
            return String.format("%s-%s.0.0%s%s", getMcVersion(), describe, versionSuffix, branchSuffix)
        }
        logger.error("Git describe information: \"$describe\" in unknown/incorrect format")
        return unknownVersion
    }
    //Describe format: vX.Y-build-hash
    val parts = describe.split("-")
    if (!parts[0].matches(Regex(baseVersionRegex))) {
        logger.error("Git describe information: \"$describe\" in unknown/incorrect format")
        return unknownVersion
    }
    if (!parts[1].matches(Regex("[0-9]+"))) {
        logger.error("Git describe information: \"$describe\" in unknown/incorrect format")
        return unknownVersion
    }
    val mcVersion = getMcVersion()
    val modAndApiVersion = parts[0].substring(1)
    //next we have commit-since-tag
    val commitSinceTag = Integer.parseInt(parts[1])

    val minorFreeze = if ((versionMinorFreeze as String).isEmpty()) -1 else Integer.parseInt(versionMinorFreeze as String)

    val minor = if (minorFreeze < 0) commitSinceTag else minorFreeze
    val patch = if (minorFreeze < 0) 0 else (commitSinceTag - minorFreeze)

    return String.format("%s-%s.%d.%d%s%s", mcVersion, modAndApiVersion, minor, patch, versionSuffix, branchSuffix)
}

fun extractForgeMinorVersion(): String {
    // version format: MC_VERSION-MAJOR.MINOR.?.BUILD
    return (theForgeVersion as String).split(Regex("-")).getOrNull(1)?.split(Regex("\\."))?.getOrNull(1)
            ?: throw RuntimeException("Invalid forge version format: " + theForgeVersion)
}

allprojects {
    apply {
        plugin<ForgePlugin>()
    }

    repositories {
        mavenLocal()
        mavenCentral()
        jcenter()
        maven {
            setUrl("https://oss.sonatype.org/content/repositories/public/")
        }
        maven {
            setUrl("http://repo.spongepowered.org/maven")
        }
    }

    configure<ForgeExtension> {
        version = theForgeVersion as String
        runDir = "run"
        mappings = theMappingsVersion as String

        isUseDepAts = true

        replace("@@VERSION@@", project.version)
        replace("\"/*@@DEPS_PLACEHOLDER@@*/", ";after:malisiscore@[$malisisCoreMinVersion,)\"")
        replace("@@MALISIS_VERSION@@", malisisCoreMinVersion)
        replaceIn("cubicchunks/CubicChunks.java")

        val args = listOf(
                // the core mod classes, needed for mixins
                "-Dfml.coreMods.load=cubicchunks.asm.CubicChunksCoreMod,io.github.opencubicchunks.cubicchunks.core.asm.CubicGenCoreMod",
                "-Dmixin.env.compatLevel=JAVA_8", //needed to use java 8 when using mixins
                "-Dmixin.debug.verbose=true", //verbose mixin output for easier debugging of mixins
                "-Dmixin.debug.export=true", //export classes from mixin to runDirectory/.mixin.out
                "-Dcubicchunks.debug=true", //various debug options of cubic chunks mod. Adds items that are not normally there!
                "-XX:-OmitStackTraceInFastThrow", //without this sometimes you end up with exception with empty stacktrace
                "-Dmixin.checks.interfaces=true", //check if all interface methods are overriden in mixin
                "-Dfml.noGrab=false", //change to disable Minecraft taking control over mouse
                "-ea", //enable assertions
                "-da:io.netty..." //disable netty assertions because they sometimes fail
        )

        clientJvmArgs.addAll(args)
        serverJvmArgs.addAll(args)
    }
}

subprojects {

    plugins {
        base
        java
        signing
    }

    apply {
        plugin<MixinGradlePlugin>()
        plugin<LicensePlugin>()
        plugin<ShadowPlugin>()
    }
    val minecraft = the<ForgeExtension>()

    val sourceSets = the<JavaPluginConvention>().sourceSets
    val mainSourceSet = sourceSets["main"]!!

    configure<JavaPluginConvention> {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    configure<MixinExtension> {
        token("MC_FORGE", extractForgeMinorVersion())
    }

    configure<LicenseExtension> {
        val ext = (this as HasConvention).convention.extraProperties
        ext["project"] = projectName
        ext["year"] = licenseYear
        exclude("**/*.info")
        exclude("**/package-info.java")
        exclude("**/*.json")
        exclude("**/*.xml")
        exclude("assets/*")
        exclude("io/github/opencubicchunks/cubicchunks/core/server/chunkio/async/forge/*") // Taken from forge
        header = file("HEADER.txt")
        ignoreFailures = false
        strictCheck = true
        mapping(mapOf("java" to "SLASHSTAR_STYLE"))
    }

    val compile by configurations
    val testCompile by configurations

    // for unit test dependencies
    val testArtifacts by configurations.creating
    val deobfArtifacts by configurations.creating
    deobfArtifacts.extendsFrom(compile)

    // this is needed because it.ozimov:java7-hamcrest-matchers:0.7.0 depends on guava 19, while MC needs guava 21
    configurations.all { resolutionStrategy { force("com.google.guava:guava:21.0") } }

    dependencies {
        testCompile("junit:junit:4.11")
        testCompile("org.hamcrest:hamcrest-junit:2.0.0.0")
        testCompile("it.ozimov:java7-hamcrest-matchers:0.7.0")
        testCompile("org.mockito:mockito-core:2.1.0-RC.2")
        testCompile("org.spongepowered:launchwrappertestsuite:1.0-SNAPSHOT")
        compile("org.spongepowered:mixin:0.7.5-SNAPSHOT") {
            isTransitive = false
        }
    }

    tasks {
        val jar by tasks
        "assemble"().dependsOn("reobfJar")

        "javadoc"(Javadoc::class) {
            (options as StandardJavadocDocletOptions).tags = listOf("reason")
        }

        "signing"(Sign::class) {
            isRequired = false
            // isRequired = gradle.taskGraph.hasTask("uploadArchives")
            sign(configurations.archives)
        }
        val javadocJar by creating(Jar::class) {
            classifier = "javadoc"
            from(tasks["javadoc"])
        }
        val sourcesJar by creating(Jar::class) {
            classifier = "sources"
            from(sourceSets["main"].java.srcDirs)
        }
        // for project dependency to work correctly
        val deobfJar by creating(Jar::class) {
            classifier = "deobf"
            from(sourceSets["main"].output)
        }
        // tests jar used as test dependency to use by other modules
        val testsJar by creating(Jar::class) {
            classifier = "tests"
            from(sourceSets["test"].output)
        }


        // tasks must be before artifacts, don't change the order
        artifacts {
            withGroovyBuilder {
                "testArtifacts"(testsJar)
                "deobfArtifacts"(deobfJar)
                "archives"(jar, sourcesJar, javadocJar)
            }
        }

        "processResources"(ProcessResources::class) {
            // this will ensure that this task is redone when the versions change.
            inputs.property("version", project.version)
            inputs.property("mcversion", minecraft.version)

            // replace stuff in mcmod.info, nothing else
            from(mainSourceSet.resources.srcDirs) {
                include("mcmod.info")

                // replace version and mcversion
                expand(mapOf("version" to project.version, "mcversion" to minecraft.version))
            }

            // copy everything else, thats not the mcmod.info
            from(mainSourceSet.resources.srcDirs) {
                exclude("mcmod.info")
            }
        }


        val writeModVersion by creating {
            file("VERSION").writeText("VERSION=" + version)
        }
        "build"().dependsOn(writeModVersion)
    }
}

project(":cubicchunks-cubicgen") {
    dependencies {
        val compileOnly by configurations
        val testCompile by configurations
        // no runtime dependency because cubicgen never runs alone, this root project depends on both core and cubicgen and IDE runs this
        //compileOnly(project(":cubicchunks-core", "deobfArtifacts"))
        compileOnly(project(":cubicchunks-api", "deobfArtifacts"))
        testCompile(project(":cubicchunks-core", "testArtifacts"))
        testCompile(project(":cubicchunks-core", "deobfArtifacts"))
        testCompile(project(":cubicchunks-api", "deobfArtifacts"))
    }
}

project(":cubicchunks-core") {
    dependencies {
        val compileOnly by configurations
        val testCompile by configurations
        // no runtime dependency because cubicgen never runs alone, this root project depends on both core and cubicgen and IDE runs this
        compileOnly(project(":cubicchunks-api", "deobfArtifacts"))
        testCompile(project(":cubicchunks-api", "deobfArtifacts"))
    }
}

minecraft.apply {
    subprojects.forEach {
        atSource(it.the<JavaPluginConvention>().sourceSets["main"])
    }
}

val embed by configurations.creating
val shadeEmbed by configurations.creating
val mainJar by configurations.creating
val shade by configurations.creating
shade.extendsFrom(mainJar)

dependencies {
    mainJar(project(":cubicchunks-core", "coreModArtifacts"))
    embed(project(":cubicchunks-core", "mainModArtifacts"))
    embed(project(":cubicchunks-api"))
    embed(project(":cubicchunks-cubicgen"))

    shade(project(":cubicchunks-core", "mainModArtifacts"))
    shade(project(":cubicchunks-api"))
    shadeEmbed(project(":cubicchunks-cubicgen", "shadeJarAtrifact"))
}

project(":") {
    fun Jar.setupManifest(embed: Configuration) {
        manifest {
            attributes["FMLAT"] = "cubicchunks_at.cfg"
            attributes["FMLCorePlugin"] = "io.github.opencubicchunks.cubicchunks.core.asm.CubicChunksCoreMod"
            attributes["TweakClass"] = "org.spongepowered.asm.launch.MixinTweaker"
            attributes["TweakOrder"] = "0"
            attributes["ForceLoadAsMod"] = "true"
            // LazyToString is a workaround to gradle attempting to resolve the configurations now,
            // before ForgeGradle does it's magic to make deobfCompile dependencies in subprojects work
            attributes["ContainedDeps"] = LazyToString { embed.files.joinToString(" ") { it.name } }
            attributes["Maven-Version"] = "${project.group}:${project.base.archivesBaseName}:${project.version}:core"
        }
    }

    tasks {
        // Note: this jar doesn't work yet, waiting for Mixin to make it work
        // create another jar task so that reobfJar doesn't run twice on the same files
        // because we include files from already reobfuscated jar from a subproject
        val depExtJar by creating(Jar::class) {
            dependsOn(":cubicchunks-core:reobfCoreMixinJar")
            from(zipTree(mainJar.files.single()))
            into("/") {
                from(embed).exclude { it.name.toLowerCase().contains("dummy") }
            }

            setupManifest(embed)
            classifier = "all-depext"
        }
        val shadeJarBase by creating(ShadowJar::class) {
            dependsOn(
                    ":cubicchunks-core:reobfCoreMixinJar",
                    ":cubicchunks-core:reobfJar",
                    ":cubicchunks-api:reobfJar"
            )
            exclude("META-INF/MUMFREY.*")
            configurations = listOf(shade)
            classifier = "shade-base"
        }
        // this needs to be separate from the shadeJarBase task because ShadowJar doesn't support embedding jars inside jars
        val shadeJarDepExt by creating(Jar::class) {
            dependsOn(shadeJarBase, ":cubicchunks-cubicgen:reobfShadeJar")
            from(zipTree(shadeJarBase.archivePath))
            into("/") {
                from(shadeEmbed).exclude { it.name.toLowerCase().contains("dummy") }
            }
            setupManifest(shadeEmbed)
            classifier = "shade-all"
        }
        "assemble"().dependsOn(depExtJar, shadeJarDepExt)
        // and make the original jar as empty as possible, we won't use it
        "jar"(Jar::class) {
            exclude("*")
            archiveName = "dummyOutput.jar"
        }
    }
}
class LazyToString(val obj: () -> Any) {
    override fun toString(): String {
        return obj().toString()
    }
}