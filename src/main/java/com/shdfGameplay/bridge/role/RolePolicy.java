package com.shdfGameplay.bridge.role;

import java.util.Map;
import java.util.Set;

/**
 * 模式对角色系统的「规则请求」纯数据(恰八字段+AssignmentStrategy+NONE 常量):机制=声明不是命令,
 * 解释执行全在 RoleComponent 的 Loadout facet(组件内写 if (policy == X_POLICY) 即违规);
 * 奖励不经本 record(只住 RoleComponent.Spec 的 rewards)。
 */
public record RolePolicy(
        /** false = 同一角色全场唯一(含跨队),true = 允许重复。 */
        boolean allowDuplicates,
        /** 每队可用角色池;空集 = 该队不限制(用全量目录)。键为队伍 id 字符串,桥接不认识任何具体队伍 id。 */
        Map<String, Set<String>> poolByTeam,
        /** 全局禁用列表(无论池子怎么写都不允许)。 */
        Set<String> globallyBanned,
        /** 必须在场人数达到此值后才允许选角(防单人偷跑)。 */
        int minParticipantsBeforeSelect,
        /** true = 选角只记录,部署/复活时才真正 setRole。 */
        boolean applyOnDeployOnly,
        /** true = 死亡时立刻 clearRole,重生再 setRole。 */
        boolean clearOnDeath,
        /** 未选角的参赛者如何兜底。 */
        AssignmentStrategy autoAssign,
        /** 每个玩家整局内可换角色的次数上限(-1 = 不限)。 */
        int maxReselectsPerPlayer
) {

    /** 未选角参赛者的兜底策略(08 §8.6)。 */
    public enum AssignmentStrategy {
        NONE,
        RANDOM_AVAILABLE,
        FIRST_AVAILABLE,
        BALANCED
    }

    /** 大厅/无角色模式的「不介入」策略——组件直接不挂,这里只是显式表达(08 §8.6)。 */
    public static final RolePolicy NONE = new RolePolicy(
            true, Map.of(), Set.of(), 0, false, false, AssignmentStrategy.NONE, -1);

    // TODO(3.2): 字段名与 08 §8.6 逐字一致(3.2:57,RolePolicyTest 反射断言恰 8 组件防增删改名);record 零行为,解释只在 Loadout facet —— 出自 08-角色桥接 §8.6/施工计划3.2 步骤3

    // TODO(3.2): 三策(BOMB/TDM/BOSS)逐字段值以 component/role 的 RolePolicies 为准(常量住组件包,★23 豁免位置,08 §8.6"常量在各自工厂里"与 ★23 的机械冲突已列 3.2 待裁决);奖励不经本 record(只住 RoleComponent.Spec(List&lt;RoleReward&gt;)/RoleReward) —— 出自 08-角色桥接 §8.6/施工计划3.2 步骤4/待裁决
}
