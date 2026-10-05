plugins {
    // 纯 Kotlin/JVM 模块：契约层零 Android 依赖，插件工程可直接 compileOnly 引用
    kotlin("jvm") version "2.4.20"
    `java-library`
    // 供 JitPack（推 GitHub 打 tag 即得 com.github.<user>:book-api:<tag>）
    // 与私有 maven（宿主的 com.jason.* 规则）发布使用
    `maven-publish`
}

group = "com.jason.reader"
version = "0.1.0"

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

kotlin {
    jvmToolchain(11)
}

repositories {
    mavenCentral()
    google()
}

dependencies {
    // 仅需 JDK 与 Kotlin 标准库；不允许引入 okhttp / Android / JSON 库，
    // 插件作者面向本模块接口与 String 编程（JSON 解析可用宿主注入的能力或自带 shading）
    api(kotlin("stdlib"))
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}
