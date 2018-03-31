import kotlin.apply

// Gradle repositories and dependencies
buildscript {
    repositories {
        mavenCentral()
        jcenter()
    }
}

plugins {
    base
    java
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