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
    // lobby 测试桩族(StubHydrokarbsApi/FakeSessionHandle)implements 框架接口、路由测试直接构造
    // SessionEventView record,compileOnly 不进测试类路径,补同文件 testImplementation —— 出自 本次任务(shdf-launchstop-流程开关)。
    testImplementation(files("../Hydrokarbs/build/libs/Hydrokarbs-3.0.0-all.jar"))

    // 角色系统经 mavenLocal 坐标参与编译,不打进产物 —— 出自 08-角色桥接 §8.8 表;README 对齐注③。
    compileOnly("com.shadowHunterRolesPlugin:ShadowHunterRolesPlugin:1.0.0")
    // 3.1 步骤1:测试类族要用假 RoleAPI 实现打桩,compileOnly 不进测试类路径,补同坐标 testImplementation —— 出自 施工计划3.1 步骤1。
    testImplementation("com.shadowHunterRolesPlugin:ShadowHunterRolesPlugin:1.0.0")
    // JUnit 5 版本对齐框架工程 Hydrokarbs 的 build.gradle.kts 惯例。
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    // junit-jupiter 聚合不带 launcher,Gradle 9 要求测试 runtime 显式在案(Hydrokarbs 靠 kotlin("test")
    // 传递获得,本工程纯 Java 无此路径);版本与 junit-jupiter 5.10.2 同系配对。
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.2")
    // compileOnly/testCompileOnly 不进 test runtime,JUnit 反射展开测试类会 NoClassDefFoundError——框架工程已踩过,
    // 补同坐标 testRuntimeOnly(不降级 compileOnly、不影响产物依赖)。
    testRuntimeOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    // 主源集 compileOnly 不传播进 testCompileClasspath,而测试桩 implements RoleAPI 须编译解析 Component/Material/
    // Player/Event——补同坐标 testCompileOnly(对齐 Hydrokarbs/build.gradle.kts:32 惯例,三件套之中间行)。
    testCompileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(21)
}

tasks {
    test {
        useJUnitPlatform()
    }

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
