package com.shdfGameplay.lobby.ui;

import com.shdfGameplay.config.LobbyConfig;
import com.shdfGameplay.lobby.LobbyManager;
import com.shdfGameplay.lobby.ReadyState;
import com.shdfGameplay.lang.Lang;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

/**
 * 大厅侧边计分板件:当前游戏开启条件面板(样式参照弃用插件 SHDFGamePlugin WaitingPhase
 * updateSidebarObjective 的「条件行绿=已满足/灰=未满足 + 倒计时行」形态)——
 * 共享一块自建 Scoreboard,重建式刷新(先注销旧 objective 再注册,杜绝跨帧残留行),
 * 行内容 Component 构造后经 LegacyComponentSerializer 序列化进 getScore(String)
 * (1.21 计分行入口只有 String 重载,§ 码着色;行文本互异保证去重键)。
 * 挂载按 present 逐人 setScoreboard(attached 账本去重,同板重挂不重发);
 * detach 回主计分板。生命周期随 LobbyView,clear 撤板注销 objective。
 * —— 出自 本次任务(大厅交互层)。
 */
final class LobbySidebar {

    /** objective 注册名(同板重建复用;unregister 后可重注册)。 */
    private static final String OBJECTIVE_NAME = "shdf_lobby_sidebar";

    /** 空白行文本(两行互异保证 getScore 去重键不撞,参照弃用插件空格差分口径)。 */
    private static final Component BLANK_1 = Component.text(" ");
    private static final Component BLANK_2 = Component.text("  ");

    /** Component → § 码行文本(计分行唯一入口 getScore(String) 的适配器,静态无状态复用)。 */
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    private final LobbyManager lobby;
    private final ReadyState ready;
    private final LobbyConfig config;

    /** 共享计分板(null=未建;首次 render 懒建,clear 置空)。 */
    private Scoreboard scoreboard;

    /** 当前 objective(重建式刷新:null=未建/已撤)。 */
    private Objective objective;

    /** 已挂板玩家账本(detach 回主板判定;clear 全清)。 */
    private final Set<UUID> attached = new HashSet<>();

    /**
     * @param lobby  大厅账本(在场人数与全员 ready 判定面)
     * @param ready  准备账本(已准备人数统计)
     * @param config 大厅配置(min-players/enable_ready 门槛行)
     */
    LobbySidebar(final LobbyManager lobby, final ReadyState ready, final LobbyConfig config) {
        this.lobby = lobby;
        this.ready = ready;
        this.config = config;
    }

    /**
     * 重建侧边计分板并给在场全员挂板(新入场者含在内;≤1Hz 或 present/全员 ready 翻转时由调用方触发):
     * 行序=准备阶段→空→当前人数→开启条件(人数门色随满足态,准备门仅 enableReady 时出)→空→倒计时行(仅倒计时中)。
     *
     * @param present            当前在场者(挂板面)
     * @param countdownRemaining 判定核倒计时剩余 tick(0=未在倒计时,不出倒计时行)
     */
    void render(final Set<UUID> present, final long countdownRemaining) {
        if (this.scoreboard == null) {
            this.scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        }
        if (this.objective != null) {
            this.objective.unregister();
        }
        this.objective = this.scoreboard.registerNewObjective(OBJECTIVE_NAME, Criteria.DUMMY, Lang.render("lobby.sidebar.title"));
        this.objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        int row = 0;
        line(Lang.render("lobby.sidebar.phase"), --row);
        line(BLANK_1, --row);
        line(Lang.render("lobby.sidebar.players",
                Placeholder.unparsed("count", String.valueOf(present.size()))), --row);
        line(Lang.render("lobby.sidebar.requirements"), --row);
        final boolean enoughPlayers = present.size() >= this.config.minPlayers();
        line(Lang.render(enoughPlayers ? "lobby.sidebar.gate_players_met" : "lobby.sidebar.gate_players_unmet",
                Placeholder.unparsed("min", String.valueOf(this.config.minPlayers()))), --row);
        if (this.config.enableReady()) {
            final int readyCount = countReady(present);
            final boolean allReady = this.ready.allReady(present);
            line(Lang.render(allReady ? "lobby.sidebar.gate_ready_met" : "lobby.sidebar.gate_ready_unmet",
                    Placeholder.unparsed("ready", String.valueOf(readyCount)),
                    Placeholder.unparsed("total", String.valueOf(present.size()))), --row);
        }
        line(BLANK_2, --row);
        if (countdownRemaining > 0) {
            final long seconds = (countdownRemaining + 19L) / 20L;   // 向上取整:首拍即显示整秒
            line(Lang.render("lobby.sidebar.countdown",
                    Placeholder.unparsed("seconds", String.valueOf(seconds))), --row);
        }
        for (final UUID player : present) {
            attach(player);
        }
    }

    /** 撤单个玩家的板(present 差集离场:attached 命中才回主板)。 */
    void detach(final UUID player) {
        if (!this.attached.remove(player)) {
            return;
        }
        final Player online = Bukkit.getPlayer(player);
        if (online != null) {
            online.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        }
    }

    /** 撤板收摊:全部挂板者回主计分板、注销 objective、弃板(幂等,stop 用)。 */
    void clear() {
        for (final UUID player : Set.copyOf(this.attached)) {
            detach(player);
        }
        if (this.objective != null) {
            this.objective.unregister();
            this.objective = null;
        }
        this.scoreboard = null;
    }

    /** 挂板(attached 去重,同板重挂不重发;离线摘账回线不再挂)。 */
    private void attach(final UUID player) {
        if (this.attached.contains(player)) {
            return;
        }
        final Player online = Bukkit.getPlayer(player);
        if (online == null) {
            return;
        }
        online.setScoreboard(this.scoreboard);
        this.attached.add(player);
    }

    /** 在场者中已准备人数(准备门行 (r/N) 统计)。 */
    private int countReady(final Set<UUID> present) {
        int count = 0;
        for (final UUID player : present) {
            if (this.ready.isReady(player)) {
                count++;
            }
        }
        return count;
    }

    /** 计分行:Component 序列化 § 码入 getScore(String),score 负数递减保证行序自上而下。 */
    private void line(final Component text, final int score) {
        this.objective.getScore(LEGACY.serialize(text)).setScore(score);
    }
}
