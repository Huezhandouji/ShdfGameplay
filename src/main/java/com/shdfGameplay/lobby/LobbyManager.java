package com.shdfGameplay.lobby;

import com.hydrokarbs.api.HydrokarbsApi;
import com.shdfGameplay.config.LobbyConfig;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Location;

/**
 * 主世界大厅管理器:大厅区域进出判定与 present 账本(不聚合投票/准备/开局三件——组合住 L0 装配点与开局周期壳,2.5 裁决)。
 * 运行态闸:open 时记入 LobbyConfig 并清账本;未 open 时 enter 无操作、present 恒空——
 * 由此「插件加载完成即不进入大厅运行态」成立,监听器无需感知开关。
 * 三事件口径:onMove 跨方块位移进出场判定(bounds 世界+区域)、onJoin 自查接回(不在账本且不在任何会话
 * →传回出生点并记账)、QUIT 当即清账复用 leave(大厅无断线宽限)——listener/LobbyEventsListener 只路由,
 * 判定与账本住本类。
 * —— 出自 本次任务(shdf-launchstop-流程开关)设计文档 §4.2;04-大厅模式 §4.2;2.5 步骤2。
 */
public final class LobbyManager {

    // /shdf lobby 传送进场归命令面:传送成功即 enter 记账(实现住 command/ShdfCommands;候选行由周期壳差集补发) —— 出自 04 §4.2、2.5 步骤10
    // 大厅不是会话不是模式:不建会话/不注册 MatchMode/不占世界副本/不经框架名册;lobby 包对框架 import 白名单恰四类
    // (api.HydrokarbsApi/api.SessionEventView/api.SessionRecipe/engine.session.JoinResult;SessionHandle 经 var 承接
    // 建会话返回值不显式 import;MatchBootstrapGateTest 扫描冻结) —— 出自 README 对齐注⑩、04 §4.8 命题④

    /** present 账本:当前身处主世界大厅区域的玩家(open 注入配置起记账,close 清空弃配置)。 */
    private final Set<UUID> players = new HashSet<>();

    /** 框架 API(join 自查经 sessionOf 查「在任一会话」;白名单类)。 */
    private final HydrokarbsApi hk;

    /** 大厅回传缝(join 接回传出生点;生产 Bukkit/测试 fake 各一适配器)。 */
    private final LobbyTeleport teleport;

    /** 大厅配置(open 注入缓存实例;null = 未 open,单一状态源住本类)。 */
    private LobbyConfig config;

    /**
     * @param hk 框架 API(仅用 sessionOf,join 自查口径)
     * @param teleport 大厅回传缝(join 接回传 lobby.spawn)
     */
    public LobbyManager(final HydrokarbsApi hk, final LobbyTeleport teleport) {
        this.hk = hk;
        this.teleport = teleport;
    }

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

    /**
     * PlayerMove 跨方块位移后的进出场判定(节流住监听器:纯视角位移不进本口):
     * 落点世界与 bounds 一致且在区域内 enter、否则 leave(含跨世界位移)——幂等自愈账本。
     *
     * @param player 玩家 uuid
     * @param to 位移落点(监听器保证非 null)
     */
    public void onMove(final UUID player, final Location to) {
        if (this.config == null) {
            return;
        }
        if (inside(to)) {
            enter(player);
        } else {
            leave(player);
        }
    }

    /**
     * 玩家进服自查接回:不在账本且不在任何会话(宽限满被静默移除者重进场景)→
     * 传回大厅出生点并记账;在任一会话(对局重连)不打扰;未 open 无操作。
     *
     * @param player 玩家 uuid
     */
    public void onJoin(final UUID player) {
        if (this.config == null || this.players.contains(player) || this.hk.sessionOf(player).isPresent()) {
            return;
        }
        if (this.teleport.toSpawn(player, this.config.spawn())) {
            enter(player);
        }
    }

    /** 大厅区域内判定:落点世界与 bounds.world 一致且点在区域内(Objects.equals:null 对 null,离线测试直接构造两侧 null 世界)。 */
    private boolean inside(final Location at) {
        return Objects.equals(at.getWorld(), this.config.boundsWorld())
                && this.config.bounds().contains(at.toVector());
    }

    /** 玩家进入大厅记账(区域踏入、/shdf lobby 传送或 join 接回;未 open 无操作)。 */
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
