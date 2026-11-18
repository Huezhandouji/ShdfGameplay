package com.shdfGameplay.lobby;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 大厅成员准备账本:记录/查询成员 ready 标记,allReady(在场者) 非空且全 ready 才视为「立即开局」信号
 * (人数门槛仍住开局门);周期壳每拍 retainPresent 对齐大厅账本清离场者标记,周期停止与开局失败节流
 * 两口径 clearAll 全清(玩家重新 /shdf ready 触发)。纯宿主状态零框架类型,主线程维护。
 * —— 出自 2.5 大厅逻辑(04-大厅模式 §4.2、§4.3;2.5 步骤4)。
 */
public final class ReadyState {

    /** 准备账本:已 ready 的玩家集合(未记入即未准备;主线程维护,无并发面)。 */
    private final Set<UUID> ready = new HashSet<>();

    /** 记/清单个玩家的 ready 标记(ready=false 移出账本)。 */
    public void setReady(final UUID player, final boolean ready) {
        if (ready) {
            this.ready.add(player);
        } else {
            this.ready.remove(player);
        }
    }

    /** 单个玩家是否已准备(未记入视为未准备)。 */
    public boolean isReady(final UUID player) {
        return this.ready.contains(player);
    }

    /**
     * 全员 ready 判定:who 非空且全员已记 ready 才 true(空集=未就绪,人数门槛仍住开局门)。
     *
     * @param who 在场者集合(周期壳每拍传大厅 present 账本)
     */
    public boolean allReady(final Collection<UUID> who) {
        return !who.isEmpty() && this.ready.containsAll(who);
    }

    /** 清不在场者的 ready 记录(周期壳每拍对齐大厅账本:离场即弃标记,免改 LobbyManager 构造)。 */
    public void retainPresent(final Collection<UUID> present) {
        this.ready.retainAll(present);
    }

    /** 全清(周期停止全清;开局失败节流清,enableReady 门打回 WAIT 由玩家重新准备触发)。 */
    public void clearAll() {
        this.ready.clear();
    }
}
