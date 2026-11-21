package com.shdfGameplay.config;

import com.shdfGameplay.lang.Lang;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Logger;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * 宿主配置读取与缓存管理器:init() 先装填 config.yml 主配置(lobby 块 → {@link LobbyConfig}),
 * 再按主配置 map_mode_config 键所指标记文件的 map_modes 列表(条目 模式/图)点名读取图配置,按 modeId 归组——
 * 列表外的 mode_config/ 文件不入集(标记即白名单)。
 * 容错口径:主配置是插件级基建,带病响亮;标记文件缺席/带病=空集不炸,条目级失败(格式坏/文件缺席/
 * 解析失败/重复)跳过并记 warning 日志;重复 init() 整体换缓存(重载口径)。
 * 一模式可挂多图:byMode 取该模式全部图、all 展平给投票候选集、find 定位单图。
 */
public final class GameConfigManager {

    private static final Logger LOGGER = Logger.getLogger(GameConfigManager.class.getName());

    /** map_mode_config 键缺省值(相对宿主数据目录,与资源模板一致)。 */
    private static final String DEFAULT_MARKER_PATH = "mode_config/map_modes.yaml";

    private final Path dataDir;
    private Map<String, List<GameConfig>> configsByModeId = Map.of();
    private LobbyConfig lobbyConfig;

    /**
     * @param dataDir 宿主数据目录(plugins/ShdfGameplay/,config.yml、标记文件与 mode_config/ 相对其解析)
     */
    public GameConfigManager(final Path dataDir) {
        this.dataDir = dataDir;
    }

    /**
     * 装填主配置 lobby 块并按标记文件点名读取图配置,整体换缓存。
     *
     * <p>主配置(config.yml lobby 块)带病响亮;地图读取标记驱动——只读 map_modes 列表点名的
     * mode_config/&lt;模式&gt;/&lt;图&gt;.yaml(.yml),标记文件缺席/带病=空集不炸(warning 日志)。</p>
     *
     * @throws IllegalStateException config.yml 缺失/为空/语法错误或 lobby 块带病
     */
    public void init() {
        final Path mainFile = this.dataDir.resolve("config.yml");
        final YamlConfiguration mainYaml = loadMainConfig(mainFile);
        this.lobbyConfig = LobbyConfig.parse(mainYaml, mainFile);
        this.configsByModeId = loadMarkedConfigs(mainYaml);
    }

    /**
     * 主配置 lobby 块装填结果(大厅边界/出生点/开局倒计时/准备开关/地图投票设置)。
     *
     * @return init() 装填的大厅配置;init() 前为 null
     */
    public LobbyConfig lobby() {
        return this.lobbyConfig;
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
     * 已配置模式 id 集(标记文件点名且至少一张图加载成功的模式 id,排序稳定)。
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

    /**
     * 主配置装载:缺席/空文件/语法错误响亮(口径同 {@link LobbyConfig#from},插件级基建不降级运行)。
     */
    private static YamlConfiguration loadMainConfig(final Path file) {
        if (!Files.isRegularFile(file)) {
            throw new IllegalStateException(Lang.plain("config.main_missing",
                    Placeholder.unparsed("file", String.valueOf(file))));
        }
        final YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file.toFile());
        if (yaml.getKeys(false).isEmpty()) {
            throw new IllegalStateException(Lang.plain("config.main_parse_failed",
                    Placeholder.unparsed("file", String.valueOf(file))));
        }
        return yaml;
    }

