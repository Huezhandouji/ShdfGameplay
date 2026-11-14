package com.shdfGameplay.config;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Logger;

/**
 * mode_config/ 配置读取与缓存管理器:init() 一次性扫描数据目录逐文件解析缓存,按 modeId 归组。
 * 容错口径:单文件解析失败跳过并记 warning 日志,不阻整体;重复 init() 整体换缓存(重载口径)。
 * 一模式可挂多图:byMode 取该模式全部图、all 展平给投票候选集、find 定位单图。
 */
public final class GameConfigManager {

    private static final Logger LOGGER = Logger.getLogger(GameConfigManager.class.getName());

    private final Path dataDir;
    private Map<String, List<GameConfig>> configsByModeId = Map.of();

    /**
     * @param dataDir 宿主数据目录(plugins/ShdfGameplay/,mode_config/ 相对其解析)
     */
    public GameConfigManager(final Path dataDir) {
        this.dataDir = dataDir;
    }

    /**
     * 一次性读取全部图配置并整体换缓存。
     *
     * <p>mode_config/ 目录缺失=空集不炸;单文件解析失败跳过并记 warning 日志(带病文件不入集);
     * 仅目录级 IO 失败响亮。</p>
     *
     * @throws IllegalStateException mode_config 目录扫描 IO 失败
     */
    public void init() {
        final Path configRoot = this.dataDir.resolve("mode_config");
        if (!Files.isDirectory(configRoot)) {
            this.configsByModeId = Map.of();
            return;
        }
        final Map<String, List<GameConfig>> byMode = new TreeMap<>();
        try (DirectoryStream<Path> modeDirs = Files.newDirectoryStream(configRoot, Files::isDirectory)) {
            for (final Path modeDir : modeDirs) {
                final String modeId = modeDir.getFileName().toString();
                final List<GameConfig> configs = new ArrayList<>();
                try (DirectoryStream<Path> files = Files.newDirectoryStream(modeDir, "*.{yaml,yml}")) {
                    for (final Path file : files) {
                        try {
                            configs.add(GameConfig.from(modeId, file));
                        } catch (final IllegalStateException e) {
                            LOGGER.warning("跳过图配置: " + e.getMessage());
                        }
                    }
                }
                configs.sort(Comparator.comparing(GameConfig::mapId));
                if (!configs.isEmpty()) {
                    byMode.put(modeId, List.copyOf(configs));
                }
            }
        } catch (final IOException e) {
            throw new IllegalStateException("mode_config 目录扫描失败: " + configRoot, e);
        }
        this.configsByModeId = Collections.unmodifiableMap(new TreeMap<>(byMode));
    }

    /**
     * 取某模式的全部图配置。
     *
     * @param modeId 模式 id(目录名,如 bosswar)
     * @return 该模式图配置列表(按 mapId 排序);模式缺席为空列表
     */
    public List<GameConfig> byMode(final String modeId) {
        return this.configsByModeId.getOrDefault(modeId, List.of());
    }

    /**
     * 全部已缓存图配置(模式按 id 排序、模式内按 mapId 排序)。
     *
     * @return 只读集合
     */
    public Collection<GameConfig> all() {
        return this.configsByModeId.values().stream().flatMap(List::stream).toList();
    }

    /**
     * 已配置模式 id 集(mode_config/ 目录名,排序稳定)。
     *
     * @return 只读集合
     */
    public Set<String> modeIds() {
        return this.configsByModeId.keySet();
    }

    /**
     * 按 modeId+mapId 定位单图配置(投票候选行 mode@map 拆解后的回查口,GUI 展示键经此读取)。
     *
     * @param modeId 模式 id(目录名,如 bosswar)
     * @param mapId  图 id(文件名去扩展名,如 example)
     * @return 该图配置;缺席为空
     */
    public Optional<GameConfig> find(final String modeId, final String mapId) {
        return byMode(modeId).stream()
                .filter(config -> config.mapId().equals(mapId))
                .findFirst();
    }
}
