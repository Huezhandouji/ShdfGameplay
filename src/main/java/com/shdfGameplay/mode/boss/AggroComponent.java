package com.shdfGameplay.mode.boss;

import com.hydrokarbs.engine.component.MatchModeComponent;
import com.hydrokarbs.engine.port.MatchModeInstanceServices;

/**
 * Boss 仇恨表/目标选择/仇恨转移可选件(SHDF 玩法专用件 #22):默认读 Boss 实体原版目标(Mob#getTarget)维护仇恨视图,可独立后补。
 * 来源:05-四模式矩阵 §5.5-#22(框架阶段4 README:20 确认 #20~#22 住宿主 mode/**);6.3-首领战模式 步骤 9;README 对齐注①。
 */
public final class AggroComponent extends MatchModeComponent {

    // TODO(6.3): 可选挂载(➕,不阻塞出口闸门);仇恨表(伤害者→仇恨值)/目标选择/转移;若 Q4 裁决出通用伤害事件桥再升级事件驱动精确归因;住 mode/boss 包,零引擎/既有组件改动 —— 出自 6.3-首领战模式 步骤 9;05-四模式矩阵 §5.5-#22。
    AggroComponent(final String id, final MatchModeInstanceServices svc) {
        super(id, svc);
    }

    /**
     * 声明件:仇恨参数为构造数据,6.3 落位。
     */
    public static final class Spec extends MatchModeComponent.Specification<AggroComponent> {

        public Spec() {
            super("Aggro");
        }

        @Override
        public AggroComponent create(final String id, final MatchModeInstanceServices svc) {
            return new AggroComponent(id, svc);
        }
    }
}
