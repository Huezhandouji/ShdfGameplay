package com.shdfGameplay.component.role;

import com.hydrokarbs.engine.event.MatchModeInstanceEvent;
import com.shdfGameplay.bridge.role.RoleStateAccess;
import java.util.UUID;

/**
 * 状态差分合成事件:采样差分后"值真的变了"才发的当前帧(普通 record 实现框架事件开放根,不进 Bukkit 总线)。
 * 来源:08-角色桥接 §8.4 v3.1 注;09-角色组件层 §9.4 update 行;施工计划/阶段3 3.4-RoleComponent四facet 步骤2;README 对齐注①⑤。
 */
public record RoleStateChangedEvent(
        UUID who,
        String roleId,
        RoleStateAccess.RoleSnapshot now,
        int tick,
        long occurredAtMillis
) implements MatchModeInstanceEvent {

    // TODO(3.4): 差分后取当前帧,值真的变了才发(buffs 不入帧,帧只含 energy/sanTe 两量,3.2 已定);第 5 分量 occurredAtMillis 与紧凑工厂口径同 RoleKillEvent(接口双抽象法,4 分量无法编译);不是 Bukkit Event 不进服务端总线 —— 出自 3.4-RoleComponent四facet 步骤2;09-角色组件层 §9.4。

    /**
     * 构造时刻工厂:occurredAtMillis 取 System.currentTimeMillis()。
     */
    public static RoleStateChangedEvent of(UUID who, String roleId, RoleStateAccess.RoleSnapshot now, int tick) {
        return new RoleStateChangedEvent(who, roleId, now, tick, System.currentTimeMillis());
    }
}
