package com.shdfGameplay.component.role;

import com.hydrokarbs.engine.event.MatchModeInstanceEvent;
import com.shdfGameplay.bridge.role.RoleFacade;
import com.shdfGameplay.bridge.role.RoleStateAccess;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * ②③④ facet 容器:StateFacet(采样差分)/HostilityFacet(敌对口径唯一封装)/RewardsFacet(事件→角色操作)——均包私有 final 内部接缝,不是子组件,构造于 awake。
 * 来源:09-角色组件层 §9.3 决断/§9.4;施工计划/阶段3 3.4-RoleComponent四facet 步骤5②③④;README 对齐注①⑤。
 */
final class RoleFacets {

    private RoleFacets() {
    }

    // TODO(3.4): facet 是内部接缝不是子组件,不拆独立组件;构造于 awake(注入 facade、new RoleStateAccess(facade)、组件持有的三表);对外只经 RoleComponent 读口 —— 出自 09-角色组件层 §9.3"为什么不拆四个组件"决断;3.4-RoleComponent四facet 步骤5。

    /**
     * ②数值与采样 facet:持 lastSeen 快照表引用,组装一帧并经 RoleEventSynth.diffSample 差分后发实例总线。
     */
    static final class StateFacet {

        private final Map<UUID, RoleStateAccess.RoleSnapshot> lastSeen;
        private final RoleStateAccess stateAccess;

        StateFacet(Map<UUID, RoleStateAccess.RoleSnapshot> lastSeen, RoleStateAccess stateAccess) {
            this.lastSeen = lastSeen;
            this.stateAccess = stateAccess;
        }

        /**
         * 采一帧并差分:值真的变了才产 RoleStateChangedEvent(调度门在组件 update,此处只管一帧)。
         */
        void sample() {
            // TODO(3.4): 对全体持角成员采一帧(energy/sanTe 经 stateAccess,无角色者跳过)→ 调 RoleEventSynth.diffSample → 逐条发实例总线(发布口经组件侧 svc().self().events(),facet 自身不持 svc);快照表 lastSeen 由组件持有此处只读写 —— 出自 3.4-RoleComponent四facet 步骤5②(:81);08-角色桥接 §8.4 注②。
        }
    }

    /**
     * ③敌对口径 facet:areHostile 非对称原语(在 a 的实例上判;a 无实例⇒true)的唯一封装点,桥接原语不外露给模式。
     */
    static final class HostilityFacet {

        private final RoleFacade facade;

        HostilityFacet(RoleFacade facade) {
            this.facade = facade;
        }

        /**
         * 敌对判定:统一封装非对称口径,调用方(其他组件/模式)不再自己记口径。
         */
        boolean areHostile(UUID a, UUID b) {
            // 转发 facade.areHostile(a, b)(非对称:在 a 的实例上判,a 无实例⇒true"没有阵营⇒敌人",任一方为 null⇒false)——敌对原语不外露,其他组件一律经 RoleComponent#areHostile —— 出自 3.4-RoleComponent四facet 步骤5③(:82);08-角色桥接 §8.2/§8.10(二)。
            return false;
        }
    }

    /**
     * ④事件→角色操作 facet:遍历 Spec.rewards,matches 命中即经 RoleOpContext apply。
     */
    static final class RewardsFacet {

        private final List<RoleReward> rewards;

        RewardsFacet(List<RoleReward> rewards) {
            this.rewards = rewards;
        }

        /**
         * 奖励求值:含自己合成的 RoleKillEvent 的回环求值(击杀奖励正是设计要的)。
         */
        void evaluate(MatchModeInstanceEvent event) {
            // TODO(3.4): 遍历 rewards,matches 命中即 apply(经 RoleOpContext 三动词);含自己合成的 RoleKill 回环求值(事件回环是形状不是 bug,当前映射不产生新 RoleKill 无环) —— 出自 3.4-RoleComponent四facet 步骤5④(:83);3.4 关键实现要点"事件回环是形状不是 bug"行。
        }
    }
}
