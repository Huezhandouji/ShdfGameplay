package com.shdfGameplay.lobby;

import java.util.UUID;
import org.bukkit.Location;

/**
 * 大厅回传缝:玩家→出生点传送的唯一实现点(生产 Bukkit/测试 fake 各一适配器)。
 * 消费方:GameFlow stop 两段传送、LobbyManager join 自查接回;
 * 2.5 的 LobbyReturnListener MemberLeft 单人传送将来复用同一缝。
 * —— 出自 本次任务(shdf-launchstop-流程开关)设计文档 §4.3;04-大厅模式 §4.6/§4.8 传送分工。
 */
public interface LobbyTeleport {

    /**
     * 传送在线玩家到指定出生点。
     *
     * @param player 玩家 uuid
     * @param spawn 大厅出生点(config.yml lobby.spawn)
     * @return 传送成功 true;玩家离线或传送异常返回 false(调用方跳过,不抛)
     */
    boolean toSpawn(UUID player, Location spawn);
}
