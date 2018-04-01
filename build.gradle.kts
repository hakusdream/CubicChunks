import net.minecraftforge.gradle.user.patcherUser.forge.ForgeExtension
import net.minecraftforge.gradle.user.patcherUser.forge.ForgePlugin
import kotlin.apply

// Gradle repositories and dependencies
buildscript {
    repositories {
        mavenCentral()
        jcenter()
        maven {
            setUrl("http://repo.spongepowered.org/maven")
        }
        maven {
            setUrl("http://files.minecraftforge.net/maven")
        }
    }
    dependencies {
        classpath("net.minecraftforge.gradle:ForgeGradle:2.3-SNAPSHOT")
        classpath("org.spongepowered:mixingradle:0.4-SNAPSHOT")
    }
}

//it can't be named forgeVersion because ForgeExtension has property named forgeVersion
val theForgeVersion by project
val theMappingsVersion by project
val malisisCoreVersion by project
val malisisCoreMinVersion by project

allprojects {

    plugins {
        base
        java
    }
    apply {
        plugin<ForgePlugin>()
        plugin<org.spongepowered.asm.gradle.plugins.MixinGradlePlugin>()
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

    // this is needed because it.ozimov:java7-hamcrest-matchers:0.7.0 depends on guava 19, while MC needs guava 21
    configurations.all { resolutionStrategy { force("com.google.guava:guava:21.0") } }

    dependencies {
        testCompile("junit:junit:4.11")
        testCompile("org.hamcrest:hamcrest-junit:2.0.0.0")
        testCompile("it.ozimov:java7-hamcrest-matchers:0.7.0")
        testCompile("org.mockito:mockito-core:2.1.0-RC.2")
        testCompile("org.spongepowered:launchwrappertestsuite:1.0-SNAPSHOT")
    }
}

project(":cubicchunks-cubicgen") {

    dependencies {
        compileOnly(project(":cubicchunks-core", "deobfArtifacts"))
        //runtime(project(":cubicchunks-core"))
        testCompile(project(":cubicchunks-core", "testArtifacts"))
        testCompile(project(":cubicchunks-core", "deobfArtifacts"))
    }
}

dependencies {
    childProjects.forEach { t, _ ->
        compile(project(t))
    }

}