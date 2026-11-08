package com.shdfGameplay.map;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * mode_config/ 配置目录自解析结果容器:数据目录下 &lt;模式&gt;/&lt;图&gt;.yaml 逐文件解析,按 modeId 归组。
 *
 * <p>一模式可挂多图:byMode 取该模式全部图,all 展平给投票候选集;
 * MatchBootstrap(开局判定)与 LobbyVote(投票候选集)的数据源。</p>
 *
 * <p>来源:资源目录 mode_config/ 现行结构;04-大厅模式 §4.3 字段;README 对齐注⑧⑩适用项</p>
 */
public final class GameConfigs {

    private final Map<String, List<GameConfig>> configsByModeId;

    private GameConfigs(final Map<String, List<GameConfig>> configsByModeId) {
        this.configsByModeId = configsByModeId;
    }

    /**
     * 扫描宿主数据目录下 mode_config/&lt;模式&gt;/&lt;图&gt;.yaml,构建配置容器。
     *
     * @param dataDir 宿主数据目录(plugins/ShdfGameplay/)
     * @return 扫描结果容器;目录缺失时为空集不炸
     * @throws IllegalStateException 任一 yaml 解析失败(空/语法错误/缺 map_path,GameConfig.from 响亮)或目录扫描 IO 失败
     */
    public static GameConfigs scan(final Path dataDir) {
        final Path configRoot = dataDir.resolve("mode_config");
        if (!Files.isDirectory(configRoot)) {
            return new GameConfigs(Map.of());
        }
        final Map<String, List<GameConfig>> byMode = new TreeMap<>();
        try (DirectoryStream<Path> modeDirs = Files.newDirectoryStream(configRoot, Files::isDirectory)) {
            for (final Path modeDir : modeDirs) {
                final String modeId = modeDir.getFileName().toString();
                final List<GameConfig> configs = new ArrayList<>();
                try (DirectoryStream<Path> files = Files.newDirectoryStream(modeDir, "*.{yaml,yml}")) {
                    for (final Path file : files) {
                        configs.add(GameConfig.from(modeId, file));
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
        return new GameConfigs(Map.copyOf(byMode));
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
     * 全部已解析图配置(模式按 id 排序、模式内按 mapId 排序)。
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
}
