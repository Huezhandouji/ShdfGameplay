package com.shdfGameplay.mode.tdm;

import com.shdfGameplay.component.role.RoleKillEvent;

/**
 * TDM 规则件容器:击杀计分规则(消费 RoleKillEvent,自杀不计、≥5 连杀加成);ScoreObjective 为框架侧交付物,宿主不建同名件。
 * 来源:05-四模式矩阵 §5.5 矩阵 TDM 行;5.1-TDM模式工厂 步骤 4;框架线施工计划 阶段4 README(component/score/ 含 ScoreObjective)。
 */
public final class TdmRules {

    // TODO(框架线4.1): ScoreObjective 归属核对——框架阶段4 4.1 列同名件于 component/score/,回填时消费框架件,宿主只保留阈值参数构造数据 —— 出自 框架线施工计划 阶段4 README:42。
    private TdmRules() {
    }

    /**
     * 击杀计分规则:tdm.kill.score——Rule 纯函数纪律(§16.2-★9),离线可测。
     * 来源:05-四模式矩阵 §5.2-B 规则示例;5.1-TDM模式工厂 步骤 4。
     */
    public static final class KillScoreRule {

        private KillScoreRule() {
        }

        /**
         * 计一次击杀的分值:自杀不计;连杀 ≥ 阈值(缺省 5)加成。
         *
         * @param kill RoleComponent 合成并发布到实例总线的击杀事件(官方死亡信号经 Directory.dispatch 路由,禁 player.getKiller(),§14.4)
         * @return 本次击杀得分(骨架桩恒 0)
         */
        public static int score(final RoleKillEvent kill) {
            // TODO(5.1): base=score-per-kill(缺省 1)+连杀 ≥ streak-bonus-threshold(缺省 5)加 1;RoleKillEvent 不带队伍字段,经 getComponent(TeamComponent.class) 自查(TeamComponent 待框架线4 交付) —— 出自 05-四模式矩阵 §5.2-B;5.1-TDM模式工厂 步骤 4(§16.2-★9 纯函数离线可测)。
            return 0;
        }
    }
}
