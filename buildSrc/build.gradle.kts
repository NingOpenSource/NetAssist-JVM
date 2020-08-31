// SPDX-License-Identifier: MIT OR Apache-2.0

plugins {
    `kotlin-dsl`
//    kotlin("multiplatform")
}

repositories {
    google {
        setUrl("https://maven.aliyun.com/repository/google")
    }
    jcenter {
        setUrl("https://maven.aliyun.com/repository/public")
    }
}

