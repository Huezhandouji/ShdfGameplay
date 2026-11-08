package com.shdfGameplay.flow;

import com.hydrokarbs.engine.component.SessionComponent;
import com.shdfGameplay.map.GameConfig;
import java.util.List;

/**
 * 会话流程策略(mode_config/&lt;模式&gt;/&lt;图&gt;.yaml 可选 session-flow 键的宿主词汇):ARENA 单场解散回大厅 / ROTATION 同会话局末投票续局链;决定 recipe extras(与 MatchBootstrap 同源)。
 *
 * <p>来源:04 §4.4;README 对齐注⑨⑩适用;overview 模块表。</p>
 */
public enum SessionFlowPolicy {

    /** 单场解散:局末回大厅。 */
    ARENA,

    /** 同会话局末投票续局链。 */
    ROTATION;

    /** 由单个图配置解析其会话流程策略(可选 session-flow 键;缺省/未知值 ARENA)。 */
    public static SessionFlowPolicy of(GameConfig game) {
        return "rotation".equalsIgnoreCase(game.sessionFlow()) ? ROTATION : ARENA;
    }

    // TODO(2.5): recipeExtras() 本阶段即返回 List.of(new EmptyArenaComponent.Spec(Duration.ofSeconds(10)))(空场 ~10s 解散,DissolveReturnTest 依赖);MatchBootstrap 经此取 flowExtras —— 出自 2.5:39
    // TODO(5.2): rotation 分支:局末投票组件(宿主件,本骨架留位不落文件)+EndGate 产 TransitionIntent(new MatchModeSelection(modeId, Optional.of(mapId), false), Handoff.of(战报键, summary)) 续局,force=false 被拒由宿主重试 —— 出自 04 §4.4
    /** 本策略对应的 SessionRecipe.extraComponents 贡献(MatchBootstrap 同源取用)。 */
    public List<SessionComponent.Specification<?>> recipeExtras() {
        return List.of();
    }
}
