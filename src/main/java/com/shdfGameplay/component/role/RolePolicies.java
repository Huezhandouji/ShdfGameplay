package com.shdfGameplay.component.role;

import com.shdfGameplay.bridge.role.RolePolicy;

import java.util.Map;
import java.util.Set;

/**
 * 三策常量持有类:BOMB/TDM/BOSS 三策(纯数据 record,Spec 冻结后跨模式共享安全),值元组按 08 §8.6 字段序。
 * 来源:08-角色桥接 §8.6 声明示例;09-角色组件层 §9.6;施工计划/阶段3 3.2-类型化访问与策略数据件 步骤4;README 对齐注①⑤。
 */
public final class RolePolicies {

    /** 严格池/禁重复/部署时应用/死亡即清(allowDuplicates=false,按队池,部署记账-应用分离,clearOnDeath 善后)。 */
    public static final RolePolicy BOMB_POLICY = new RolePolicy(
            /* allowDuplicates */ false,
            /* poolByTeam */ Map.of("ATTACKER", Set.of("red", "canglu"),
                    "DEFENDER", Set.of("sinThorn", "remoteness")),
            /* globallyBanned */ Set.of("selfUpdateExample"),
            /* minParticipantsBeforeSelect */ 4,
            /* applyOnDeployOnly */ true,
            /* clearOnDeath */ true,
            /* autoAssign */ RolePolicy.AssignmentStrategy.RANDOM_AVAILABLE,
            /* maxReselectsPerPlayer */ 1); // 按 08 §8.6 BOMB 元组冻结 —— 出自 08-角色桥接 §8.6(:245-254)。

    /** 宽松/可重复/全员池/复活即应用(快速迭代手感)。 */
    public static final RolePolicy TDM_POLICY = new RolePolicy(
            /* allowDuplicates */ true,
            /* poolByTeam */ Map.of(),
            /* globallyBanned */ Set.of(),
            /* minParticipantsBeforeSelect */ 2,
            /* applyOnDeployOnly */ false,
            /* clearOnDeath */ false,
            /* autoAssign */ RolePolicy.AssignmentStrategy.FIRST_AVAILABLE,
            /* maxReselectsPerPlayer */ -1); // 按 08 §8.6 TDM 元组冻结 —— 出自 08-角色桥接 §8.6(:257-259)。

    /** 锁定池/禁换角(maxReselectsPerPlayer=0,保证首领机制可解)。 */
    public static final RolePolicy BOSS_POLICY = new RolePolicy(
            /* allowDuplicates */ false,
            /* poolByTeam */ Map.of("RAIDERS", Set.of("meiqihezi", "sinThorn")),
            /* globallyBanned */ Set.of(),
            /* minParticipantsBeforeSelect */ 3,
            /* applyOnDeployOnly */ true,
            /* clearOnDeath */ false,
            /* autoAssign */ RolePolicy.AssignmentStrategy.NONE,
            /* maxReselectsPerPlayer */ 0); // 按 08 §8.6 BOSS 元组冻结 —— 出自 08-角色桥接 §8.6(:262-264)。

    private RolePolicies() {
    }
}
