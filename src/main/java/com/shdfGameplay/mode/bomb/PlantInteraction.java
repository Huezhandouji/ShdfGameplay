package com.shdfGameplay.mode.bomb;

import com.hydrokarbs.engine.component.MatchModeComponent;
import com.hydrokarbs.engine.port.MatchModeInstanceServices;
import com.hydrokarbs.engine.session.MatchModeConfig;
import com.hydrokarbs.map.MapDefinition;

import java.util.List;

/**
 * 安放/拆除读条交互件:安放 3s(60t)/拆除 7s(140t)/拆弹钳 kit 减半 3.5s(70t),MOVE/DAMAGE/RELEASE/ITEM_SWITCH 四类打断——经交互分发组件路由,本类只声明数据。
 * 来源:05-四模式矩阵 §5.7 InteractionComponent 原文;6.2-炸弹攻防 步骤 1~3;README 对齐注①(宿主包根)。
 */
public final class PlantInteraction extends MatchModeComponent {

    // TODO(6.2): 分发组件 InteractionComponent 本次 unzip 核实不在框架 jar(NO_INTERACTION),框架线阶段4 4.2 交付(框架阶段4 README:43 component/interact/)——回填 import 后按 InteractionPoint 数据声明 plant_x/defuse_x(Trigger.Channel(60/140/70,EnumSet.of(MOVE,DAMAGE,RELEASE,ITEM_SWITCH))),若届时仍未交付按 05 §5.7 原文宿主侧落分发件(回本方案补条目) —— 出自 6.2-炸弹攻防 步骤 1/2;05-四模式矩阵 §5.7。
    PlantInteraction(final String id, final MatchModeInstanceServices svc) {
        super(id, svc);
    }

    /**
     * 声明件:读条交互数据(点位/时长/打断集/允许谓词)6.2 落位。
     */
    public static final class Spec extends MatchModeComponent.Specification<PlantInteraction> {

        private final BombConfig cfg;

        public Spec(final BombConfig cfg) {
            super("PlantInteraction");
            this.cfg = cfg;
        }

        @Override
        public PlantInteraction create(final String id, final MatchModeInstanceServices svc) {
            return new PlantInteraction(id, svc);
        }

        @Override
        public List<String> checkMap(final MapDefinition map, final MatchModeConfig cfg) {
            // TODO(6.2): checkMap 完整化——交互点齐全且 spawns.attacker/spawns.defender 非空;缺失进 REJECTED_BY_TARGET_MODE 的 detail(§3.2-A3) —— 出自 6.2-炸弹攻防 步骤 3。
            return List.of();
        }
    }
}
