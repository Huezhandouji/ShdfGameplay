package com.shdfGameplay.listener;

import com.shdfGameplay.lobby.LobbyManager;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * 大厅三事件路由件:PlayerMove 节流进出场 / PlayerQuit 清账 / PlayerJoin 自查接回——住 listener/ 包(全工程唯一允许 @EventHandler 的层),只路由不做大厅业务。
 * 主类 onEnable 一次性注册,不随流程开关反复;判定与账本住 LobbyManager —— 出自 04 §4.2、2.5 步骤2、12-代码骨架.md:202。
 */
public final class LobbyEventsListener implements Listener {

    private final LobbyManager lobby;

    /** 构造注入只进数据(LobbyManager 引用)不进行为、不做服务定位。 */
    public LobbyEventsListener(LobbyManager lobby) {
        this.lobby = lobby;
    }

    /**
     * PlayerMove 节流后转 LobbyManager 判定进出场:仅跨方块位移触发判定(纯视角位移不判);
     * PlayerTeleportEvent 为本事件子类,传送落点同口径判定。
     */
    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        final Location to = event.getTo();
        if (to == null || !event.hasChangedBlock()) {
            return;
        }
        this.lobby.onMove(event.getPlayer().getUniqueId(), to);
    }

    /** PlayerQuit 转 LobbyManager 当即清账(大厅无断线宽限)。 */
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        this.lobby.leave(event.getPlayer().getUniqueId());
    }

    /** PlayerJoin 转 LobbyManager 自查接回(不在大厅且不在任何会话→传回出生点并记账)。 */
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        this.lobby.onJoin(event.getPlayer().getUniqueId());
    }
}
