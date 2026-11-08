package com.shdfGameplay.component.role;

import com.shdfGameplay.bridge.role.RolePolicy;

/**
 * 三策常量持有类:BOMB/TDM/BOSS 三策(纯数据 record,Spec 冻结后跨模式共享安全),值元组按 08 §8.6 字段序。
 * 来源:08-角色桥接 §8.6 声明示例;09-角色组件层 §9.6;施工计划/阶段3 3.2-类型化访问与策略数据件 步骤4;README 对齐注①⑤。
 */
public final class RolePolicies {

    // TODO(3.2): 逐字段按 08 §8.6 字段序(allowDuplicates, poolByTeam, globallyBanned, minParticipantsBeforeSelect, applyOnDeployOnly, clearOnDeath, autoAssign, maxReselectsPerPlayer):
    //   BOMB  = (false, {ATTACKER→[red,canglu], DEFENDER→[sinThorn,remoteness]}, [selfUpdateExample], 4, true, true,  RANDOM_AVAILABLE, 1)
    //   TDM   = (true,  {}, [], 2, false, false, FIRST_AVAILABLE, -1)
    //   BOSS  = (false, {RAIDERS→[meiqihezi,sinThorn]}, [], 3, true, false, NONE, 0)
    //   骨架期以 null 占位,3.2 以 new RolePolicy(...) 冻结(Spec 冻结后跨模式共享安全) —— 出自 08-角色桥接 §8.6;3.2-类型化访问与策略数据件 步骤4。

    /** 严格池/禁重复/部署时应用/死亡即清(allowDuplicates=false,按队池,部署记账-应用分离,clearOnDeath 善后)。 */
    public static final RolePolicy BOMB_POLICY = null; // 按 08 §8.6 BOMB 元组冻结 —— 出自 08-角色桥接 §8.6(:245-254)。

    /** 宽松/可重复/全员池/复活即应用(快速迭代手感)。 */
    public static final RolePolicy TDM_POLICY = null; // 按 08 §8.6 TDM 元组冻结 —— 出自 08-角色桥接 §8.6(:257-259)。

    /** 锁定池/禁换角(maxReselectsPerPlayer=0,保证首领机制可解)。 */
    public static final RolePolicy BOSS_POLICY = null; // 按 08 §8.6 BOSS 元组冻结 —— 出自 08-角色桥接 §8.6(:262-264)。

    private RolePolicies() {
    }
}
