package com.shdfGameplay.map;

import com.hydrokarbs.api.HydrokarbsApi;
import com.hydrokarbs.api.view.MapInfo;
import com.hydrokarbs.engine.AttributeBag;
import com.hydrokarbs.engine.port.ResetStrategy;
import com.hydrokarbs.map.MapDefinition;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.shdfGameplay.config.GameConfig;
import com.shdfGameplay.config.GameConfigManager;
import com.shdfGameplay.lang.Lang;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.plugin.Plugin;

/**
 * 地图装配器:取 GameConfigManager 已缓存图配置 → 按 map_path 归组去重(同世界多模式合并为一份
 * MapDefinition)→ 构造两字段(世界目录+重置策略)→ 先落盘后 registerMap。
 * MapDefinition 不携带玩法配置 metadata——玩法配置改由 MatchBootstrap 开局前解析、随
 * MatchConfigComponent 会话组件入局(原 ShdfMapKeys 词料通道已弃用);词料仅剩框架解释键
 * MapDefinition.TAGS(per-yaml 可选 tags 键,按需引入)。
 * 世界存档存放在数据目录 maps/ 下,yaml 的 map_path 相对数据目录解析。
 */
public final class ShdfMapAssembler {

    /** 默认重置策略:maps/ 为存档模板,每局模板恢复(maps/README:"在这个目录下存放地图存档")。 */
    private static final ResetStrategy DEFAULT_RESET_STRATEGY = ResetStrategy.TEMPLATE_RESTORE;

    /**
     * 注册全部地图(配置由 GameConfigManager.init() 一次性读取缓存,本类不重复扫描)。
     *
     * @param plugin  宿主插件(注册提供方归属)
     * @param hk      HydrokarbsApi 公共面
     * @param dataDir 宿主数据目录(含 maps/ 世界存档,map_path 相对其解析)
     * @param configs 已 init 的配置管理器(缓存数据源)
     * @return 各 registerMap 返回的 MapInfo 清单
     * @throws IllegalStateException 世界目录未落盘(先落盘后注册)
     */
    public static List<MapInfo> registerAll(final Plugin plugin, final HydrokarbsApi hk, final Path dataDir,
                                            final GameConfigManager configs) {
        // 同 map_path 归组:框架 map id 按世界目录派生,同世界只注册一份;玩法配置不进 MapDefinition(随会话组件入局)
        final Map<Path, List<GameConfig>> byWorld = new LinkedHashMap<>();
        for (final GameConfig config : configs.all()) {
            final Path worldDir = dataDir.resolve(config.mapPath()).normalize();
            byWorld.computeIfAbsent(worldDir, key -> new ArrayList<>()).add(config);
        }
        final List<MapInfo> registered = new ArrayList<>();
        for (final Map.Entry<Path, List<GameConfig>> entry : byWorld.entrySet()) {
            final Path worldDir = entry.getKey();
            if (!Files.isDirectory(worldDir)) {
                throw new IllegalStateException(Lang.plain("config.map.world_dir_missing",
                        Placeholder.unparsed("dir", String.valueOf(worldDir))));
            }
            registered.add(hk.registerMap(plugin,
                    new MapDefinition(worldDir, DEFAULT_RESET_STRATEGY, AttributeBag.empty())));
        }
        return List.copyOf(registered);
        // TODO(2.5): 首启从 jar 释放默认资源(mode_config/** 与 maps/** 世界存档整目录拷贝至数据目录)后再扫描;MapAssemblerTest 冻结响亮+两字段+共用世界分支 —— 出自 2.5-大厅模式与最小闭环 §步骤7
        // TODO(2.5): tags 词料(MapDefinition.TAGS,per-yaml 可选 tags 键)与 per-yaml reset-strategy 可选键按需引入;热重载口径:unregisterMap(被会话使用会被拒摘)+重新 registerMap —— 出自 2.5 §步骤7;README 对齐注⑧
    }

    private ShdfMapAssembler() {
    }
}
