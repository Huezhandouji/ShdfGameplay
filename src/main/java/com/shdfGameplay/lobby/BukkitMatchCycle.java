package com.shdfGameplay.lobby;

import com.shdfGameplay.config.GameConfigManager;
import com.shdfGameplay.config.LobbyConfig;
import com.shdfGameplay.lang.Lang;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.random.RandomGenerator;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

/**
 * 大厅开局周期生产壳(BukkitScheduler runTaskTimer 每拍 1 tick,主线程契约):拍序=①present 快照+
 * ready.retainPresent 对齐账本→②新入场者候选行差集补发(本轮已起票才发;覆盖步行 enter/命令传送/
 * onJoin 接回/MemberLeft 回厅全路径,F2 候选行推送唯一口=每次起票全员广播+差集补发,/shdf lobby
 * 不自发防双发)→③判定核 tick 转发(WAIT 静默/COUNTDOWN 首拍广播一次倒计时/ABORT 广播人数不足/
 * LAUNCH 调开局单闸:成功广播开局+rejections 摘要+清票重起票服务下一轮(ready 保留给留厅者);
 * 失败(F1 节流)广播 rejections 恰一组+门表自愈重收票(单闸拒收路径已摘失败候选,本壳重发新板或重起票,
 * 04 §4.3 门表「不过怎么办」列落地)+ready.clearAll+判定核冷却,防每拍重试刷屏;异常 SEVERE+stop
 * 防每拍重炸不自旋)→④交互层 {@link LobbyView} tick 差集驱动(物品发放/票本变更刷新开着的投票菜单/
 * 侧边计分板重建,单一刷新点覆盖菜单点击与 /shdf vote 命令全部票本变更路径)。判定与节流住
 * {@link MatchCycleJudge} 离线可测,本壳只做 Bukkit 面(调度/广播/差集),行为覆盖走起服实测。
 * —— 出自 2.5 大厅逻辑(04-大厅模式 §4.2/§4.3;2.5 步骤4)。
 */
public final class BukkitMatchCycle implements MatchCycle {

    /** 周期节拍(tick):每拍 1 次,首拍延迟 1 tick(起票广播先于第一拍)。 */
    private static final long PERIOD_TICKS = 1L;

    private final Plugin plugin;
    private final GameConfigManager games;
    private final LobbyConfig config;
    private final LobbyManager lobby;
    private final LobbyVote vote;
    private final ReadyState ready;
    private final MatchBootstrap bootstrap;
    private final MatchCycleJudge judge;

    /** 交互层缝(物品/投票菜单/侧边计分板;随周期起停、每拍差集驱动)。 */
    private final LobbyView view;

    /** 节拍任务(null=未运行;start/stop 幂等依据)。 */
    private BukkitTask task;

    /** 上一拍 present 快照(差集补发新入场者用;start/stop 重置为空)。 */
    private Set<UUID> lastPresent = Set.of();

    /**
     * @param plugin    宿主插件(调度归属方:runTaskTimer 注册与日志面)
     * @param games     图配置管理器(起票池诊断:池空警告,提示检查标记文件)
     * @param config    大厅配置(起票口径 mapVoteSetting 与倒计时广播时长)
     * @param lobby     大厅账本(每拍 present 快照与 LAUNCH 参与者采集)
     * @param vote      投票状态机(起票/票本/候选行渲染)
     * @param ready     准备账本(每拍对齐与失败节流清账)
     * @param bootstrap 开局单闸(LAUNCH 拍调用)
     * @param view      交互层缝(生产 lobby/ui 各件与测试假件各一;start/stop 联动、每拍 tick 驱动)
     * @param random    预留随机源(壳侧抽样与平局裁决已随构造注入 LobbyVote,本件无自持消费点,参数对齐装配契约)
     */
    public BukkitMatchCycle(final Plugin plugin, final GameConfigManager games, final LobbyConfig config,
                            final LobbyManager lobby, final LobbyVote vote, final ReadyState ready,
                            final MatchBootstrap bootstrap, final LobbyView view, final RandomGenerator random) {
        this.plugin = plugin;
        this.games = games;
        this.config = config;
        this.lobby = lobby;
        this.vote = vote;
        this.ready = ready;
        this.bootstrap = bootstrap;
        this.view = view;
        this.judge = new MatchCycleJudge(config.minPlayers(), config.enableReady(), config.countdown());
    }

    /**
     * 启动周期:交互层注册物品定义并复位差集基准→起票并向当前 present 全员广播候选行
     * (launch 时账本恰清空,广播面为空集,入场者由每拍差集补发覆盖)→注册节拍任务。
     *
     * @return true=本次完成启动;false=已在运行(幂等无副作用)
     */
    @Override
    public boolean start() {
        if (this.task != null) {
            return false;
        }
        this.view.start();
        beginVoteRound();
        this.lastPresent = this.lobby.present();
        this.task = Bukkit.getScheduler().runTaskTimer(this.plugin, this::onTick, PERIOD_TICKS, PERIOD_TICKS);
        return true;
    }

    /** 停止周期:取消节拍任务+交互层收摊(关菜单/收物品/撤侧边栏)+清票本+清准备账本+判定核回 WAIT(全幂等;流程 stop 与 disable 共用)。 */
    @Override
    public void stop() {
        if (this.task != null) {
            this.task.cancel();
            this.task = null;
        }
        this.view.stop();
        this.vote.clear();
        this.ready.clearAll();
        this.judge.reset();
        this.lastPresent = Set.of();
    }

