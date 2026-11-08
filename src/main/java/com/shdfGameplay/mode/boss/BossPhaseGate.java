package com.shdfGameplay.mode.boss;

import java.util.List;

/**
 * Boss 血线阶段门:按 HP 阈值升序推进 P1→P2→P3(每次越阈只触发一次,血量回升不回跳);内嵌血线进度 Objective 数据件。
 * 来源:05-四模式矩阵 §5.2-C Objectives 行;6.3-首领战模式 步骤 5;overview BossPhaseGate/BossHealthGate 条;README 对齐注①。
 */
public final class BossPhaseGate {

    /** 血线阈值(HP 百分比,升序;默认 0.66/0.33,经 GameConfig 注入)。 */
    private final List<Double> hpThresholdsAscending;

    public BossPhaseGate(final List<Double> hpThresholdsAscending) {
        this.hpThresholdsAscending = List.copyOf(hpThresholdsAscending);
    }

    // TODO(6.3): 阈值参数经 GameConfig 注入(默认 66%/33%);Resolution 阶段衔接 WipeCounter 宽限;progress = 阶段内剩余血量反向比例,注册进 ObjectiveComponent(待框架线4.2) —— 出自 6.3-首领战模式 步骤 5;05-四模式矩阵 §5.2-C。
    /**
     * 血线门 Objective 数据:阶段序号 + HP 百分比,供 DisplayModel.Progress 渲染。
     * 来源:05-四模式矩阵 §5.2-C Objectives 行(BossHealthGate(phaseIndex, hpPercent))。
     */
    public record BossHealthGate(int phaseIndex, double hpPercent) {
    }
}
