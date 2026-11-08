package com.shdfGameplay.component.role;

import com.shdfGameplay.bridge.role.RoleCatalog;
import com.shdfGameplay.bridge.role.RoleFacade;
import com.shdfGameplay.bridge.role.RolePolicy;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/**
 * ①授权与占用 facet:解释 RolePolicy 的职责链(目录校验/全局禁用拒/按队池/占用去重/换角限额/人数门/记账-应用分离/死亡善后/离开释放),包私有内部接缝,构造于 awake。
 * 来源:09-角色组件层 §9.3/§9.4;08-角色桥接 §8.6;施工计划/阶段3 3.4-RoleComponent四facet 步骤5①;README 对齐注①⑤。
 */
final class LoadoutFacet {

    private final RoleFacade facade;
    private final Map<String, UUID> occupied;
    private final Map<UUID, Integer> reselects;

    LoadoutFacet(RoleFacade facade, Map<String, UUID> occupied, Map<UUID, Integer> reselects) {
        this.facade = facade;
        this.occupied = occupied;
        this.reselects = reselects;
    }

    /**
     * 选角请求统一裁决入口:按 RolePolicy 职责链逐条判定,坏数据降级为该玩家无角色不炸实例。
     */
    RoleComponent.AssignmentResult request(UUID player, String roleId, RolePolicy policy, RoleCatalog catalog,
            Function<UUID, String> teamOf) {
        // TODO(3.4): 职责链逐条照 3.4 步骤5①:isValidRole 目录拒(ROLE_INVALID)/globallyBanned 拒(BANNED)/poolByTeam 按队池(NOT_IN_POOL)——队组件缺席(可选依赖,TeamComponent 属框架线阶段4 交付)而池非空→拒绝并在消息里点名装配声明错误,不降级全局池/allowDuplicates=false 占用去重含跨队(OCCUPIED)/maxReselectsPerPlayer 限额(-1 不限 0 禁换,RESELECT_LIMITED)/minParticipantsBeforeSelect 人数门(MIN_PARTICIPANTS)/applyOnDeployOnly=true 只记账(WRONG_TIMING)由 applyPending 应用 —— 出自 3.4-RoleComponent四facet 步骤5①(:71-78);08-角色桥接 §8.6。
        return new RoleComponent.AssignmentResult(false, RoleComponent.AssignmentResult.Reason.ROLE_INVALID);
    }

    /**
     * 部署/复活统一应用点:applyOnDeployOnly 记账的授权在此真正 setRole。
     */
    void applyPending(UUID player) {
        // TODO(3.4): 经 facade.setRole 应用记账中的授权(主线程);无记账则为空操作 —— 出自 3.4-RoleComponent四facet 步骤5①;09-角色组件层 §9.3 读口 applyPending 行。
    }

    /**
     * 成员离开实例时释放占用并清角(ParticipantLeft 订阅路径触发)。
     */
    void release(UUID player) {
        // TODO(3.4): 释放该玩家占用(occupied 反查移除)+facade.clearRole(幂等);release 由 ParticipantLeft 订阅触发,与角色系统掉线清角互为双保险非双写者 —— 出自 3.4-RoleComponent四facet 步骤5①(:80);09-角色组件层 §9.5 分工表末行。
    }
}
