package com.shdfGameplay;

import io.papermc.paper.plugin.loader.PluginClasspathBuilder;
import io.papermc.paper.plugin.loader.PluginLoader;

/**
 * Paper 插件类路径加载器:维持空实现,禁止一切动态依赖加载。
 * 来源:工程现状(空模板原文件);README 对齐注②③(宿主为独立插件、禁 shade);施工计划/2.5-大厅模式与最小闭环 步骤前言(:29,禁 shade 行)。
 */
class ShdfGameplayLoader implements PluginLoader {

    @Override
    public void classloader(final PluginClasspathBuilder builder) {
        // TODO(2.5): 维持空 PluginLoader,不在 loader 加载任何库(三姊妹 jar 经 build.gradle.kts compileOnly + 服务端 plugins/ 侧共存) —— 出自 README 对齐注②③;施工计划/2.5-大厅模式与最小闭环(:29)。
    }
}
