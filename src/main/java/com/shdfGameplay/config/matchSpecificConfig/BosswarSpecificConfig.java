package com.shdfGameplay.config.matchSpecificConfig;

import com.shdfGameplay.config.ConfigUtil;
import com.shdfGameplay.config.GameConfig;
import com.shdfGameplay.config.SpecificConfigParser;
import com.shdfGameplay.map.regionNotation.Region;

import java.util.List;
import java.util.Map;

/**
 * bosswar 玩法的图配置专用项:三处区域(红/蓝出生点、游玩限定区)、首领生命数与三项 tick 计时、
 * 重复角色开关与双方可用角色 id。全部必填,无缺省兜底(带病由 Parser 响亮拒收)。
 */
public record BosswarSpecificConfig(
       Region redSpawn, Region blueSpawn, Region playRegion,
       int bossLifeNumber,
       long timeLimit, long respawnDelay, long immuneTime,
       boolean allowsDuplicatedRoles,
       List<String> redAvailableRoleIds, List<String> blueAvailableRoleIds
) {

    /**
     * bosswar 图配置解析器:metadata 必需键(见 docs/玩法设计/bosswar.md)全齐才成,
     * 任一缺席或类型不符响亮抛 {@link IllegalStateException},跳过与否由上层决定。
     */
    public static final class Parser implements SpecificConfigParser<BosswarSpecificConfig> {

        @Override
        public BosswarSpecificConfig parseRawGameConfig(final GameConfig rawConfig) {
            final Map<String, Object> metadata = rawConfig.metadata();
            final Map<String, Object> spawns = ConfigUtil.section(metadata, "spawns");
            final Map<String, Object> playRegion = ConfigUtil.section(metadata, "play_region");
            final Map<String, Object> availableRoles = ConfigUtil.section(metadata, "available_roles");
            return new BosswarSpecificConfig(
                    ConfigUtil.parseRegion(ConfigUtil.section(spawns, "red_spawn_region")),
                    ConfigUtil.parseRegion(ConfigUtil.section(spawns, "blue_spawn_region")),
                    ConfigUtil.parseRegion(ConfigUtil.section(playRegion, "region")),
                    ConfigUtil.getInt(metadata, "boss_life_number"),
                    ConfigUtil.getLong(metadata, "time_limit"),
                    ConfigUtil.getLong(metadata, "respawn_delay"),
                    ConfigUtil.getLong(metadata, "immune_time"),
                    ConfigUtil.getBoolean(metadata, "allows_duplicated_roles"),
                    ConfigUtil.getStringList(availableRoles, "team_red"),
                    ConfigUtil.getStringList(availableRoles, "team_blue"));
        }
    }

}
