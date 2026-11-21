package com.shdfGameplay.config;

import com.shdfGameplay.lang.Lang;
import com.shdfGameplay.map.regionNotation.Region;

import java.nio.file.Files;
import java.nio.file.Path;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * 宿主主配置(config.yml lobby 块)载体:大厅边界(世界 + 区域表示法)、大厅出生点、开局倒计时(tick)、
 * 准备开关、地图投票设置与开局最少人数。
 * 容错口径分两档:几何必需键(bounds 块及其 world 与区域表示法、spawn 块及其 world)缺席或非法
 * 响亮抛 {@link IllegalStateException}(同 GameConfig.map_path 口径);计时/开关类键缺省取默认值
 * (countdown→600、enable_ready→false、map_vote_setting→enabled=true/map_amount=3、min-players→2),
 * 老配置升级缺键不 NPE。
 * bounds 与 mode_config 图配置的区域表示法同构(经 {@link ConfigUtil#parseRegion} 按 type 分发),
 * 额外 world 键定位大厅所在世界、不参与区域解析;bounds.world 与 spawn.world 各自独立解析不校验一致。
 */
public record LobbyConfig(World boundsWorld, Region bounds, Location spawn, long countdown, boolean enableReady,
                          MapVoteSetting mapVoteSetting, int minPlayers) {

    /** lobby.map_vote_setting 子块:是否开启地图投票(false=大厅启动时随机抽图)与投票候选抽图数。 */
    public record MapVoteSetting(boolean enabled, int mapAmount) {
    }

    /** countdown 键缺省值(tick,30s)。 */
    private static final long DEFAULT_COUNTDOWN = 600L;

    /** enable_ready 键缺省值(默认不启用准备门槛)。 */
    private static final boolean DEFAULT_ENABLE_READY = false;

    /** map_vote_setting.enabled 键缺省值(默认开启投票,沿大厅投票历史行为)。 */
    private static final boolean DEFAULT_VOTE_ENABLED = true;

    /** map_vote_setting.map_amount 键缺省值。 */
    private static final int DEFAULT_VOTE_AMOUNT = 3;

    /** min-players 键缺省值。 */
    private static final int DEFAULT_MIN_PLAYERS = 2;

    /**
     * 从宿主数据目录 config.yml 解析 lobby 块。
     *
     * <p>config.yml 缺失(应先经 saveDefaultConfig 释放)、空文件/语法错误、缺 lobby 块
     * 及几何必需键带病均响亮(主配置是插件级基建,坏了大厅整体不可用,不降级运行)。</p>
     *
     * @param file 宿主数据目录下的 config.yml 路径
     * @return 解析结果(计时/开关类键缺省取默认值)
     * @throws IllegalStateException 文件缺失/为空/语法错误,或 lobby 块必需键缺席/非法(消息带文件路径)
     */
    public static LobbyConfig from(final Path file) {
        if (!Files.isRegularFile(file)) {
            throw new IllegalStateException(Lang.plain("config.main_missing",
                    Placeholder.unparsed("file", String.valueOf(file))));
        }
        final YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file.toFile());
        if (yaml.getKeys(false).isEmpty()) {
            throw new IllegalStateException(Lang.plain("config.main_parse_failed",
                    Placeholder.unparsed("file", String.valueOf(file))));
        }
        return parse(yaml, file);
    }

    /**
     * 已装载主配置 → lobby 块解析:与 {@link #from} 的差集仅为文件缺席/空文件检查归调用方——
     * GameConfigManager 复用同一装载结果另读 map_mode_config 键,主配置只落盘读一次。
     *
     * @param yaml 已装载的 config.yml
     * @param file 主配置路径(带病消息上下文)
     * @return 解析结果(计时/开关类键缺省取默认值)
     * @throws IllegalStateException 缺 lobby 块或必需键缺席/非法(消息带文件路径)
     */
    static LobbyConfig parse(final YamlConfiguration yaml, final Path file) {
        final ConfigurationSection lobby = yaml.getConfigurationSection("lobby");
        if (lobby == null) {
            throw new IllegalStateException(Lang.plain("config.lobby.block_missing",
                    Placeholder.unparsed("file", String.valueOf(file))));
        }
        try {
            return parseLobby(lobby);
        } catch (final IllegalStateException e) {
            // 子键定位消息补上主配置文件上下文,与 GameConfig.from 的带病口径一致
            throw new IllegalStateException(Lang.plain("config.lobby.block_parse_failed",
                    Placeholder.unparsed("message", e.getMessage()),
                    Placeholder.unparsed("file", String.valueOf(file))), e);
        }
    }

    private static LobbyConfig parseLobby(final ConfigurationSection lobby) {
        final ConfigurationSection bounds = requireSection(lobby, "bounds");
        final long countdown = lobby.getLong("countdown", DEFAULT_COUNTDOWN);
        if (countdown < 0) {
            throw new IllegalStateException(Lang.plain("config.lobby.countdown_negative",
                    Placeholder.unparsed("count", String.valueOf(countdown))));
        }
        final int minPlayers = lobby.getInt("min-players", DEFAULT_MIN_PLAYERS);
        if (minPlayers < 1) {
            throw new IllegalStateException(Lang.plain("config.lobby.min_players_below_one",
                    Placeholder.unparsed("count", String.valueOf(minPlayers))));
        }
        return new LobbyConfig(
                parseWorld(bounds, "bounds"),
                ConfigUtil.parseRegion(ConfigUtil.deepValues(bounds)),
                parseSpawn(requireSection(lobby, "spawn")),
                countdown,
                lobby.getBoolean("enable_ready", DEFAULT_ENABLE_READY),
                parseVoteSetting(lobby.getConfigurationSection("map_vote_setting")),
                minPlayers);
    }

    /** spawn 块 → Location:world 必填响亮,x/y/z/yaw/pitch 缺省 0(Bukkit 原生容错口径)。 */
    private static Location parseSpawn(final ConfigurationSection spawn) {
        return new Location(parseWorld(spawn, "spawn"),
                spawn.getDouble("x"), spawn.getDouble("y"), spawn.getDouble("z"),
                (float) spawn.getDouble("yaw"), (float) spawn.getDouble("pitch"));
    }

    /** map_vote_setting 块 → MapVoteSetting:整块缺省取默认;开启投票时 map_amount 至少 1(响亮)。 */
    private static MapVoteSetting parseVoteSetting(final ConfigurationSection vote) {
        final boolean enabled = vote == null ? DEFAULT_VOTE_ENABLED : vote.getBoolean("enabled", DEFAULT_VOTE_ENABLED);
        final int mapAmount = vote == null ? DEFAULT_VOTE_AMOUNT : vote.getInt("map_amount", DEFAULT_VOTE_AMOUNT);
        if (enabled && mapAmount < 1) {
            throw new IllegalStateException(Lang.plain("config.lobby.map_amount_below_one",
                    Placeholder.unparsed("count", String.valueOf(mapAmount))));
        }
        return new MapVoteSetting(enabled, mapAmount);
    }

    /** world 键 → World:缺失/留白/世界不在场均响亮(世界名笔误当场暴露,不留 null 世界进运行期)。 */
    private static World parseWorld(final ConfigurationSection section, final String what) {
        final String name = section.getString("world");
        if (name == null || name.isBlank()) {
            throw new IllegalStateException(Lang.plain("config.lobby.world_missing",
                    Placeholder.unparsed("what", what)));
        }
        final World world = Bukkit.getWorld(name);
        if (world == null) {
            throw new IllegalStateException(Lang.plain("config.lobby.world_absent",
                    Placeholder.unparsed("what", what),
                    Placeholder.unparsed("name", name)));
        }
        return world;
    }

    private static ConfigurationSection requireSection(final ConfigurationSection parent, final String key) {
        final ConfigurationSection section = parent.getConfigurationSection(key);
        if (section == null) {
            throw new IllegalStateException(Lang.plain("config.lobby.section_missing",
                    Placeholder.unparsed("key", key)));
        }
        return section;
    }
}
