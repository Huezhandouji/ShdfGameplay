package com.shdfGameplay.bridge.role;

import java.util.UUID;

/**
 * 宿主词汇的角色生命周期信号:角色插件事件流经 RoleFacade 翻译后的只读载体(事件类型不出 bridge/role)。
 * 见序合约:DEATH 恒为死时所持角色且随后必跟 CLEAR;换角=CLEAR(旧)+SET(新)按到达顺序;
 * 掉线/故障隔离/停用回收只发 CLEAR。均为主线程同步送达。
 */
public record RoleSignal(Kind kind, UUID player, String roleId) {

    /** 信号种类:与角色插件 api/event 三事件 1:1。 */
    public enum Kind {
        /** 角色被设置(RoleSetEvent;换角时先旧角 CLEAR 后本条)。 */
        SET,
        /** 角色被清除(RoleClearEvent;死亡清角在 DEATH 之后到达)。 */
        CLEAR,
        /** 持角玩家死亡(RoleDeathEvent;roleId=死时所持角色,本条替换宿主原 LOWEST 抢读探测)。 */
        DEATH
    }
}
