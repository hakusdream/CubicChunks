import net.minecraftforge.gradle.user.patcherUser.forge.ForgeExtension
import net.minecraftforge.gradle.user.patcherUser.forge.ForgePlugin
import nl.javadude.gradle.plugins.license.LicenseExtension
import nl.javadude.gradle.plugins.license.LicensePlugin
import org.gradle.api.internal.HasConvention
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

//it can't be named forgeVersion because ForgeExtension has property named forgeVersion
val theForgeVersion by project
val theMappingsVersion by project
val malisisCoreVersion by project
val malisisCoreMinVersion by project

val licenseYear by project
val projectName by project

allprojects {
    plugins {
        base
        java
    }

    apply {
        plugin<ForgePlugin>()
        plugin<MixinGradlePlugin>()
        plugin<LicensePlugin>()
    }

    val sourceSets = the<JavaPluginConvention>().sourceSets

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
                "-Dfml.coreMods.load=cubicchunks.asm.CubicChunksCoreMod", //the core mod class, needed for mixins
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
    }

    tasks {
        val jar = "jar"(Jar::class)

        "build"().dependsOn("reobfJar")

        "javadoc"(Javadoc::class) {
            (options as StandardJavadocDocletOptions).tags = listOf("reason")
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
    }
}

configure<ForgeExtension> {
    subprojects.forEach {
        it.the<JavaPluginConvention>().sourceSets["main"].resources.asFileTree.filter { it.name.endsWith("_at.cfg") }.forEach { at(it) }
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

dependencies {
    val compile by configurations

    childProjects.forEach { t, _ ->
        compile(project(t))
    }
}