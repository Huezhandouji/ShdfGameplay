package com.shdfGameplay.lobby;

import com.shdfGameplay.config.LobbyConfig;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Server;
import org.bukkit.event.player.PlayerVelocityEvent;

/**
 * 主世界大厅管理器:大厅区域进出判定与 present 账本,聚合大厅三件(LobbyVote/ReadyState/MatchBootstrap)。
 * 运行态闸(本次新增):open 时记入 LobbyConfig 并清账本;未 open 时 enter 无操作、present 恒空——
 * 由此「插件加载完成即不进入大厅运行态」成立,监听器无需感知开关。
 * —— 出自 本次任务(shdf-launchstop-流程开关)设计文档 §4.2。
 */
public final class LobbyManager {

    // TODO(2.5): bounds 承接 PlayerMove 节流 contains 判定 —— 出自 04 §4.2、2.5 步骤2
    // TODO(2.5): 三事件(PlayerMoveEvent 节流 bounds.contains 判定 / /shdf lobby 进场 / PlayerQuitEvent 清账 / PlayerJoinEvent 自查接回)由 listener/LobbyEventsListener 只路由,账本与判定住本类;大厅无断线宽限,QUIT 当即清 —— 出自 04 §4.2、2.5 步骤2
    // TODO(2.5): 大厅不是会话不是模式:不建会话/不注册 MatchMode/不占世界副本/不经框架名册;lobby 包对框架 import 白名单恰六类(SessionHandle 经 var 承接 createSession 返回值,不显式 import) —— 出自 README 对齐注⑩

    /** present 账本:当前身处主世界大厅区域的玩家(open 注入配置起记账,close 清空弃配置)。 */
    private final Set<UUID> players = new HashSet<>();

    /** 大厅配置(open 注入缓存实例;null = 未 open,单一状态源住本类)。 */
    private LobbyConfig config;

    /** 大厅是否开放(config 在场性判定;open/close 由 GameFlow 随流程开关单调调用,本类不做幂等承诺)。 */
    public boolean isOpen() {
        return this.config != null;
    }

    /** 启动:记配置、清账本(注入 onEnable 已缓存实例,不读盘)。 */
    public void open(LobbyConfig config) {
        this.config = config;
        this.players.clear();
    }

    /** 停止:清账本、弃配置。 */
    public void close() {
        this.players.clear();
        this.config = null;
    }

    /** present 账本只读视图:当前身处主世界大厅区域的玩家(未 open 恒空集)。 */
    public Set<UUID> present() {
        return this.config == null ? Set.of() : Set.copyOf(this.players);
    }

    /** 玩家进入大厅记账(区域踏入或 /shdf lobby 传送;未 open 无操作)。 */
    public void enter(UUID player) {
        if (this.config == null) {
            return;
        }
        this.players.add(player);
    }

    /** 玩家离开大厅清账(加入会话托管、主动离开或断线;未 open 无操作)。 */
    public void leave(UUID player) {
        if (this.config == null) {
            return;
        }
        this.players.remove(player);
    }
}
