package com.shdfGameplay.mode.bomb;

import com.hydrokarbs.engine.AttributeBag;
import com.hydrokarbs.engine.instance.MatchModeInstanceView;
import com.hydrokarbs.engine.mode.MatchMode;
import com.hydrokarbs.engine.mode.MatchModeMetadata;
import com.hydrokarbs.engine.mode.MatchModeProvider;
import com.shdfGameplay.component.display.DisplayModel;
import com.shdfGameplay.lang.Lang;
import org.bukkit.Material;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 炸弹攻防模式工厂:Builder 声明全部组件,零生命周期代码;阶段链 Warmup→Prep(5s 冻结)→Live(115s)→RoundEnd(5s)→回 Prep 循环→MatchEnd。
 * 来源:05-四模式矩阵 §5.2-A/§5.5/§5.8 原文样例;施工计划/阶段6-回合制与首领战/6.1-炸弹攻防 步骤 8;README 对齐注①(包行按对齐注重读)⑨(meta 词料键)。
 */
public final class BombDefusalModes implements MatchModeProvider {

    @Override
    public String id() {
        return "bomb_defusal";
    }

    @Override
    public MatchMode build() {
        // TODO(6.1): teams(ATTACKER/DEFENDER+SPECTATOR)/join(REJECT_WHEN_RUNNING)/score(回合分)/respawn(NONE)/bomb/plant 独有件+endgate(仅 RoundLimit)+rules+role(BOMB_POLICY)+guard/visibility/disconnect/stats 挂法;rounds=框架 RoundComponent(框架线4.1 交付后回填,回合级判定住其内);组件表序 BombDefusalDeclarationTest 冻结;阶段链数据 Warmup(AWAIT)→Prep(TIMED 100t)→Live(AWAIT)→RoundEnd(TIMED 100t)→MatchEnd —— 出自 6.1-炸弹攻防 步骤 4/8;05-四模式矩阵 §5.8。
        // TODO(6.1): teams/join/score/respawn 等框架组件 FQN 待框架线阶段4 核实后填(NO_MATCH 现状禁 import);PhaseComponent/EndGateComponent/RuleBookComponent 已在 jar(javap 核)可直接挂 —— 出自 框架线施工计划 阶段4 README;本方案 API 白名单。
        // TODO(6.2): economy=框架 EconomyComponent(框架线4.1 交付后回填,可选挂载,缺省 false 关=零痕迹);半场换边/重传送/RoleComponent.applyPending 同一 PhaseChanged 事件栈内完成 —— 出自 6.2-炸弹攻防 步骤 4/7。
        return new MatchMode.Builder(id())
                .displayName(Lang.render("mode.bomb.name"))
                .icon(Material.TNT)
                .mapTags(Set.of("bomb"))
                .metadata(MatchModeMetadata.addTo(new AttributeBag.Builder(), true, 4, 24).build())
                .build();
    }

    /**
     * 炸弹模式表现构建函数容器:数据驱动渲染,零特权构建函数。
     * 来源:05-四模式矩阵 §5.6(自框架 06 §6.2 迁入原文);6.1-炸弹攻防 文件清单 BombDisplays 行。
     */
    public static final class BombDisplays {

        private BombDisplays() {
        }

        /**
         * 炸弹模式构建函数:标题/回合行(BossBar)/比分行(Sidebar)/倒计时(PhaseComponent)/全局进度(ObjectiveComponent)。
         *
         * @param ctx 实例视图(倒计时与进度经组件读口取,非引擎口子)
         * @return 中立表现模型(骨架桩为空模型)
         */
        public static DisplayModel bombDisplay(final MatchModeInstanceView ctx) {
            return new DisplayModel(
                    Lang.render("mode.bomb.name"),
                    List.of(),
                    List.of(),
                    Optional.empty(),
                    Optional.empty());
        }
    }
}
