import net.minecraftforge.gradle.tasks.DeobfuscateJar
import net.minecraftforge.gradle.user.ReobfMappingType
import net.minecraftforge.gradle.user.ReobfTaskFactory
import net.minecraftforge.gradle.user.patcherUser.forge.ForgeExtension
import net.minecraftforge.gradle.user.patcherUser.forge.ForgePlugin
import nl.javadude.gradle.plugins.license.LicenseExtension
import nl.javadude.gradle.plugins.license.LicensePlugin
import org.ajoberstar.grgit.Grgit
import org.ajoberstar.grgit.operation.DescribeOp
import org.gradle.api.internal.HasConvention
import org.gradle.kotlin.dsl.creating
import org.gradle.kotlin.dsl.extra
import org.gradle.plugins.ide.idea.model.IdeaModel
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
    }
}

plugins {
    base
    java
    idea
    eclipse
    maven
    signing
}

val deobfMcSRG: DeobfuscateJar by tasks
val deobfMcMCP: DeobfuscateJar by tasks

defaultTasks = listOf("licenseFormat", "build")

val theForgeVersion by project
val theMappingsVersion by project
val malisisCoreVersion by project
val malisisCoreMinVersion by project

val projectName by project

val versionSuffix by project
val versionMinorFreeze by project

val sourceSets = the<JavaPluginConvention>().sourceSets
val mainSourceSet = sourceSets["main"]!!

version = getModVersion()
group = "io.github.opencubicchunks"
(mainSourceSet as ExtensionAware).extra["refMap"] = "cubicchunks.mixins.refmap.json"

idea {
    module.apply {
        inheritOutputDirs = true
    }
    module.isDownloadJavadoc = true
    module.isDownloadSources = true
}

