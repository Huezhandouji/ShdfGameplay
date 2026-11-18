package com.shdfGameplay.lobby;

import com.hydrokarbs.api.SessionEventView;
import com.shdfGameplay.config.LobbyConfig;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * 散场单闸:全工程唯一 subscribeSessionEvents 订阅点——Dissolved 接引收尾、MemberLeft 单人传送回大厅、ModeSwitchCompleted(to=limbo) 记待散信号。
 * 三分支只按事件子类路由,不做业务之外的动作:MemberLeft 接引 = 传送回 lobby.spawn + enter 恢复大厅账本
 * (「回大厅」完整语义 = 位置回区域 + 账本回大厅;候选行由周期壳差集 1 tick 内补发,本类不推);
 * QUIT 离线宽限语义不传送(宽限满被静默移除者下次进服由 onJoin 自查接回);
 * limbo 仅 WARN 记待散信号(本阶段正常流程不应出现,出现即异常态提示;MatchEndReturn 组件阶段5 回接)。
 * —— 出自 2.5 大厅逻辑(04-大厅模式 §4.6/§4.8);2.5 步骤6。
 */
public final class LobbyReturnListener implements Consumer<SessionEventView> {

    // onDisable close 订阅句柄归 Assembly.disable(幂等);账本与框架名册同步点仅 join 成功/MemberLeft/Dissolved 三处 —— 出自 04 §4.8
    // 框架解散先传送后发 Dissolved——宿主以散场引导名单(GameFlow.pendingReturns)+MemberLeft 兜底,
    // 两段传送(主世界出生点→大厅 spawn)无缝衔接;REUSE_DIRTY 副本随解散销毁宿主零感知 —— 出自 04 §4.6

    private final GameFlow flow;
    private final LobbyManager lobby;
    private final LobbyTeleport teleport;
    private final LobbyConfig config;
    private final Logger log;

    /**
     * @param flow 流程开关(Dissolved 路由目标 + MemberLeft 接引的运行闸:未运行不传送)
     * @param lobby 大厅账本(MemberLeft 传送成功后 enter 恢复大厅态)
     * @param teleport 大厅回传缝(生产 Bukkit/测试 fake 各一)
     * @param config onEnable 已缓存的大厅配置(不读盘)
     * @param log 插件日志(limbo 待散信号 WARN 面)
     */
    public LobbyReturnListener(final GameFlow flow, final LobbyManager lobby,
                               final LobbyTeleport teleport, final LobbyConfig config, final Logger log) {
        this.flow = flow;
        this.lobby = lobby;
        this.teleport = teleport;
        this.config = config;
        this.log = log;
    }

    /** 唯一订阅点回调:只按事件子类路由/记账,不做业务之外的动作;其余事件空壳。 */
    @Override
    public void accept(final SessionEventView event) {
        if (event instanceof SessionEventView.Dissolved dissolved) {
            this.flow.onDissolved(dissolved.session().value());
        } else if (event instanceof SessionEventView.MemberLeft memberLeft) {
            receiveBack(memberLeft);
        } else if (event instanceof SessionEventView.ModeSwitchCompleted completed
                && "limbo".equals(completed.to())) {
            // 待散信号(§4.4 不变式④):无显式去向回落 limbo 属异常态兜底,本阶段正常流程不应出现
            this.log.warning("会话 " + completed.session().value()
                    + " 切至 limbo:记待散信号(本阶段正常流程不应出现,MatchEndReturn 阶段5 回接)");
        }
    }

    /**
     * 单人离场接引回大厅(§4.6「单人中途离场」行:框架 leaveSession 只动名册不传送,接引责任在宿主):
     * reason 以字符串形态回放比对(不解析成枚举,冻结面不被引擎枚举演进绑死)——
     * QUIT 不传送(玩家离线,宽限内重连由框架恢复会话);其余 reason(KICK 等)在流程运行中
     * 传送回 lobby.spawn,成功即 enter 恢复大厅账本(传送失败/未运行零动作)。
     */
    private void receiveBack(final SessionEventView.MemberLeft memberLeft) {
        if ("QUIT".equals(memberLeft.reason())) {
            return;
        }
        if (!this.flow.isRunning()) {
            return;
        }
        if (this.teleport.toSpawn(memberLeft.player(), this.config.spawn())) {
            this.lobby.enter(memberLeft.player());
        }
    }
}
