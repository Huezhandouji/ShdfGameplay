package com.shdfGameplay.component.role;

import com.hydrokarbs.engine.event.MatchModeInstanceEvent;
import java.util.UUID;

/**
 * 击杀归属合成事件:死前读序合成的实例事件词汇(普通 record 实现框架事件开放根,不进 Bukkit 总线)。
 * 来源:08-角色桥接 §8.4 v3.1 注;09-角色组件层 §9.3;施工计划/阶段3 3.4-RoleComponent四facet 步骤2;README 对齐注①⑤。
 */
public record RoleKillEvent(
        UUID killerUuid,
        UUID victimUuid,
        String victimRoleId,
        int tick,
        long occurredAtMillis
) implements MatchModeInstanceEvent {

    // TODO(3.4): 接口是双抽象法 tick()+occurredAtMillis()(javap 已核,仅 is() 为 default),第 5 分量 long occurredAtMillis 为必需 record 分量(Java record 不能新增实例字段,4 分量无法实现 occurredAtMillis()——照框架自家 record 形态,如 MatchModeInstanceEvent$ParticipantKilled(int,long,UUID,UUID,DamageCause));不带队伍字段(消费者经 getComponent(TeamComponent.class) 自查,TeamComponent 属框架线阶段4 交付);不是 Bukkit Event 不进服务端总线 —— 出自 3.4-RoleComponent四facet 步骤2;08-角色桥接 §8.4。

    /**
     * 紧凑工厂:occurredAtMillis 取构造时刻(System.currentTimeMillis())。
     */
    public static RoleKillEvent of(UUID killer, UUID victim, String roleId, int tick) {
        return new RoleKillEvent(killer, victim, roleId, tick, System.currentTimeMillis());
    }
}
