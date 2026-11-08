package com.shdfGameplay.map;

import com.hydrokarbs.api.MiniGameApi;
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
import org.bukkit.plugin.Plugin;

/**
 * 地图装配器:扫描 mode_config/&lt;模式&gt;/&lt;图&gt;.yaml → 按 map_path 归组去重(同世界多模式合并为一份
 * MapDefinition,各模式词料同袋并存)→ 构造 MapDefinition 三字段(worldDirectoryPath/
 * resetStrategy/metadata)→ 先落盘后 registerMap。
 *
 * <p>世界存档存放在数据目录 maps/ 下,yaml 的 map_path 相对数据目录解析(如 maps/example_world)。</p>
 *
 * <p>来源:资源目录 maps/ 与 mode_config/ 现行结构;04-大厅模式 §4.5;README 对齐注⑦⑧⑨适用项</p>
 */
public final class ShdfMapAssembler {

    /** 默认重置策略:maps/ 为存档模板,每局模板恢复(maps/README:"在这个目录下存放地图存档")。 */
    private static final ResetStrategy DEFAULT_RESET_STRATEGY = ResetStrategy.TEMPLATE_RESTORE;

    /**
     * 扫描数据目录并注册全部地图。
     *
     * @param plugin  宿主插件(注册提供方归属)
     * @param api     MiniGameApi 公共面
     * @param dataDir 宿主数据目录(含 mode_config/&lt;模式&gt;/&lt;图&gt;.yaml 与 maps/ 世界存档)
     * @return 各 registerMap 返回的 MapInfo 清单
     * @throws IllegalStateException 世界目录未落盘(先落盘后注册)或 yaml 解析失败(GameConfigs.scan 响亮)
     */
    public static List<MapInfo> registerAll(final Plugin plugin, final MiniGameApi api, final Path dataDir) {
        // 同 map_path 归组:框架 map id 按世界目录派生,同世界只注册一份;多模式词料各占命名空间键并存
        final Map<Path, List<GameConfig>> byWorld = new LinkedHashMap<>();
        for (final GameConfig config : GameConfigs.scan(dataDir).all()) {
            final Path worldDir = dataDir.resolve(config.mapPath()).normalize();
            byWorld.computeIfAbsent(worldDir, key -> new ArrayList<>()).add(config);
        }
        final List<MapInfo> registered = new ArrayList<>();
        for (final Map.Entry<Path, List<GameConfig>> entry : byWorld.entrySet()) {
            final Path worldDir = entry.getKey();
            if (!Files.isDirectory(worldDir)) {
                throw new IllegalStateException("地图世界目录未落盘(先落盘后注册): " + worldDir);
            }
            final AttributeBag.Builder metadata = new AttributeBag.Builder();
            for (final GameConfig config : entry.getValue()) {
                metadata.put(ShdfMapKeys.mapDataKey(config.modeId()), config.metadata());
            }
            registered.add(api.registerMap(plugin,
                    new MapDefinition(worldDir, DEFAULT_RESET_STRATEGY, metadata.build())));
        }
        return List.copyOf(registered);
        // TODO(2.5): 首启从 jar 释放默认资源(mode_config/** 与 maps/** 世界存档整目录拷贝至数据目录)后再扫描;MapAssemblerTest 冻结三响亮+三字段+共用世界分支 —— 出自 2.5-大厅模式与最小闭环 §步骤7
        // TODO(2.5): tags 词料(MapDefinition.TAGS)与 per-yaml reset-strategy 可选键按需引入;热重载口径:unregisterMap(被会话使用会被拒摘)+重新 registerMap —— 出自 2.5 §步骤7;README 对齐注⑧
    }

    private ShdfMapAssembler() {
    }
}
