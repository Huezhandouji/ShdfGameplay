package com.shdfGameplay.component;

import com.hydrokarbs.engine.component.SessionComponent;
import com.hydrokarbs.engine.port.SessionServices;
import java.time.Duration;

/**
 * 散场引导宿主会话组件(随 SessionRecipe.extraComponents 贡献):消费局末无续局信号→逐人 leaveSession(KICK)+传送回大厅→AFK 宽限强制离场。
 *
 * <p>来源:04 §4.4;2.5 步骤5(:39/:53 阶段5 随 TDM 落地)。</p>
 */
public final class MatchEndReturn extends SessionComponent {

    /**
     * 声明面:携带 AFK 宽限时长。
     */
    public static final class Spec extends SessionComponent.Specification<MatchEndReturn> {

        private final Duration afkGrace;

        public Spec(Duration afkGrace) {
            super("match_end_return");
            this.afkGrace = afkGrace;
        }

        @Override
        public MatchEndReturn create(String id, SessionServices services) {
            return new MatchEndReturn(id, services, afkGrace);
        }
    }

    /** AFK 宽限时长(5.2 阶段消费:局末未续局时对滞留者强制离场)。 */
    private final Duration afkGrace;

    protected MatchEndReturn(String id, SessionServices services, Duration afkGrace) {
        super(id, services);
        this.afkGrace = afkGrace;
    }

    // TODO(5.2): 消费实例自定义 shdf.matchend 事件(局末无续局信号);2.5 不做——先以 /shdf leave 命令离场简化(2.5:39 尾注) —— 出自 04 §4.4
    // TODO(5.2): 逐人 api.leaveSession(u, LeaveReason.KICK)+当场传送回大厅;框架 leaveSession 不传送,传送由本组件+LobbyReturnListener 承担 —— 出自 04 §4.6
}
