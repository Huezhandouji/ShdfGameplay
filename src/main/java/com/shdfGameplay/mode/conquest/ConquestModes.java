package com.shdfGameplay.mode.conquest;

import com.hydrokarbs.engine.AttributeBag;
import com.hydrokarbs.engine.mode.MatchMode;
import com.hydrokarbs.engine.mode.MatchModeMetadata;
import com.hydrokarbs.engine.mode.MatchModeProvider;
import com.shdfGameplay.component.teleport.MapTeleportComponent;
import com.shdfGameplay.lang.Lang;
import org.bukkit.Material;

import java.util.Set;

/**
 * 占领战模式工厂(空实现):仅模式声明 + map_teleport 进出场传送组件(开局传副本世界出生点/收摊传回主世界),
 * 零玩法逻辑。关键契约:mapTags 必须空集(空 = 通配;ShdfMapAssembler 注册图词料恒空,声明非空 tags 反而选不中图);
 * MODE_ID 必须与 mode_config/conquest 目录名一致(GameConfig.modeId 取目录名)。
 * 来源:本次任务(bosswar/conquest 空实现);声明形态对照 mode/bosswar/BosswarModes 与 mode/tdm/TeamDeathmatchModes。
 */
public final class ConquestModes implements MatchModeProvider {

    // TODO(conquest): 占领玩法逻辑(热据点 hotpoints/出生点/限定区)施工阶段编号待路线图补定,本类目前仅空实现 —— 出自 本次任务边界
    /** 模式 id(注册表唯一键;必须与 mode_config/conquest 目录名一致——GameConfig.modeId 取目录名)。 */
    public static final String MODE_ID = "conquest";

    @Override
    public String id() {
        return MODE_ID;
    }

    @Override
    public MatchMode build() {
        return new MatchMode.Builder(id())
                .displayName(Lang.render("mode.conquest.name"))
                .icon(Material.BEACON)
                .mapTags(Set.of())
                .metadata(MatchModeMetadata.addTo(new AttributeBag.Builder(), false, 1, 24).build())
                .addComponent("map_teleport", new MapTeleportComponent.Spec())
                .build();
    }
}
