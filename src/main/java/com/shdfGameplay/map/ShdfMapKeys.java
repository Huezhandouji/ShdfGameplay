package com.shdfGameplay.map;

import com.hydrokarbs.engine.AttributeBag;
import java.util.Map;

/**
 * 宿主地图词料键注册处:一模式一键(shdf.&lt;模式&gt;.map),值为该模式 yaml 已解析的 metadata 自由映射
 * (bosswar: spawns/boss_life_number/play_region;conquest: hotpoints/spawns/play_region),
 * 存入 MapDefinition.metadata 的玩法命名空间;同世界多模式时各键同袋并存。
 *
 * <p>框架唯一解释键 = MapDefinition.TAGS,其余键全部宿主自定(README 对齐注⑨,v1.10/11)。</p>
 *
 * <p>来源:资源目录 mode_config/ 现行结构;04-大厅模式 §4.5;README 对齐注⑨适用项</p>
 */
public final class ShdfMapKeys {

    /**
     * 某模式的地图词料键(玩法命名空间 shdf.&lt;模式&gt;.map),值为该模式 yaml 的 metadata 映射。
     *
     * @param modeId 模式 id(mode_config/ 目录名)
     * @return 泛型词料键(类型令牌为 Map.class;逐模式键名运行期拼出,无法静态收窄为具体 record)
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static AttributeBag.Key<Map<String, Object>> mapDataKey(final String modeId) {
        return (AttributeBag.Key) AttributeBag.Key.of("shdf." + modeId + ".map", Map.class);
    }

    // TODO(2.5): idle 两模式(idlepad/idlepark)词料定形(出生点分组等)后按需加静态收窄键 shdf.idlepad.map / shdf.idlepark.map —— 出自 2.5-大厅模式与最小闭环 §步骤9
    // TODO(bosswar/conquest 工厂落地时): metadata 自由映射收紧为 typed record(bosswar: spawns 分组/boss_life_number/play_region;conquest: hotpoints 据点表/spawns 分组/play_region),键名不变只换值类型 —— 出自 mode_config/<模式>/example.yaml 结构;05-四模式矩阵 词料口径

    private ShdfMapKeys() {
    }
}
