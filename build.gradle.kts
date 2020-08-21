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

group = "org.jvm"
version = "1.0-SNAPSHOT"


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
    implementation(kotlin("stdlib-jdk8"))
    implementation("no.tornado:tornadofx:1.7.20")
    implementation("io.netty:netty-all:4.1.51.Final")
    testImplementation("junit:junit:4.12")
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}
tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
}