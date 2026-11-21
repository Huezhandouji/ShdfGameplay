package com.shdfGameplay.mode.tdm;

import com.hydrokarbs.component.endgate.EndCondition;
import com.hydrokarbs.component.endgate.GateVerdict;
import com.hydrokarbs.engine.AttributeBag;
import com.hydrokarbs.engine.instance.MatchModeInstanceView;
import com.hydrokarbs.engine.mode.MatchMode;
import com.hydrokarbs.engine.mode.MatchModeMetadata;
import com.hydrokarbs.engine.mode.MatchModeProvider;
import com.shdfGameplay.lang.Lang;
import org.bukkit.Material;

import java.util.Optional;
import java.util.Set;

/**
 * TDM(团队死斗)模式工厂:首模式先例——声明序/测试形态供 bomb/boss 工厂照抄;2~4 队击杀计分、定时重生,Builder 声明零生命周期代码。
 * 来源:05-四模式矩阵 §5.2-B/§5.8;施工计划/阶段5-TDM首模式/5.1-TDM模式工厂.md 步骤 1~5;README 对齐注①⑨(包根映射、meta 词料键)与⑩(lobby 列为历史口径)。
 */
public final class TeamDeathmatchModes implements MatchModeProvider {

    // TODO(5.2): 2 人端到端:开局托管→击杀计分→阈值→WIN→回大厅战报(★18 战报交接/HANDOFF_RESULT) —— 出自 5.2-端到端验收-大厅到对局。
    @Override
    public String id() {
        return "team_deathmatch";
    }

    @Override
    public MatchMode build() {
        // TODO(5.1): Builder 声明序冻结(组件表 14 行,TdmDeclarationTest=§13.1-8 模板);三判定器内嵌容器 TdmEndConditions 照 5.1 步骤 5 表(纯函数,状态经 MatchModeInstanceView 从 ScoreComponent/RespawnComponent/PhaseComponent 读口取,§14.26 不复制比分/存活数到判定器字段);display 挂 new DisplayComponent.Spec(构建函数方法引用) —— 出自 5.1-TDM模式工厂 步骤 2/5/7。
        // TODO(5.1): teams(RED/BLUE/GREEN/YELLOW)/score/respawn(5~15s 可配)/join 挂法——TeamComponent/ScoreComponent/JoinControlComponent 本次 unzip 核实不在框架 jar(NO_MATCH),待框架线阶段4 交付后填 import;观战安置=JoinControl Spec 配 SPECTATE_WHEN_RUNNING(组件数据零代码) —— 出自 5.1-TDM模式工厂 步骤 2;框架线施工计划 阶段4。
        return new MatchMode.Builder(id())
                .displayName(Lang.render("mode.tdm.name"))
                .icon(Material.IRON_SWORD)
                .mapTags(Set.of("competitive"))
                .metadata(MatchModeMetadata.addTo(new AttributeBag.Builder(), true, 2, 24).build())
                .build();
    }

    /**
     * TDM 三判定器容器(整场级,全挂 EndGateComponent.Spec;同 tick 命中按 priority 降序取首)。
     * 来源:05-四模式矩阵 §5.2-B EndConditions 表;5.1-TDM模式工厂 步骤 5(:82-88 表)。
     */
    public static final class TdmEndConditions {

        private TdmEndConditions() {
        }

        /**
         * 优先级 100:某队分数 ≥ scoreLimit → 该队 WIN(reason=SCORE_LIMIT)。
         */
        public static final class ScoreLimitEndCondition implements EndCondition {

            @Override
            public int priority() {
                return 100;
            }

            @Override
            public Optional<GateVerdict> evaluate(final MatchModeInstanceView ctx) {
                return Optional.empty();
            }
        }

        /**
         * 优先级 90:LIVE 计时归零 → 最高队 WIN,并列 DRAW(reason=TIME_LIMIT)。
         */
        public static final class TimeLimitEndCondition implements EndCondition {

            @Override
            public int priority() {
                return 90;
            }

            @Override
            public Optional<GateVerdict> evaluate(final MatchModeInstanceView ctx) {
                return Optional.empty();
            }
        }

        /**
         * 优先级 85:只剩一队存活且他队重生配额 0 → 该队 WIN(reason=LAST_TEAM_STANDING)。
         */
        public static final class LastTeamStandingEndCondition implements EndCondition {

            @Override
            public int priority() {
                return 85;
            }

            @Override
            public Optional<GateVerdict> evaluate(final MatchModeInstanceView ctx) {
                return Optional.empty();
            }
        }
    }

    /**
     * TDM 表现构建函数容器位:tdmDisplay(ctx) 于 5.1 落地(各队比分行/PhaseComponent 倒计时行/SuddenDeath 高亮行,形状照 05 §5.6)。
     * 来源:05-四模式矩阵 §5.6;5.1-TDM模式工厂 步骤 7。
     */
    public static final class TdmDisplays {

        private TdmDisplays() {
        }
    }
}
