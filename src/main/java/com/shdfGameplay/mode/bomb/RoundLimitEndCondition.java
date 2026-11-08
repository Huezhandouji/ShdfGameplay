package com.shdfGameplay.mode.bomb;

import com.hydrokarbs.component.endgate.EndCondition;
import com.hydrokarbs.component.endgate.GateVerdict;
import com.hydrokarbs.engine.instance.MatchModeInstanceView;

import java.util.Optional;

/**
 * 炸弹攻防唯一挂 endgate 的整场判定:某队回合比分到顶 → 该队 WIN(reason=ROUND_LIMIT);回合级判定住框架 RoundComponent 内(4.1 R2 拆分口径)。
 * 来源:05-四模式矩阵 §5.2-A 判定 100 行;6.1-炸弹攻防 步骤 5/6(待裁决 Q1 方案 A);框架阶段4 README §5 待裁决-1;README 对齐注①。
 */
public final class RoundLimitEndCondition implements EndCondition {

    private final BombConfig cfg;

    public RoundLimitEndCondition(final BombConfig cfg) {
        this.cfg = cfg;
    }

    @Override
    public int priority() {
        return 100;
    }

    @Override
    public Optional<GateVerdict> evaluate(final MatchModeInstanceView ctx) {
        // TODO(6.1): 挂 EndGateComponent$Spec(List&lt;EndCondition&gt;)(javap 已核);best-of 与 BombConfig 联动(roundLimit=⌈bestOf/2⌉,6.1-Q3);含 MatchEnd 门控前置;回合内 WIN(优先级 90~60)不触发 switchMode、只有整场(100)产 EndResult 的行为规格随 rounds 挂法在框架 RoundComponent 侧核对 —— 出自 6.1-炸弹攻防 步骤 5/6/要点 3;05-四模式矩阵 §5.2 注记。
        return Optional.empty();
    }
}
