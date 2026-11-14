package com.shdfGameplay.config;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * 单图配置载体:mode_config/&lt;模式&gt;/&lt;图&gt;.yaml 的自解析结果;一文件一图(文件名即 mapId、目录名即 modeId)。
 * metadata 自由嵌套映射由 MatchBootstrap 开局前经 {@link SpecificConfigParser} 解析为玩法专用 record,
 * 随 MatchConfigComponent 会话组件入局(不写入 MapDefinition.metadata,词料通道已弃用);mapPath 相对宿主数据目录。
 * 展示三键(display_name/description/icon)解析时归一默认值,永不为 null(display_name→mapId、description→空串、icon→MAP)。
 */
public record GameConfig(String modeId, String mapId, Path mapPath, String displayName, String description,
                         Material icon, Map<String, Object> metadata) {

    /** icon 键缺省/留白/未知材质名时的兜底图标(地图条目 GUI 图标)。 */
    private static final Material DEFAULT_ICON = Material.MAP;

    /**
     * 从 mode_config/&lt;模式&gt;/&lt;图&gt;.yaml 解析出单图配置。
     *
     * <p>展示三键容错读取(缺键/留白取默认值,老配置不带病):display_name→mapId、description→空串、icon→MAP。</p>
     *
     * @param modeId 模式 id(目录名,如 bosswar)
     * @param file   mode_config/&lt;模式&gt;/&lt;图&gt;.yaml 文件路径
     * @return 解析结果(mapId=文件名去扩展名)
     * @throws IllegalStateException 文件为空/语法错误、缺 map_path 键或 map_path 非法路径(响亮;跳过与否由 GameConfigManager 决定)
     */
    public static GameConfig from(final String modeId, final Path file) {
        final YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file.toFile());
        if (yaml.getKeys(false).isEmpty()) {
            throw new IllegalStateException("mode_config 解析失败(空文件或语法错误): " + file);
        }
        final String mapPathRaw = yaml.getString("map_path");
        if (mapPathRaw == null || mapPathRaw.isBlank()) {
            throw new IllegalStateException("mode_config 缺 map_path 键: " + file);
        }
        final Path mapPath;
        try {
            mapPath = Path.of(mapPathRaw);
        } catch (final InvalidPathException e) {
            throw new IllegalStateException("mode_config map_path 非法路径: " + mapPathRaw + " @ " + file, e);
        }
        final String mapId = stripExtension(file.getFileName().toString());
        final ConfigurationSection metadata = yaml.getConfigurationSection("metadata");
        final Map<String, Object> rawMetadata = metadata == null ? Map.of() : deepValues(metadata);
        return new GameConfig(modeId, mapId, mapPath,
                orDefault(yaml.getString("display_name"), mapId),
                orDefault(yaml.getString("description"), ""),
                iconOf(yaml.getString("icon")),
                Collections.unmodifiableMap(new LinkedHashMap<>(rawMetadata)));
    }

    private static String orDefault(final String value, final String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    /** icon 键 → Material(缺省/留白/未知材质名兜底 MAP,装饰键写错不响亮)。 */
    private static Material iconOf(final String name) {
        if (name == null || name.isBlank()) {
            return DEFAULT_ICON;
        }
        final Material matched = Material.matchMaterial(name);
        return matched == null ? DEFAULT_ICON : matched;
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
