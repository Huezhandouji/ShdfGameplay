package com.shdfGameplay.mode.boss;

import com.hydrokarbs.component.endgate.EndCondition;
import com.hydrokarbs.component.endgate.GateVerdict;
import com.hydrokarbs.engine.AttributeBag;
import com.hydrokarbs.engine.instance.MatchModeInstanceView;
import com.hydrokarbs.engine.mode.MatchMode;
import com.hydrokarbs.engine.mode.MatchModeMetadata;
import com.hydrokarbs.engine.mode.MatchModeProvider;
import com.hydrokarbs.engine.session.MatchModeConfig;
import com.shdfGameplay.component.display.DisplayModel;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 首领战模式工厂:RAIDERS vs BOSS、限 0~1 命重生、阶段链 Warmup→P1(100~66%)→P2(66~33%)→P3(33~0%)→Resolution(30s)→MatchEnd、机关交互点,Builder 声明零生命周期代码。
 * 来源:05-四模式矩阵 §5.2-C/§5.5/§5.6;施工计划/阶段6-回合制与首领战/6.3-首领战模式 步骤 1/6;README 对齐注①⑨。
 */
public final class BossRaidModes implements MatchModeProvider {

    @Override
    public String id() {
        return "boss_raid";
    }

    @Override
    public MatchMode build(final MatchModeConfig cfg) {
        // TODO(6.3): role(BOSS_POLICY 锁定池禁换角)+限命重生+阶段表(Warmup AWAIT ≥min → P1/P2/P3 AWAIT 血线门 → Resolution TIMED 600t → MatchEnd TIMED)挂法;三判定器内嵌容器 BossEndConditions 照 6.3 步骤 6 表(纯函数、离线可测,同 tick 命中按 priority 降序取首);BossDeclarationTest 冻结组件表序/阶段表序/EndCondition 优先级/依赖校验 —— 出自 6.3-首领战模式 步骤 1/5/6;05-四模式矩阵 §5.2-C。
        // TODO(6.3): respawn/teams 等框架组件 FQN 待框架线阶段4 核实后填(NO_MATCH 现状禁 import) —— 出自 框架线施工计划 阶段4 README;本方案 API 白名单。
        return new MatchMode.Builder(id())
                .displayName(Component.text("首领战"))
                .icon(Material.WITHER_SKELETON_SKULL)
                .mapTags(Set.of("boss"))
                .metadata(MatchModeMetadata.addTo(new AttributeBag.Builder(), true, 3, 24).build())
                .build();
    }

    /**
     * 首领战三判定器容器(boss 无回合,三条整场级全挂 EndGateComponent.Spec;同 tick 命中按 priority 降序取首)。
     * 来源:05-四模式矩阵 §5.2-C EndConditions 表;6.3-首领战模式 步骤 6(:106-114 表)。
     */
    public static final class BossEndConditions {

        private BossEndConditions() {
        }

        /**
         * 优先级 100:Boss 实体死亡 → 玩家队 WIN(reason=BOSS_DEFEATED,含 Resolution 宽限期内)。
         */
        public static final class BossDefeatedEndCondition implements EndCondition {

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
         * 优先级 90:Resolution 已过期且存活 = 0 → Boss WIN(reason=RAID_WIPED)。
         */
        public static final class RaidWipedEndCondition implements EndCondition {

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
         * 优先级 80:重生配额耗尽且全员死(任意阶段)→ Boss WIN(reason=OUT_OF_LIVES)。
         */
        public static final class OutOfLivesEndCondition implements EndCondition {

            @Override
            public int priority() {
                return 80;
            }

            @Override
            public Optional<GateVerdict> evaluate(final MatchModeInstanceView ctx) {
                return Optional.empty();
            }
        }
    }

    /**
     * 首领战表现构建函数容器:数据驱动渲染,零特权构建函数。
     * 来源:05-四模式矩阵 §5.6(自框架 06 §6.2 迁入原文);6.3-首领战模式 步骤 1 display 行。
     */
    public static final class BossDisplays {

        private BossDisplays() {
        }

        /**
         * 首领战构建函数:标题(当前阶段名)/血线条(BossBar,DANGER)/存活行(Sidebar)/Boss 血线进度。
         *
         * @param ctx 实例视图(存活数与血量经组件读口取)
         * @return 中立表现模型(骨架桩为空模型)
         */
        public static DisplayModel bossDisplay(final MatchModeInstanceView ctx) {
            return new DisplayModel(
                    Component.text("首领战"),
                    List.of(),
                    List.of(),
                    Optional.empty(),
                    Optional.empty());
        }
    }
}
