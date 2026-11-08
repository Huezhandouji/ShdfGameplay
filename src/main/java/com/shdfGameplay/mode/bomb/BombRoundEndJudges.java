package com.shdfGameplay.mode.bomb;

/**
 * 回合级四判定器容器:回合胜负只回跳 Prep↔Live(RoundComponent 阶段回跳),事件驱动、不挂 endgate、不实现 EndCondition(6.1-R1 方案 A 裁决)。
 * 来源:05-四模式矩阵 §5.2-A 判定 90~60 行与注记;6.1-炸弹攻防 步骤 6;框架阶段4 README §5 待裁决-1;README 对齐注①。
 */
public final class BombRoundEndJudges {

    // TODO(6.1): 四条判定走实例事件驱动(BombObjective 状态机/计时器/团灭检查),不实现 EndCondition 接口;与框架 RoundComponent 的回合结果入口对接(6.1 文档:66 无入口则 escalate,不得自改 component/**) —— 出自 6.1-炸弹攻防 步骤 6;05-四模式矩阵 §5.2。
    private BombRoundEndJudges() {
    }

    /**
     * 引爆判定(优先级序 90):任一炸弹 EXPLODED → 本回合 ATTACKER WIN(reason=BOMB_EXPLODED)。
     */
    public static final class BombExploded {

        private BombExploded() {
        }
    }

    /**
     * 全拆判定(80):存在过 PLANTED 且全部炸弹 DEFUSED、无活动炸弹 → 本回合 DEFENDER WIN(reason=ALL_DEFUSED)。
     */
    public static final class AllDefused {

        private AllDefused() {
        }
    }

    /**
     * 超时判定(70):Live 计时归零且无 PLANTED → 本回合 DEFENDER WIN(reason=TIME_EXPIRED)。
     */
    public static final class TimeExpired {

        private TimeExpired() {
        }
    }

    /**
     * 团灭判定(60):某队存活 = 0 且无重生配额 → 本回合对方 WIN(reason=TEAM_WIPED)。
     */
    public static final class TeamWiped {

        private TeamWiped() {
        }
    }
}
