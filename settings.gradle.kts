pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
    }
}

// 契约层是独立可发布的纯 Kotlin 构建：
// - 宿主（根工程）通过 includeBuild 按 com.jason.reader:book-api 坐标复合引用；
// - 插件工程同样以 includeBuild 引用并 compileOnly。
rootProject.name = "book-api"
