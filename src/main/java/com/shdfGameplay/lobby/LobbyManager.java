package com.shdfGameplay.lobby;

import java.util.Set;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.event.player.PlayerVelocityEvent;
import org.bukkit.util.BoundingBox;

/**
 * 主世界大厅管理器:大厅区域进出判定与 present 账本,聚合大厅三件(LobbyVote/ReadyState/MatchBootstrap)。
 */
public final class LobbyManager {

    /**
     * 大厅配置载体(config.yml lobby 块):区域边界、大厅出生点、投票周期秒数、开局最少人数。
     */
    public record LobbyConfig(BoundingBox bounds, Location spawn, int voteSeconds, int minPlayers) {
        // TODO(2.5): 由配置装载从 config.yml lobby 块装填;bounds 承接 PlayerMove 节流 contains 判定 —— 出自 04 §4.2、2.5 步骤2
    }

    // TODO(2.5): 三事件(PlayerMoveEvent 节流 bounds.contains 判定 / /shdf lobby 进场 / PlayerQuitEvent 清账 / PlayerJoinEvent 自查接回)由 listener/LobbyEventsListener 只路由,账本与判定住本类;大厅无断线宽限,QUIT 当即清 —— 出自 04 §4.2、2.5 步骤2
    // TODO(2.5): 大厅不是会话不是模式:不建会话/不注册 MatchMode/不占世界副本/不经框架名册;lobby 包对框架 import 白名单恰六类(SessionHandle 经 var 承接 createSession 返回值,不显式 import) —— 出自 README 对齐注⑩

    /** present 账本只读视图:当前身处主世界大厅区域的玩家。 */
    public Set<UUID> present() {
        return Set.of();
    }

    /** 玩家进入大厅记账(区域踏入或 /shdf lobby 传送)。 */
    public void enter(UUID player) {
    }

    /** 玩家离开大厅清账(加入会话托管、主动离开或断线)。 */
    public void leave(UUID player) {
    }
}
