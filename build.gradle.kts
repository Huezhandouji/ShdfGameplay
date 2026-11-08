plugins {
    id("java-library")
    id("xyz.jpenilla.run-paper") version "3.1.0"
}

repositories {
    // 角色系统坐标自本地 Maven 仓库解析(见下方 compileOnly 行)。
    mavenLocal()
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")

    // 本地姊妹插件：只参与编译，不打进产物。
    // 版本升级后需先在对应工程执行 build，再同步更新这里的文件名。
    compileOnly(files("../ZeoliteLib/build/libs/ZeoliteLib-1.0-SNAPSHOT.jar"))
    // Hydrokarbs 是 Kotlin 工程，-all jar 附带未重定位的 kotlin-stdlib，
    // 其 API 签名若引用 Kotlin 类型，Java 侧编译也能解析。
    compileOnly(files("../Hydrokarbs/build/libs/Hydrokarbs-3.0.0-all.jar"))

    // TODO(阶段3.1): 角色系统经 mavenLocal 坐标参与编译,仅此一处依赖新增(阶段3 测试类族就位时再补 testImplementation 同坐标——compileOnly 不进测试类路径) —— 出自 08-角色桥接 §8.8 表;README 对齐注③。
    compileOnly("com.shadowHunterRolesPlugin:ShadowHunterRolesPlugin:1.0.0")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(21)
}

tasks {
    runServer {
        // Configure the Minecraft version for our task.
        // This is the only required configuration besides applying the plugin.
        // Your plugin's jar (or shadowJar if present) will be used automatically.
        minecraftVersion("1.21.11")
        jvmArgs("-Xms2G", "-Xmx2G")
    }

    processResources {
        val props = mapOf("version" to version)
        filesMatching("plugin.yml") {
            expand(props)
        }
    }
}
