package com.shdfGameplay.mode.bomb;

import com.hydrokarbs.engine.component.MatchModeComponent;
import com.hydrokarbs.engine.port.MatchModeInstanceServices;
import com.hydrokarbs.engine.session.MatchModeConfig;
import com.hydrokarbs.map.MapDefinition;
import com.shdfGameplay.component.display.DisplayModel;
import net.kyori.adventure.text.Component;

import java.util.List;

/**
 * 炸弹目标状态机(每张图每个炸弹点一个实例):UNPLANTED→PLANTED→EXPLODED|DEFUSED;progress() 供表现层渲染。
 * 来源:05-四模式矩阵 §5.2-A BombObjective 骨架;6.1-炸弹攻防 步骤 3;README 对齐注⑨(map 词料 API 已删;
 * bomb-site 区域配置读会话 MatchConfigComponent 装载的炸弹模式专用 record,地图词料通道已弃用)。
 */
public final class BombObjective {

    // TODO(6.1): 每炸弹点一实例;bomb-site 区域读会话组件 svc().self().session().component(MatchConfigComponent.class)
    //             .config(炸弹专用配置 record.class)(SessionView 只读软接入;配置开局前 MatchBootstrap 解析装入,跨模式切换存活);
    //             引信计时在实例 update() 节拍内查 fuseDeadlineTick,不自起任务 —— 出自 6.1-炸弹攻防 步骤 3/要点 4;05-四模式矩阵 §5.2。
    /** 炸弹状态机:UNPLANTED → PLANTED → EXPLODED | DEFUSED。 */
    public enum BombStatus {
        UNPLANTED, PLANTED, EXPLODED, DEFUSED
    }

    private BombStatus status = BombStatus.UNPLANTED;

    /**
     * 目标进度:四态数值照 §5.2 骨架(骨架桩返回 UNPLANTED 态)。
     *
     * @return 中立进度模型(供 DisplayModel.Progress 渲染)
     */
    public DisplayModel.Progress progress() {
        // TODO(6.1): progress() 四态——UNPLANTED(0,1,待安放)/PLANTED(1,1,已安放)/EXPLODED(1,1,已引爆)/DEFUSED(0,1,已拆除);label 按状态语义取值(§5.2 骨架 label 疑似错位,按 6.1-Q2 建议实现) —— 出自 6.1-炸弹攻防 步骤 3/待裁决 Q2。
        return new DisplayModel.Progress(0, 1, Component.text("待安放"));
    }

    /**
     * 炸弹目标工厂件:awake/start 期对图内每个 bomb-site 区域各建一个 BombObjective 实例并注册进 ObjectiveComponent(待框架线4.2)。
     * 来源:05-四模式矩阵 §5.8 组件表 bomb 行;6.1-炸弹攻防 步骤 3。
     */
    public static final class BombObjectiveFactory extends MatchModeComponent {

        BombObjectiveFactory(final String id, final MatchModeInstanceServices svc) {
            super(id, svc);
        }

        /**
         * 声明件:checkMap 要求地图存在 bomb-site 区域(装配期拒绝,不等到 awake)。
         */
        public static final class Spec extends MatchModeComponent.Specification<BombObjectiveFactory> {

            private final BombConfig cfg;

            public Spec(final BombConfig cfg) {
                super("BombObjective");
                this.cfg = cfg;
            }

            @Override
            public BombObjectiveFactory create(final String id, final MatchModeInstanceServices svc) {
                return new BombObjectiveFactory(id, svc);
            }

            @Override
            public List<String> checkMap(final MapDefinition map, final MatchModeConfig cfg) {
                // TODO(6.1): bomb-site 区域与配套 plant_*/defuse_* 交互点的完整清单校验前移至炸弹模式 SpecificConfigParser
                //             开局响亮(带病配置不进会话);checkMap 仅剩地图侧校验(mapTags 等,MapDefinition.TAGS) —— 出自 6.1-炸弹攻防 步骤 3;6.2-炸弹攻防 步骤 3(§3.2-A3)。
                return List.of();
            }
        }
    }
}
