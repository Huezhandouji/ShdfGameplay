package com.shdfGameplay.lobby;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Location;

/**
 * LobbyTeleport 记录假件:成功传送记 uuid 与落点(并写入共享调用序日志,与 StubHydrokarbsApi
 * 同一日志供次序断言);预置离线名单内玩家返 false 跳过(离线口径)。
 * —— 出自 本次任务(shdf-launchstop-流程开关)设计文档 §9 测试面构造。
 */
final class RecordingTeleport implements LobbyTeleport {

    private final List<String> callLog;
    private final Set<UUID> offline = new HashSet<>();
    private final List<UUID> teleported = new ArrayList<>();
    private final List<Location> locations = new ArrayList<>();

    /** @param callLog 与 StubHydrokarbsApi 共享的调用序日志(null=不记录) */
    RecordingTeleport(final List<String> callLog) {
        this.callLog = callLog;
    }

    /** 预置离线名单(toSpawn 对这些玩家返 false)。 */
    void markOffline(final UUID... players) {
        this.offline.addAll(List.of(players));
    }

    @Override
    public boolean toSpawn(final UUID player, final Location spawn) {
        if (this.offline.contains(player)) {
            return false;
        }
        this.teleported.add(player);
        this.locations.add(spawn);
        if (this.callLog != null) {
            this.callLog.add("tp:" + player);
        }
        return true;
    }

    /** 成功传送的玩家序列(含重复,第一段+第二段各一笔)。 */
    List<UUID> teleported() {
        return List.copyOf(this.teleported);
    }

    /** 成功传送的落点序列(与 teleported 同长,断言落点==cfg.spawn 用)。 */
    List<Location> locations() {
        return List.copyOf(this.locations);
    }
}
