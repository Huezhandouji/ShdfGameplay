package com.shdfGameplay.component.role;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * 宿主侧角色组件登记簿(方案新增):RoleComponent awake 登记/stop 摘除,参与者只经实例事件维护;listener 与命令层经 lookup 把玩家路由到所在实例的 RoleComponent——弥补 MiniGameApi 无"玩家→实例→组件"通道(sessionOf 仅回扁平 SessionInfo,javap 已核)。
 * 来源:方案「玩家→组件路由裁决」;08-角色桥接 §8.9;09-角色组件层 §9.3;README 对齐注⑤。
 */
public final class RoleComponentDirectory {

    // TODO(3.4): register/unregister 幂等;awake 期 register(自身+空参与者集)——首切实例先建后逐人 joinSession(04-大厅模式 §4.3),参与者此后只经实例事件 MatchModeInstanceEvent$ParticipantJoined/$ParticipantLeft(jar 已核,record(int,long,UUID))或 svc().self() 参与者读口维护;★禁绑 StateFacet 采样点(采样关闭模式 sampleIntervalTicks=0 不采样,09 §9.6/:143——绑采样点则采样关闭模式的 Directory 恒空→阶段3.5 闸门②击杀合成与 5.2 计分链静默断裂) —— 出自 方案玩家→组件路由裁决;04-大厅模式 §4.3;09-角色组件层 §9.6。

    private static final Map<UUID, RoleComponent> BY_PLAYER = new HashMap<>();
    private static final Map<RoleComponent, Set<UUID>> PARTICIPANTS = new HashMap<>();

    private RoleComponentDirectory() {
    }

    /**
     * 登记组件实例与其初始参与者集(awake 期调用,空参与者集起步)。
     */
    public static void register(RoleComponent component, Set<UUID> participants) {
        // 幂等登记(重复 register 覆盖前先摘旧参与者映射) —— 出自 方案玩家→组件路由裁决。
    }

    /**
     * 摘除组件登记(stop 期调用):参与者映射一并回收。
     */
    public static void unregister(RoleComponent component) {
        // 幂等摘除 —— 出自 方案玩家→组件路由裁决。
    }

    /**
     * 参与者入册(实例事件 ParticipantJoined 驱动)。
     */
    public static void onParticipantJoined(RoleComponent component, UUID player) {
        // 只经实例事件维护参与者集合 —— 出自 方案玩家→组件路由裁决。
    }

    /**
     * 参与者出册(实例事件 ParticipantLeft 驱动)。
     */
    public static void onParticipantLeft(RoleComponent component, UUID player) {
        // 只经实例事件维护参与者集合 —— 出自 方案玩家→组件路由裁决。
    }

    /**
     * 玩家→所在实例的 RoleComponent 路由查询:不在会话/当前实例未挂角色组件返回 empty。
     */
    public static Optional<RoleComponent> lookup(UUID player) {
        // TODO(3.5): RoleSignalListener.onDeath 与 DebugCommand.printRoleInfo 统一经本口路由;★23 兼容——listener/command 只 import component.role,不触 bridge.role;若框架线后续开放组件查询 API,回方案评估替换为框架通道 —— 出自 方案玩家→组件路由裁决;3.5-监听器与击杀归属 步骤1/步骤5。
        return Optional.empty();
    }
}
