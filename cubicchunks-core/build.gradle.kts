import com.github.jengelman.gradle.plugins.shadow.ShadowPlugin
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
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
        classpath("org.spongepowered:mixingradle:0.6-SNAPSHOT")
        classpath("gradle.plugin.nl.javadude.gradle.plugins:license-gradle-plugin:0.13.1")
        classpath("net.minecraftforge.gradle:ForgeGradle:2.3-SNAPSHOT")
    }
}

plugins {
    maven
}
apply {
    plugin<ShadowPlugin>()
}

val deobfMcSRG: DeobfuscateJar by tasks
val deobfMcMCP: DeobfuscateJar by tasks

defaultTasks = listOf("licenseFormat", "build")

val theForgeVersion by project
val theMappingsVersion by project
val malisisCoreVersion by project
val malisisCoreMinVersion by project

val projectName by project

val sourceSets = the<JavaPluginConvention>().sourceSets
val mainSourceSet = sourceSets["main"]!!


group = "io.github.opencubicchunks"
//(mainSourceSet as ExtensionAware).extra["refMap"] = "cubicchunks.mixins.refmap.json"

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

    embed("io.github.opencubicchunks:regionlib:0.44.0-SNAPSHOT")
}

// TODO: coremod dependency extraction

// modified version of https://github.com/PaleoCrafter/Dependency-Extraction-Example/blob/coremod-separation/build.gradle
tasks {

    // TODO: move to root build.gradle.kts
    // based on:
    // https://github.com/Ordinastie/MalisisCore/blob/30d8efcfd047ac9e9bc75dfb76642bd5977f0305/build.gradle#L204-L256
    // https://github.com/gradle/kotlin-dsl/blob/201534f53d93660c273e09f768557220d33810a9/samples/maven-plugin/build.gradle.kts#L10-L44
    "uploadArchives"(Upload::class) {
        repositories {
            withConvention(MavenRepositoryHandlerConvention::class) {
                mavenDeployer {
                    // Sign Maven POM
                    //beforeDeployment {
                    //    signing.signPom(this)
                    //}

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

    val coreMixinJar = "coreMixinJar"(ShadowJar::class) {
        from(mainSourceSet.output) {
            include("io/github/opencubicchunks/cubicchunks/core/asm/**", "**.json")
        }
        configurations = listOf(compile)
        dependencies {
            include(dependency("org.spongepowered:mixin"))
        }
        // Standard coremod manifest definitions
        manifest {
            attributes["FMLAT"] = "cubicchunks_at.cfg"
            attributes["FMLCorePlugin"] = "cubicchunks.asm.CubicChunksCoreMod"
            attributes["TweakClass"] = "org.spongepowered.asm.launch.MixinTweaker"
            attributes["TweakOrder"] = "0"
            attributes["ForceLoadAsMod"] = "true"
            // Strictly speaking not required (right now)
            // Allows Forge to extract the dependency to a local repository (Given that the corresponding PR is merged)
            // If another mod ships the same dependency, it doesn't have to be extracted twice
            println("${project.group}:${project.base.archivesBaseName}:${project.version}:core")
            attributes["Maven-Version"] = "${project.group}:${project.base.archivesBaseName}:${project.version}:core"
        }
        configurations = configurations.plus(listOf(coreShadow))
        classifier = "core"
    }

    configure<NamedDomainObjectContainer<ReobfTaskFactory.ReobfTaskWrapper>> {
        create("coreMixinJar").apply {
            mappingType = ReobfMappingType.SEARGE
        }
    }
    "assemble"().dependsOn("reobfCoreMixinJar")

    "jar"(Jar::class) {
        exclude("io/github/opencubicchunks/cubicchunks/core/asm/**", "**.json", "LICENSE.txt", "log4j2.xml")

        manifest {
        }
        classifier = "mod"
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

    val mainModArtifacts by configurations.creating
    mainModArtifacts.extendsFrom(embed)
    val coreModArtifacts by configurations.creating

    artifacts {
        withGroovyBuilder {
            "mainModArtifacts"(tasks["jar"])
            "coreModArtifacts"(tasks["coreMixinJar"])
        }
    }
}
