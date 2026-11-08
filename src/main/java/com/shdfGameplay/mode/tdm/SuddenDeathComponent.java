package com.shdfGameplay.mode.tdm;

import com.hydrokarbs.engine.component.MatchModeComponent;
import com.hydrokarbs.engine.port.MatchModeInstanceServices;

/**
 * TDM 加时可选件:进入 SUDDEN_DEATH 阶段时关闭重生/缩小边界/双倍分(剩余时间 &lt; 阈值且分差 &lt; 阈值触发)。
 * 来源:05-四模式矩阵 §5.5-#20(SHDF 玩法专用件,框架阶段4 README:20 确认 #20~#22 住宿主 mode/**)、§5.2-B 阶段链;5.1-TDM模式工厂 步骤 6;README 对齐注①(宿主包根)。
 */
public final class SuddenDeathComponent extends MatchModeComponent {

    // TODO(5.1): 可选挂载(sudden-death.enabled=false 时工厂不挂它);触发阈值参数经 GameConfig 注入;订阅 PhaseChanged 识别进入 SUDDEN_DEATH——不轮询、不按阶段 id 分支,改重生经 getComponent(RespawnComponent.class) 读口;住 mode/ 包不进框架 component/** —— 出自 5.1-TDM模式工厂 步骤 6;05-四模式矩阵 §5.5-#20。
    SuddenDeathComponent(final String id, final MatchModeInstanceServices svc) {
        super(id, svc);
    }

    /**
     * 声明件:加时三参数(关重生/缩边界/双倍分的阈值与倍率)为构造数据,5.1 落位。
     */
    public static final class Spec extends MatchModeComponent.Specification<SuddenDeathComponent> {

        public Spec() {
            super("SuddenDeath");
        }

        @Override
        public SuddenDeathComponent create(final String id, final MatchModeInstanceServices svc) {
            return new SuddenDeathComponent(id, svc);
        }
    }
}
