package com.shdfGameplay.map;

import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * 单图配置载体:mode_config/&lt;模式&gt;/&lt;图&gt;.yaml 的自解析结果(map_path/metadata/可选 session-flow)。
 *
 * <p>一文件一图:文件名(去扩展名)即 mapId,所在目录名即 modeId。metadata 为自由嵌套映射
 * (bosswar: spawns/boss_life_number/play_region;conquest: hotpoints/spawns/play_region),
 * 原样透传给 {@link ShdfMapKeys} 的玩法命名空间键,词料定形随各模式工厂落地。</p>
 *
 * <p>来源:资源目录 mode_config/ 现行结构(README 与 example.yaml);04-大厅模式 §4.5 分工表(引框架 §15.2 铁律1);README 对齐注⑧⑩适用项</p>
 */
public record GameConfig(String modeId, String mapId, String mapPath, Map<String, Object> metadata,
                         String sessionFlow) {

    /**
     * 从 mode_config/&lt;模式&gt;/&lt;图&gt;.yaml 解析出单图配置。
     *
     * @param modeId 模式 id(目录名,如 bosswar)
     * @param file   mode_config/&lt;模式&gt;/&lt;图&gt;.yaml 文件路径
     * @return 解析结果(mapId=文件名去扩展名)
     * @throws IllegalStateException 文件为空/语法错误、或缺 map_path 键(不带病注册)
     */
    public static GameConfig from(final String modeId, final Path file) {
        final YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file.toFile());
        if (yaml.getKeys(false).isEmpty()) {
            throw new IllegalStateException("mode_config 解析失败(空文件或语法错误): " + file);
        }
        final String mapPath = yaml.getString("map_path");
        if (mapPath == null || mapPath.isBlank()) {
            throw new IllegalStateException("mode_config 缺 map_path 键: " + file);
        }
        final ConfigurationSection metadata = yaml.getConfigurationSection("metadata");
        final Map<String, Object> rawMetadata = metadata == null ? Map.of() : deepValues(metadata);
        return new GameConfig(modeId, stripExtension(file.getFileName().toString()), mapPath,
                Collections.unmodifiableMap(new LinkedHashMap<>(rawMetadata)), yaml.getString("session-flow"));
    }

    private static String stripExtension(final String fileName) {
        return fileName.endsWith(".yaml") ? fileName.substring(0, fileName.length() - 5)
                : fileName.substring(0, fileName.length() - 4);
    }

    private static Map<String, Object> deepValues(final ConfigurationSection section) {
        final Map<String, Object> out = new LinkedHashMap<>();
        for (final String key : section.getKeys(false)) {
            final Object value = section.get(key);
            out.put(key, value instanceof final ConfigurationSection child ? deepValues(child) : value);
        }
        return out;
    }
}
