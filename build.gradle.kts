import java.io.FileInputStream
import java.util.*

buildscript {
    repositories {
        google {
            setUrl("https://maven.aliyun.com/repository/google")
        }
        jcenter {
            setUrl("https://maven.aliyun.com/repository/public")
        }
    }
}

plugins {
    java
    application
    kotlin("jvm") version "1.3.72"
}

val descriptionProperties = Properties().apply {
    load(FileInputStream(File("src/main/resources/discription.properties")))
}

group = "org.ning1994.net_assist"
version = descriptionProperties["version.name"]!!


repositories {
    google {
        setUrl("https://maven.aliyun.com/repository/google")
    }
    jcenter {
        setUrl("https://maven.aliyun.com/repository/public")
    }
}

application {
    mainClassName = "org.ning1994.net_assist.NetAssist"
}

dependencies {
    api(kotlin("stdlib-jdk8"))
    api("no.tornado:tornadofx:1.7.20")
    api("io.netty:netty-all:4.1.51.Final")
    testImplementation("junit:junit:4.12")
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}
configurations {
    api {
        isCanBeResolved = true
        isCanBeConsumed = true
    }
}
tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    jar {
        manifest {
            attributes["Class-Path"] = configurations.api.get().joinToString(" ") { it.name }
            attributes["Main-Class"] = application.mainClassName
        }
        from(configurations.api.get().map { entry -> zipTree(entry) }) {
            exclude(
                "META-INF/MANIFEST.MF",
                "META-INF/*.SF",
                "META-INF/*.DSA",
                "META-INF/*.RSA"
            )
        }
//        from("doc/assets")
    }
}