base {
    archivesBaseName = "CubicChunks"
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

configure<MixinExtension> {
    token("MC_FORGE", extractForgeMinorVersion())
}

signing {
    isRequired = false
    // isRequired = gradle.taskGraph.hasTask("uploadArchives")
    sign(configurations.archives)
}

// configurations, needed for extendsFrom
val forgeGradleMc by configurations
val forgeGradleMcDeps by configurations
val forgeGradleGradleStart by configurations
val compile by configurations
val testCompile by configurations
val deobfCompile by configurations

val embed by configurations.creating
val embedOnly by configurations.creating
val coreShadow by configurations.creating

testCompile.extendsFrom(forgeGradleGradleStart)
testCompile.extendsFrom(forgeGradleMcDeps)
compile.extendsFrom(embed)
embedOnly.extendsFrom(embed)
testCompile.extendsFrom(embed)
compile.extendsFrom(coreShadow)

dependencies {
    // https://mvnrepository.com/artifact/com.typesafe/config
    embed("com.typesafe:config:1.2.0")

    embed("org.spongepowered:mixin:0.7.5-SNAPSHOT") {
        isTransitive = false
    }

    embed("io.github.opencubicchunks:regionlib:0.44.0-SNAPSHOT")
}

// TODO: coremod dependency extraction

// modified version of https://github.com/PaleoCrafter/Dependency-Extraction-Example/blob/coremod-separation/build.gradle
tasks {

    // based on:
    // https://github.com/Ordinastie/MalisisCore/blob/30d8efcfd047ac9e9bc75dfb76642bd5977f0305/build.gradle#L204-L256
    // https://github.com/gradle/kotlin-dsl/blob/201534f53d93660c273e09f768557220d33810a9/samples/maven-plugin/build.gradle.kts#L10-L44
    "uploadArchives"(Upload::class) {
        repositories {
            withConvention(MavenRepositoryHandlerConvention::class) {
                mavenDeployer {
                    // Sign Maven POM
                    beforeDeployment {
                        signing.signPom(this)
                    }

                    val username = if (project.hasProperty("sonatypeUsername")) project.properties["sonatypeUsername"] else System.getenv("sonatypeUsername")
                    val password = if (project.hasProperty("sonatypePassword")) project.properties["sonatypePassword"] else System.getenv("sonatypePassword")

                    withGroovyBuilder {
                        "snapshotRepository"("url" to "https://oss.sonatype.org/content/repositories/snapshots") {
                            "authentication"("userName" to username, "password" to password)
                        }

                        "repository"("url" to "https://oss.sonatype.org/service/local/staging/deploy/maven2") {
                            "authentication"("userName" to username, "password" to password)
                        }
                    }

                    // Maven POM generation
                    pom.project {
                        withGroovyBuilder {

                            "name"(projectName)
                            "artifactId"(base.archivesBaseName.toLowerCase())
                            "packaging"("jar")
                            "url"("https://github.com/OpenCubicChunks/CubicChunks")
                            "description"("Unlimited world height mod for Minecraft")


                            "scm" {
                                "connection"("scm:git:git://github.com/OpenCubicChunks/CubicChunks.git")
                                "developerConnection"("scm:git:ssh://git@github.com:OpenCubicChunks/CubicChunks.git")
                                "url"("https://github.com/OpenCubicChunks/RegionLib")
                            }

                            "licenses" {
                                "license" {
                                    "name"("The MIT License")
                                    "url"("http://www.tldrlegal.com/license/mit-license")
                                    "distribution"("repo")
                                }
                            }

                            "developers" {
                                "developer" {
                                    "id"("Barteks2x")
                                    "name"("Barteks2x")
                                }
                                // TODO: add more developers
                            }

                            "issueManagement" {
                                "system"("github")
                                "url"("https://github.com/OpenCubicChunks/CubicChunks/issues")
                            }
                        }
                    }
                }
            }
        }
    }

    val coreJar by creating(org.gradle.api.tasks.bundling.Jar::class) {
        // need FQN because ForgeGradle needs this exact class and default imports use different one
        from(mainSourceSet.output) {
            include("io/github/opencubicchunks/cubicchunks/core/asm/**", "**.json")
        }
        // Standard coremod manifest definitions
        manifest {
            attributes["FMLAT"] = "cubicchunks_at.cfg"
            attributes["FMLCorePlugin"] = "cubicchunks.asm.CubicChunksCoreMod"
            attributes["TweakClass"] = "org.spongepowered.asm.launch.MixinTweaker"
            attributes["TweakOrder"] = "0"
            // Strictly speaking not required (right now)
            // Allows Forge to extract the dependency to a local repository (Given that the corresponding PR is merged)
            // If another mod ships the same dependency, it doesn't have to be extracted twice
            println("${project.group}:${project.base.archivesBaseName}:${project.version}:core")
            attributes["Maven-Version"] = "${project.group}:${project.base.archivesBaseName}:${project.version}:core"
        }
        configurations += listOf(coreShadow)
        classifier = "core"
    }

    configure<NamedDomainObjectContainer<ReobfTaskFactory.ReobfTaskWrapper>> {
        create("coreJar").apply {
            mappingType = ReobfMappingType.SEARGE
        }
    }

    "jar"(Jar::class) {
        exclude("LICENSE.txt", "log4j2.xml")
        into("/") {
            from(embed)
        }

        manifest.attributes["FMLAT"] = "cubicchunks_at.cfg"
        manifest.attributes["FMLCorePlugin"] = "cubicchunks.asm.CubicChunksCoreMod"
        manifest.attributes["TweakClass"] = "org.spongepowered.asm.launch.MixinTweaker"
        manifest.attributes["TweakOrder"] = "0"
        manifest.attributes["ForceLoadAsMod"] = "true"
        manifest.attributes["ContainedDeps"] =
                (embed.files.stream().map { x -> x.name }.reduce { x, y -> x + " " + y }).get() + " " + coreJar.archivePath.name

    }

    "test"(Test::class) {
        systemProperty("lwts.tweaker", "io.github.opencubicchunks.cubicchunks.tweaker.MixinTweakerServer")
        jvmArgs("-Dmixin.debug.verbose=true", //verbose mixin output for easier debugging of mixins
                "-Dmixin.checks.interfaces=true", //check if all interface methods are overriden in mixin
                "-Dmixin.env.remapRefMap=true")
        testLogging {
            showStandardStreams = true
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
    } catch(ex: RuntimeException) {
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
    return (theForgeVersion as String).split(Regex("-")).getOrNull(1)?.split(Regex("\\."))?.getOrNull(1) ?:
    throw RuntimeException("Invalid forge version format: " + theForgeVersion)
}
