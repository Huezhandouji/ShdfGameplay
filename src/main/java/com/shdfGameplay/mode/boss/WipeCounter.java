package com.shdfGameplay.mode.boss;

/**
 * 存活数归零团灭判定素材:RAIDERS 存活数归零 → Resolution 阶段 30s 宽限,防同 tick 竞态(最后一名玩家死亡与 Boss 死亡可能跨 tick 先后到达)。
 * 来源:05-四模式矩阵 §5.2-C Objectives 行;6.3-首领战模式 步骤 5;overview WipeCounter 条;README 对齐注①。
 */
public final class WipeCounter {

    // TODO(6.3): RaidWipedEndCondition(90) 的判定素材——存活数经 MatchModeInstanceView 从 TeamComponent/Participant 读口取(不复制存活数到字段,§14.26);归零且仅一次 jump("Resolution");宽限期参数经 GameConfig 注入(默认 30s) —— 出自 6.3-首领战模式 步骤 5/要点 4;05-四模式矩阵 §5.2-C。
    private WipeCounter() {
    }
}
