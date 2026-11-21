package com.shdfGameplay.mode.bosswar;

import com.hydrokarbs.engine.AttributeBag;
import com.hydrokarbs.engine.mode.MatchMode;
import com.hydrokarbs.engine.mode.MatchModeMetadata;
import com.hydrokarbs.engine.mode.MatchModeProvider;
import com.shdfGameplay.component.teleport.MapTeleportComponent;
import com.shdfGameplay.lang.Lang;
import org.bukkit.Material;

import java.util.Set;

/**
 * 首领战模式工厂(空实现):仅模式声明 + map_teleport 进出场传送组件(开局传副本世界出生点/收摊传回主世界),
 * 零玩法逻辑。关键契约:mapTags 必须空集(空 = 通配;ShdfMapAssembler 注册图词料恒空,声明非空 tags 反而选不中图);
 * MODE_ID 必须与 mode_config/bosswar 目录名一致(GameConfig.modeId 取目录名)。
 * 来源:本次任务(bosswar/conquest 空实现);声明形态对照 mode/tdm/TeamDeathmatchModes 与框架 examples/TheirPlugin。
 */
public final class BosswarModes implements MatchModeProvider {

    // TODO(6.3): 首领战玩法逻辑(阶段链/判定器/RolePolicies.BOSS_POLICY 等)随 6.3 施工,本类目前仅空实现 —— 出自 本次任务边界(原 mode/boss 骨架已随遗留模式清理删除)
    /** 模式 id(注册表唯一键;必须与 mode_config/bosswar 目录名一致——GameConfig.modeId 取目录名)。 */
    public static final String MODE_ID = "bosswar";

    @Override
    public String id() {
        return MODE_ID;
    }

    @Override
    public MatchMode build() {
        return new MatchMode.Builder(id())
                .displayName(Lang.render("mode.bosswar.name"))
                .icon(Material.WITHER_SKELETON_SKULL)
                .mapTags(Set.of())
                .metadata(MatchModeMetadata.addTo(new AttributeBag.Builder(), false, 1, 24).build())
                .addComponent("map_teleport", new MapTeleportComponent.Spec())
                .build();
    }
}