    /**
     * 按主配置 map_mode_config 键所指标记文件点名读取图配置。
     *
     * <p>标记键缺省/留白取 {@link #DEFAULT_MARKER_PATH};标记文件相对宿主数据目录解析,
     * 其 map_modes 条目(模式/图)相对 mode_config/ 根解析。标记文件缺席或带病(非列表/元素非字符串)
     * =空集不炸(warning 日志);条目级失败(格式坏/文件缺席/解析失败/重复)逐条跳过并记 warning。</p>
     */
    private Map<String, List<GameConfig>> loadMarkedConfigs(final YamlConfiguration mainYaml) {
        final String markerRaw = mainYaml.getString("map_mode_config");
        final Path markerFile = this.dataDir
                .resolve(markerRaw == null || markerRaw.isBlank() ? DEFAULT_MARKER_PATH : markerRaw);
        if (!Files.isRegularFile(markerFile)) {
            LOGGER.warning(Lang.plain("log.config.marker_missing",
                    Placeholder.unparsed("file", String.valueOf(markerFile))));
            return Map.of();
        }
        final List<String> entries;
        try {
            final YamlConfiguration markerYaml = YamlConfiguration.loadConfiguration(markerFile.toFile());
            entries = ConfigUtil.getStringList(ConfigUtil.deepValues(markerYaml), "map_modes");
        } catch (final IllegalStateException e) {
            LOGGER.warning(Lang.plain("log.config.marker_invalid",
                    Placeholder.unparsed("message", e.getMessage()),
                    Placeholder.unparsed("file", String.valueOf(markerFile))));
            return Map.of();
        }
        final Path configRoot = this.dataDir.resolve("mode_config");
        final Map<String, List<GameConfig>> byMode = new TreeMap<>();
        final Set<String> seen = new HashSet<>();
        for (final String entry : entries) {
            final GameConfig config = readMarkedEntry(configRoot, entry);
            if (config == null) {
                continue;
            }
            if (!seen.add(config.modeId() + "/" + config.mapId())) {
                LOGGER.warning(Lang.plain("log.config.marker_duplicate",
                        Placeholder.unparsed("entry", entry)));
                continue;
            }
            byMode.computeIfAbsent(config.modeId(), mode -> new ArrayList<>()).add(config);
        }
        for (final List<GameConfig> configs : byMode.values()) {
            configs.sort(Comparator.comparing(GameConfig::mapId));
        }
        return Collections.unmodifiableMap(new TreeMap<>(byMode));
    }

    /**
     * 单标记条目 → 图配置:条目格式 模式/图(图段可带 .yaml/.yml 扩展名,缺省补 .yaml、次选 .yml)。
     * 条目格式坏/文件缺席/解析失败 → warning 跳过返 null(带病条目不入集)。
     */
    private GameConfig readMarkedEntry(final Path configRoot, final String entry) {
        final String[] parts = entry.strip().split("/");
        if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) {
            LOGGER.warning(Lang.plain("log.config.marker_bad_format",
                    Placeholder.unparsed("entry", entry)));
            return null;
        }
        final Path file = resolveConfigFile(configRoot, parts[0], parts[1]);
        if (file == null) {
            LOGGER.warning(Lang.plain("log.config.marker_no_file",
                    Placeholder.unparsed("entry", entry)));
            return null;
        }
        try {
            return GameConfig.from(parts[0], file);
        } catch (final IllegalStateException e) {
            LOGGER.warning(Lang.plain("log.config.map_config_skipped",
                    Placeholder.unparsed("message", e.getMessage())));
            return null;
        }
    }

    /** 条目 → mode_config/&lt;模式&gt;/&lt;图&gt; 配置文件:图段自带扩展名原样,否则先 .yaml 后 .yml;均缺席返 null。 */
    private static Path resolveConfigFile(final Path configRoot, final String modeId, final String map) {
        final Path modeDir = configRoot.resolve(modeId);
        if (map.endsWith(".yaml") || map.endsWith(".yml")) {
            final Path direct = modeDir.resolve(map);
            return Files.isRegularFile(direct) ? direct : null;
        }
        final Path asYaml = modeDir.resolve(map + ".yaml");
        if (Files.isRegularFile(asYaml)) {
            return asYaml;
        }
        final Path asYml = modeDir.resolve(map + ".yml");
        return Files.isRegularFile(asYml) ? asYml : null;
    }
}