    /** 节拍入口(包私有,主线程):异常防护住此层——单拍异常 SEVERE+stop,防每拍重炸不自旋。 */
    void onTick() {
        try {
            tickOnce();
        } catch (final Exception e) {
            this.plugin.getLogger().log(Level.SEVERE, Lang.plain("log.lobby.cycle_tick_crash"), e);
            stop();
        }
    }

    /** 单拍序:账本对齐→差集补发→判定转发→交互层差集驱动(物品发放/菜单刷新/侧边栏重建,见类注释拍序)。 */
    private void tickOnce() {
        final Set<UUID> present = this.lobby.present();
        this.ready.retainPresent(present);
        sendCandidateLinesToNewcomers(present);
        this.lastPresent = present;
        switch (this.judge.tick(present.size(), this.ready.allReady(present))) {
            case WAIT -> {
                // 静默(冷却期/门未过)
            }
            case COUNTDOWN -> {
                if (this.judge.remaining() == this.config.countdown()) {
                    broadcast(Lang.render("lobby.cycle.countdown_started",
                            Placeholder.unparsed("seconds", String.valueOf(this.config.countdown() / 20L))));
                }
            }
            case ABORT -> broadcast(Lang.render("lobby.cycle.countdown_aborted"));
            case LAUNCH -> launch(present);
        }
        // 交互层置于判定转发后:LAUNCH 拍移账者当拍即被差集收件收板,不留跨拍残留
        this.view.tick(this.judge.remaining());
    }

    /** LAUNCH 拍:开局单闸调用;成功重起票服务下一轮(ready 保留),失败 F1 节流恰一组广播+门表自愈重收票。 */
    private void launch(final Set<UUID> participants) {
        final List<String> candidatesBefore = this.vote.candidates();
        final MatchBootstrap.BootstrapResult result = this.bootstrap.bootstrap(participants);
        if (result.sessionId() != null) {
            broadcast(Lang.render("lobby.cycle.launched"));
            if (!result.rejections().isEmpty()) {
                broadcast(Lang.render("lobby.cycle.launch_rejections",
                        Placeholder.unparsed("rejections", String.join(";", result.rejections()))));
            }
            // 大厅服务下一轮:清票重起票并广播新候选行(join 成功者已移出账本,广播面=留厅者);
            // 准备标记保留给留厅者(多会话并行口径),成功路径重走完整倒计时天然节流
            this.vote.clear();
            beginVoteRound();
            return;
        }
        // 开局失败(F1 节流):rejections 恰一组广播+门表自愈重收票(见 recollectVotes)+清准备账本
        // (玩家重新 /shdf ready 触发)+判定核冷却
        for (final String rejection : result.rejections()) {
            broadcast(Lang.render("lobby.cycle.launch_failed", Placeholder.unparsed("reason", rejection)));
        }
        recollectVotes(candidatesBefore, participants);
        this.ready.clearAll();
        this.judge.notifyLaunchRejected();
    }

    /**
     * 门表自愈重收票(04 §4.3 门表「不过怎么办」列落地):单闸已在拒收路径摘除失败候选(地图门摘候选/
     * 角色门摘玩法候选,票本随摘即清)——候选集有变且非空即向在场者重发新板供重收(候选序已重排,旧票失效);
     * 摘尽则重起票换新一轮样本(起票广播归 beginVoteRound);候选集无变化(人数/票选/模式/配置解析失败)
     * 不重发不重起,冷却节流防刷屏。
     */
    private void recollectVotes(final List<String> candidatesBefore, final Set<UUID> participants) {
        if (this.vote.candidates().equals(candidatesBefore)) {
            return;
        }
        if (this.vote.candidates().isEmpty()) {
            beginVoteRound();
            return;
        }
        for (final UUID player : participants) {
            pushVoteBoard(player);
        }
    }

    /** 起票:池空警告(人读运维诊断)+抽样定格+向当前 present 全员推送投票板(候选行渲染归 LobbyVote)。 */
    private void beginVoteRound() {
        if (this.games.all().isEmpty()) {
            this.plugin.getLogger().warning(Lang.plain("log.lobby.candidate_pool_empty"));
        }
        this.vote.beginRound(this.config.mapVoteSetting().enabled(), this.config.mapVoteSetting().mapAmount());
        for (final UUID player : this.lobby.present()) {
            pushVoteBoard(player);
        }
    }

    /** 新入场者差集补发(上一拍快照不含者):覆盖步行进厅/命令传送/onJoin 接回/MemberLeft 回厅全路径。 */
    private void sendCandidateLinesToNewcomers(final Set<UUID> present) {
        if (this.vote.candidates().isEmpty()) {
            return;   // 本轮未起票/无候选不补发
        }
        for (final UUID player : present) {
            if (!this.lastPresent.contains(player)) {
                pushVoteBoard(player);
            }
        }
    }

    /** 推送投票板(标题行+候选行)给单个玩家;离线静默跳过(回线后由 onJoin 接回+差集补发覆盖)。 */
    private void pushVoteBoard(final UUID player) {
        final Player online = Bukkit.getPlayer(player);
        if (online == null) {
            return;
        }
        online.sendMessage(Lang.render("lobby.cycle.vote_board_title"));
        for (final Component line : this.vote.candidateLines()) {
            online.sendMessage(line);
        }
    }

    /** 向当前大厅全员广播单条文本(离线者跳过)。 */
    private void broadcast(final Component message) {
        for (final UUID player : this.lobby.present()) {
            final Player online = Bukkit.getPlayer(player);
            if (online != null) {
                online.sendMessage(message);
            }
        }
    }
}
