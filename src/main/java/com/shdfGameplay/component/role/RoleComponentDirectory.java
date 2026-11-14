package com.shdfGameplay.component.role;

import com.shdfGameplay.bridge.role.RoleSignal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * 宿主侧角色组件登记簿:RoleComponent awake 登记/stop 摘除,参与者只经实例事件维护;
 * 官方角色信号(dispatch)与命令层经 lookup 把玩家路由到所在实例的 RoleComponent——弥补 HydrokarbsApi 无"玩家→实例→组件"通道。
 */
public final class RoleComponentDirectory {

    // TODO(3.4): register/unregister 幂等;awake 期 register(自身+空参与者集)——首切实例先建后逐人句柄 join(04-大厅模式 §4.3),参与者此后只经实例事件 MatchModeInstanceEvent$ParticipantJoined/$ParticipantLeft(jar 已核,record(int,long,UUID))或 svc().self() 参与者读口维护;★禁绑 StateFacet 采样点(采样关闭模式 sampleIntervalTicks=0 不采样,09 §9.6/:143——绑采样点则采样关闭模式的 Directory 恒空→阶段3.5 闸门②击杀合成与 5.2 计分链静默断裂) —— 出自 方案玩家→组件路由裁决;04-大厅模式 §4.3;09-角色组件层 §9.6。

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
     * 官方角色信号路由(L0 订阅回调唯一落点,原 listener/RoleSignalListener 死亡路由的替代):
     * DEATH→lookup 后交所在实例 RoleComponent#onRoleDeath;只路由,不合成。
     */
    public static void dispatch(RoleSignal signal) {
        // TODO(3.5): signal.kind()==DEATH → lookup(signal.player()).ifPresent(c -> c.onRoleDeath(signal.player(), signal.roleId()));
        // SET/CLEAR 暂不路由(occupied 账本纠偏为后续扩展点,届时在此加分支);会话外(路人)信号 lookup 无果静默返回不产实例事件;
        // 订阅接线住 L0(RoleFacade#subscribeRoleEvents(RoleComponentDirectory::dispatch)) —— 出自 3.5-监听器与击杀归属 步骤1 改造;08-角色桥接 §8.9。
    }

    /**
     * 玩家→所在实例的 RoleComponent 路由查询:不在会话/当前实例未挂角色组件返回 empty。
     */
    public static Optional<RoleComponent> lookup(UUID player) {
        // TODO(3.5): dispatch(官方死亡信号路由)与 DebugCommand.printRoleInfo 统一经本口路由;★23 兼容——listener/command 只 import component.role,不触 bridge.role;若框架线后续开放组件查询 API,回方案评估替换为框架通道 —— 出自 方案玩家→组件路由裁决;3.5-监听器与击杀归属 步骤1/步骤5。
        return Optional.empty();
    }
}
