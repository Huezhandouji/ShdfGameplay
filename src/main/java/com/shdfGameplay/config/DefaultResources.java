package com.shdfGameplay.config;

import com.shdfGameplay.lang.Lang;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Logger;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

/**
 * 首启资源释放器:枚举插件 jar 全部随包资源,逐文件拷至宿主数据目录(目录结构与 jar 内一致),
 * 目标已存在一律跳过不覆写——模板只补缺,已有配置/手工改动保得住。
 * 排除项:插件描述符(plugin.yml/paper-plugin.yml,其 ${version} 展开后也只服务加载器)、
 * META-INF/ 与 *.class(非数据资源)。
 * 容错口径:jar 打不开响亮(插件级基建,无从释放);单文件 IO 失败记 warning 跳过不阻启动,
 * 缺席的关键文件(config.yml/标记文件)由 GameConfigManager.init() 的响亮/空集口径兜底暴露。
 * 纯 JDK 零框架依赖;jar 路径由主类传入(JavaPlugin#getFile 为 protected,仅主类内部可及)。
 * 调用时序:onEnable 在 GameConfigManager.init() 之前调用(全量口径,覆盖 saveDefaultConfig 的单文件语义)。
 */
public final class DefaultResources {

    private static final Logger LOGGER = Logger.getLogger(DefaultResources.class.getName());

    /** 不释放的 jar 顶层描述符(插件元数据,非运行期数据文件)。 */
    private static final Set<String> EXCLUDED_FILES = Set.of("plugin.yml", "paper-plugin.yml");

    private DefaultResources() {
    }

    /**
     * 释放插件 jar 内全部随包资源到宿主数据目录,已存在不覆写。
     *
     * <p>结束时分项打一条 INFO(新拷贝/已存在跳过/失败,三项相加=随包资源总数,可对账);
     * 全部已存在且无失败的稳态启动不打日志。日志只报本次新拷贝数不报总数时,
     * 历史启动已释放过的文件(如旧版 saveDefaultConfig 写下的 config.yml)会被误读成少释放——
     * 分项口径消除该歧义。</p>
     *
     * @param pluginJar 插件 jar 路径(主类 getFile().toPath() 传入)
     * @param dataDir   宿主数据目录(getDataFolder().toPath() 传入)
     * @throws IllegalStateException jar 打不开(响亮,资源释放整体不可行)
     */
    public static void release(final Path pluginJar, final Path dataDir) {
        final List<Outcome> outcomes = new ArrayList<>();
        try (JarFile jar = new JarFile(pluginJar.toFile())) {
            jar.stream()
                    .filter(entry -> !shouldSkip(entry))
                    .forEach(entry -> outcomes.add(copyIfAbsent(jar, entry, dataDir.resolve(entry.getName()))));
        } catch (final IOException e) {
            throw new IllegalStateException(Lang.plain("config.resources.jar_read_failed",
                    Placeholder.unparsed("file", String.valueOf(pluginJar))), e);
        }
        final long copied = outcomes.stream().filter(outcome -> outcome == Outcome.COPIED).count();
        final long skipped = outcomes.stream().filter(outcome -> outcome == Outcome.ALREADY_PRESENT).count();
        final long failed = outcomes.stream().filter(outcome -> outcome == Outcome.FAILED).count();
        if (copied > 0 || failed > 0) {
            if (failed > 0) {
                LOGGER.info(Lang.plain("log.config.resources.released_with_failures",
                        Placeholder.unparsed("copied", String.valueOf(copied)),
                        Placeholder.unparsed("skipped", String.valueOf(skipped)),
                        Placeholder.unparsed("failed", String.valueOf(failed))));
            } else {
                LOGGER.info(Lang.plain("log.config.resources.released",
                        Placeholder.unparsed("copied", String.valueOf(copied)),
                        Placeholder.unparsed("skipped", String.valueOf(skipped))));
            }
        }
    }

    private static boolean shouldSkip(final JarEntry entry) {
        final String name = entry.getName();
        return entry.isDirectory()
                || name.startsWith("META-INF/")
                || name.endsWith(".class")
                || EXCLUDED_FILES.contains(name);
    }

    /** 单文件落盘结局:新拷贝/已存在跳过(不覆写)/失败(已另行 warning)。 */
    private enum Outcome {COPIED, ALREADY_PRESENT, FAILED}

    /**
     * 单文件落盘:目标已存在跳过不覆写;父目录按需创建;IO 失败 warning 跳过。
     *
     * @return 落盘结局(供 release 分项计数)
     */
    private static Outcome copyIfAbsent(final JarFile jar, final JarEntry entry, final Path target) {
        if (Files.exists(target)) {
            return Outcome.ALREADY_PRESENT;
        }
        try (InputStream in = jar.getInputStream(entry)) {
            if (in == null) {
                LOGGER.warning(Lang.plain("log.config.resources.entry_stream_missing",
                        Placeholder.unparsed("entry", entry.getName())));
                return Outcome.FAILED;
            }
            if (target.getParent() != null) {
                Files.createDirectories(target.getParent());
            }
            Files.copy(in, target);
            return Outcome.COPIED;
        } catch (final IOException e) {
            LOGGER.warning(Lang.plain("log.config.resources.copy_failed",
                    Placeholder.unparsed("target", String.valueOf(target)),
                    Placeholder.unparsed("error", String.valueOf(e.getMessage()))));
            return Outcome.FAILED;
        }
    }
}
