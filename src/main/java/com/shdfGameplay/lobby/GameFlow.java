package com.shdfGameplay.lobby;

import com.hydrokarbs.api.HydrokarbsApi;
import com.shdfGameplay.config.LobbyConfig;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * 游戏流程总开关:launch 启动流程控制并初始化大厅运行态+开局周期(MatchCycle 联动起票节拍);
 * stop 先停开局周期(此后无开局判定抢跑)再停止流程控制、解散全部运行会话并把会话成员传回大厅出生点
 * (lobby.spawn,两段传送:stop 立即第一段,Dissolved 后第二段补传钉死最终落点——收摊传送不可避开,
 * 见设计文档 §2.1/§6.2)。
 * 状态自持(RUNNING 标志翻转在全部动作成功后),与 LobbyManager.open/close 同步;
 * 幂等可重入:重复 launch/stop 无副作用返回 false,stop 后可再次 launch。
 * 契约:全方法仅主线程调用(命令回调与引擎 tick 均主线程),内部不做同步。
 * —— 出自 本次任务(shdf-launchstop-流程开关);周期联动出自 2.5 大厅逻辑(04-大厅模式 §4.2/§4.3);
 * 04-大厅模式 §4.6/§4.8 传送分工。
 */
public final class GameFlow {

    private final HydrokarbsApi hk;
    private final LobbyConfig lobbyConfig;
    private final LobbyManager lobby;
    private final MatchCycle cycle;
    private final LobbyTeleport teleport;
    private final Logger log;

    /** 散场引导名单:stop 记 sessionId→成员快照,Dissolved 到达时消费条目并第二段补传。 */
    private final Map<String, List<UUID>> pendingReturns = new HashMap<>();

    /** 流程控制运行标志(自持,翻转在全部动作成功后;异常窗口保持 RUNNING 允许重试 stop)。 */
    private boolean running;

    /**
     * @param hk 框架 API(onEnable 在场检查后持有)
     * @param lobbyConfig onEnable 已缓存的大厅配置(不读盘)
     * @param lobby 大厅账本(open/close 由本类单调调用)
     * @param cycle 开局周期缝(launch 起票节拍/stop 先停;生产 Bukkit 壳与测试假件各一)
     * @param teleport 大厅回传缝(生产 Bukkit/测试 fake 各一)
     * @param log 插件日志
     */
    public GameFlow(final HydrokarbsApi hk, final LobbyConfig lobbyConfig, final LobbyManager lobby,
                    final MatchCycle cycle, final LobbyTeleport teleport, final Logger log) {
        this.hk = hk;
        this.lobbyConfig = lobbyConfig;
        this.lobby = lobby;
        this.cycle = cycle;
        this.teleport = teleport;
        this.log = log;
    }

    /**
     * 启动游戏流程控制并初始化大厅+开局周期(注入 onEnable 已缓存配置,不读盘不注册模式):
     * 大厅先 open(周期起票广播读 present 账本),再 start 周期。
     *
     * @return true=本次完成启动;false=已在运行(幂等无副作用)
     */
    public boolean launch() {
        if (this.running) {
            return false;
        }
        // 兜底清残留散场名单(正常路径 Dissolved 已消费;极端漏发时防止陈旧名单跨生命周期误传)
        this.pendingReturns.clear();
        this.lobby.open(this.lobbyConfig);
        this.cycle.start();
        this.running = true;
        this.log.info("游戏流程已启动");
        return true;
    }

    /**
     * 停止游戏流程控制:先停开局周期(取消节拍任务+清票本/准备账本/判定核,此后无开局判定抢跑)→
     * 清大厅账本→枚举会话→逐成员 leaveSession(KICK 即时移除)→
     * 第一段传送回 lobby.spawn(离线跳过)→记散场引导名单→翻转状态。
     * 会话实体随后由框架空场解散,收摊传送可能把在线玩家短暂拉往主世界出生点,
     * 最终落点由 onDissolved 第二段补传钉死在 lobby.spawn。
     *
     * @return true=本次完成停止;false=未在运行(幂等无副作用)
     */
    public boolean stop() {
        if (!this.running) {
            return false;
        }
        // 先停周期再关大厅:停周期时大厅仍开放(次序见证见 GameFlowTest),此后 enter 不记账
        this.cycle.stop();
        // 先关大厅:此后 enter 不记账,传送回来的玩家不会误入账本
        this.lobby.close();
        int dissolvedSessions = 0;
        int returnedPlayers = 0;
        for (final var handle : this.hk.sessions()) {
            try {
                // 先快照名册再动手(leave 会清 roster,快照必须先行);info() 空(恰在 ENDING/DISSOLVING)视为空成员跳过
                final List<UUID> members = handle.info().map(info -> info.members()).orElse(List.of());
                // 玩家域单口,固定 KICK 即时移除(仅 roster.remove+MemberLeft);不用 handle.leave(u, QUIT):QUIT 是框架断线语义宿主不自调
                for (final UUID member : members) {
                    this.hk.leaveSession(member);
                }
                // 第一段:立即出副本世界(副本世界随解散清理,滞留者会被 Bukkit 强制移到主世界重生点而非大厅 spawn)
                for (final UUID member : members) {
                    if (this.teleport.toSpawn(member, this.lobbyConfig.spawn())) {
                        returnedPlayers++;
                    }
                }
                this.pendingReturns.put(handle.sessionId(), members);
                dissolvedSessions++;
            } catch (final Exception e) {
                // 逐会话容错:单会话处理失败不阻断其余会话;异常窗口 running 仍 true,可重入 stop 补扫
                this.log.warning("stop 解散会话失败,跳过该会话 " + handle.sessionId() + ": " + e);
            }
        }
        this.running = false;
        this.log.info("游戏流程停止:解散 " + dissolvedSessions + " 个会话,第一段传送 " + returnedPlayers + " 名玩家回大厅");
        return true;
    }

    /** 流程控制是否运行中(= 大厅是否开放)。 */
    public boolean isRunning() {
        return this.running;
    }

    /**
     * 散场第二段补传(包私有,经 LobbyReturnListener 路由 Dissolved 调入):
     * 名单命中该会话→在线成员再传 lobby.spawn(此时收摊传送已全部完成,
     * 本笔是最后一笔、最终落点不被覆盖)→消费名单条目;未命中 no-op。
     */
    void onDissolved(final String sessionId) {
        final List<UUID> members = this.pendingReturns.remove(sessionId);
        if (members == null) {
            return;
        }
        int returnedPlayers = 0;
        for (final UUID member : members) {
            if (this.teleport.toSpawn(member, this.lobbyConfig.spawn())) {
                returnedPlayers++;
            }
        }
        this.log.info("会话 " + sessionId + " 散场收尾:" + returnedPlayers + " 名玩家接回大厅");
    }
}
