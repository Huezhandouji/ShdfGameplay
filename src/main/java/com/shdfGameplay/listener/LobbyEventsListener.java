package com.shdfGameplay.listener;

import com.shdfGameplay.lobby.LobbyManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * 大厅三事件路由件(方案新增):PlayerMove 节流进出场 / PlayerQuit 清账 / PlayerJoin 自查接回——住 listener/ 包(全工程唯一允许 @EventHandler 的层),只路由不做大厅业务。
 *
 * <p>来源:04 §4.2(:55-58);2.5 步骤2;12-代码骨架.md:202 与 3.5:108(listener/ 唯一 @EventHandler 层);本方案新增裁决。</p>
 */
public final class LobbyEventsListener implements Listener {

    private final LobbyManager lobby;

    /** 构造注入只进数据(LobbyManager 引用)不进行为、不做服务定位。 */
    public LobbyEventsListener(LobbyManager lobby) {
        this.lobby = lobby;
    }

    // TODO(2.5): 主类 onEnable 一次性注册;本类只路由,大厅业务住 LobbyManager —— 出自 2.5 步骤2、12-代码骨架.md:202
    // TODO(2.5): PlayerJoin 自查口径:不在大厅且不在任何会话→接回大厅(宽限满被静默移除者重进场景) —— 出自 04 §4.2

    /** PlayerMove 节流后转 LobbyManager 判定进出场。 */
    @EventHandler
    public void onMove(PlayerMoveEvent event) {
    }

    /** PlayerQuit 转 LobbyManager 当即清账(大厅无断线宽限)。 */
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
    }

    /** PlayerJoin 转大厅自查接回。 */
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
    }
}
