package com.shdfGameplay.lobby.ui;

import com.shdfGameplay.config.GameConfigManager;
import com.shdfGameplay.config.LobbyConfig;
import com.shdfGameplay.lobby.LobbyManager;
import com.shdfGameplay.lobby.LobbyView;
import com.shdfGameplay.lobby.LobbyVote;
import com.shdfGameplay.lobby.ReadyState;
import com.shdfGameplay.lang.Lang;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * 大厅交互层生产壳(实现 {@link LobbyView},组合物品/投票菜单/侧边计分板三协作者,装配住 L0):
 * 每拍差集驱动——present 快照差集发放/收回物品与挂/撤侧边栏,LobbyVote.revision 差集刷新所有
 * 已打开投票菜单(「有玩家投票即刷新」单一刷新点,覆盖菜单点击//shdf vote 命令/门表自愈摘候选
 * 全部票本变更路径),侧边栏 ≥1Hz 重建(present 变化或全员 ready 翻转即时重建)。
 * 行为面全 Bukkit,离线测试不覆盖(对齐 BukkitMatchCycle 壳纪律,行为覆盖走起服实测)。
 * —— 出自 本次任务(大厅交互层,参照弃用插件 SHDFGamePlugin WaitingPhase 交互形态)。
 */
public final class BukkitLobbyView implements LobbyView {

    /** 侧边栏重建节流间隔(tick,1Hz;present/全员 ready 翻转不受节流)。 */
    private static final int RENDER_INTERVAL_TICKS = 20;

    /** revision 观察哨兵(start 复位;首拍必不等的差集基准)。 */
    private static final long REVISION_SENTINEL = -1L;

    private final LobbyManager lobby;
    private final LobbyVote vote;
    private final ReadyState ready;
    private final LobbyConfig config;
    private final LobbyViewItems items;
    private final LobbyVoteMenu voteMenu;
    private final LobbySidebar sidebar;

    /** 上一拍 present 观察快照(差集发放/收回)。 */
    private Set<UUID> lastPresent = Set.of();

    /** 上一拍票本观察序号(差集刷新开着的菜单)。 */
    private long lastVoteRevision = REVISION_SENTINEL;

    /** 距上次侧边栏重建拍数(节流计数)。 */
    private int ticksSinceRender = RENDER_INTERVAL_TICKS;

    /** 上一拍全员 ready 判定(翻转即时重建侧边栏)。 */
    private boolean lastAllReady;

    /**
     * @param lobby  大厅账本(在场校验与差集面)
     * @param vote   投票状态机(候选/票本观察序号)
     * @param ready  准备账本(准备物品与侧边栏统计)
     * @param games  图配置管理器(投票菜单候选展示三键)
     * @param config 大厅配置(enableReady/人数门)
     */
    public BukkitLobbyView(final LobbyManager lobby, final LobbyVote vote, final ReadyState ready,
                           final GameConfigManager games, final LobbyConfig config) {
        this.lobby = lobby;
        this.vote = vote;
        this.ready = ready;
        this.config = config;
        this.items = new LobbyViewItems(ready, config);
        this.voteMenu = new LobbyVoteMenu(lobby, vote, games);
        this.sidebar = new LobbySidebar(lobby, ready, config);
    }

    /** 启动:注册两件交互物品(右键回调即本类 openVoteMenu/toggleReady 口)并复位差集基准。 */
    @Override
    public void start() {
        this.items.register(this::openVoteMenu, this::toggleReady);
        this.lastPresent = Set.of();
        this.lastVoteRevision = REVISION_SENTINEL;
        this.ticksSinceRender = RENDER_INTERVAL_TICKS;
        this.lastAllReady = false;
    }

    /** 停止:关全部投票菜单→注销物品定义→收回在场者物品→撤侧边栏(幂等,流程 stop 与 disable 共用)。 */
    @Override
    public void stop() {
        this.voteMenu.closeAll();
        this.items.unregister();
        for (final UUID player : this.lastPresent) {
            final Player online = Bukkit.getPlayer(player);
            if (online != null) {
                this.items.strip(online);
            }
        }
        this.sidebar.clear();
        this.lastPresent = Set.of();
    }

    @Override
    public void tick(final long countdownRemaining) {
        final Set<UUID> present = this.lobby.present();
        final boolean presentChanged = !present.equals(this.lastPresent);
        if (presentChanged) {
            syncPresence(present);
        }
        if (this.vote.revision() != this.lastVoteRevision) {
            this.voteMenu.refreshAll();
            this.lastVoteRevision = this.vote.revision();
        }
        this.ticksSinceRender++;
        final boolean allReady = this.ready.allReady(present);
        if (this.ticksSinceRender >= RENDER_INTERVAL_TICKS || presentChanged || allReady != this.lastAllReady) {
            this.sidebar.render(present, countdownRemaining);
            this.ticksSinceRender = 0;
        }
        this.lastAllReady = allReady;
        this.lastPresent = present;
    }

    @Override
    public void openVoteMenu(final Player player) {
        if (!this.lobby.present().contains(player.getUniqueId())) {
            player.sendMessage(Lang.render("lobby.vote.not_in_lobby"));
            return;
        }
        if (this.vote.candidates().isEmpty()) {
            player.sendMessage(Lang.render("lobby.menu.no_candidates"));
            return;
        }
        this.voteMenu.open(player);
    }

    /** 与 /shdf ready 同口径(在场红字/未启用黄字/翻账绿字),翻账后即时换装准备物品。 */
    @Override
    public void toggleReady(final Player player) {
        final UUID uuid = player.getUniqueId();
        if (!this.lobby.present().contains(uuid)) {
            player.sendMessage(Lang.render("lobby.ready.not_in_lobby"));
            return;
        }
        if (!this.config.enableReady()) {
            player.sendMessage(Lang.render("lobby.ready.disabled"));
            return;
        }
        final boolean nowReady = !this.ready.isReady(uuid);
        this.ready.setReady(uuid, nowReady);
        player.sendMessage(nowReady
                ? Lang.render("lobby.ready.toggle_on")
                : Lang.render("lobby.ready.toggle_off"));
        this.items.refreshReady(player);
    }

    /** present 差集:新入场者发物品,离场者收物品+撤侧边栏(离线静默,物品随存档走、重入场覆盖发放)。 */
    private void syncPresence(final Set<UUID> present) {
        for (final UUID player : present) {
            if (this.lastPresent.contains(player)) {
                continue;
            }
            final Player online = Bukkit.getPlayer(player);
            if (online != null) {
                this.items.give(online);
            }
        }
        for (final UUID player : this.lastPresent) {
            if (present.contains(player)) {
                continue;
            }
            final Player online = Bukkit.getPlayer(player);
            if (online != null) {
                this.items.strip(online);
            }
            this.sidebar.detach(player);
        }
    }
}
