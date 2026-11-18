package com.shdfGameplay.lobby;

import com.hydrokarbs.api.SessionEventView;
import com.hydrokarbs.engine.session.SessionId;
import com.shdfGameplay.config.LobbyConfig;
import org.bukkit.Location;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * LobbyReturnListener 三分支路由测试(04 §4.6 散场接引表逐行):Dissolved 按 sessionId 路由
 * GameFlow.onDissolved 触发第二段补传(既有用例,断言不动);MemberLeft 单人接引——
 * KICK 在线=传送回大厅 spawn+enter 恢复账本、传送失败不入账、QUIT 离线宽限零传送、
 * 流程未运行零传送;ModeSwitchCompleted(to=limbo) WARN 记待散(经临时 Handler 断言记/不记,
 * 本阶段正常流程不应出现)、非 limbo 去向(含空串无实例)不记。事件 record 公开构造
 * (SessionId 同),路由效果经 RecordingTeleport 副作用与 LobbyManager 账本观测。
 * —— 出自 2.5 大厅逻辑(04-大厅模式 §4.6/§4.8);2.5 步骤6。
 */
class LobbyReturnListenerTest {

    private static final UUID U1 = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private LobbyConfig newConfig() {
        return new LobbyConfig(null, null, new Location(null, 0, 0, 0),
                600L, false, new LobbyConfig.MapVoteSetting(true, 3), 2);
    }

    /** no-op 周期假件(GameFlow 六参签名占位,本组用例不测周期联动)。 */
    private static MatchCycle noopCycle() {
        return new MatchCycle() {
            @Override
            public boolean start() {
                return false;
            }

            @Override
            public void stop() {
            }
        };
    }

    /** WARNING 级日志收集器(limbo 待散信号是日志面效果,经临时挂载断言记/不记)。 */
    private static final class WarningLog extends Handler {

        final List<String> messages = new ArrayList<>();

        @Override
        public void publish(final LogRecord record) {
            if (record.getLevel() == Level.WARNING) {
                this.messages.add(record.getMessage());
            }
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }
    }

    @Test
    void accept_Dissolved_路由onDissolved() {
        final List<String> calls = new ArrayList<>();
        final StubHydrokarbsApi stub = new StubHydrokarbsApi(calls);
        final RecordingTeleport teleport = new RecordingTeleport(calls);
        final LobbyConfig config = newConfig();
        final LobbyManager lobby = new LobbyManager(stub, teleport);
        final GameFlow flow = new GameFlow(stub, config, lobby, noopCycle(), teleport,
                Logger.getLogger("LobbyReturnListenerTest"));
        stub.setSessions(List.of(new FakeSessionHandle("x", List.of(U1))));
        flow.launch();
        flow.stop();
        final LobbyReturnListener listener = new LobbyReturnListener(flow, lobby, teleport, config,
                Logger.getLogger("LobbyReturnListenerTest"));
        assertEquals(1, teleport.teleported().size(), "前置:第一段一笔");

        listener.accept(new SessionEventView.Dissolved(new SessionId("x"), "EMPTY", 0L));
        assertEquals(2, teleport.teleported().size(), "Dissolved 路由命中名单:第二段补传一笔(GameFlow 侧收到 \"x\")");

        listener.accept(new SessionEventView.Dissolved(new SessionId("x"), "EMPTY", 1L));
        assertEquals(2, teleport.teleported().size(), "名单已消费:重复 Dissolved 零路由副作用");

        listener.accept(new SessionEventView.MemberJoined(new SessionId("x"), U1, 2L));
        assertEquals(2, teleport.teleported().size(), "其他事件零路由(仍留 2.5)");
    }

    @Test
    void accept_MemberLeft_KICK在线_传送回大厅并恢复账本() {
        final LobbyConfig config = newConfig();
        final StubHydrokarbsApi stub = new StubHydrokarbsApi(null);
        final RecordingTeleport teleport = new RecordingTeleport(null);
        final LobbyManager lobby = new LobbyManager(stub, teleport);
        final GameFlow flow = new GameFlow(stub, config, lobby, noopCycle(), teleport,
                Logger.getLogger("LobbyReturnListenerTest"));
        flow.launch();
        final LobbyReturnListener listener = new LobbyReturnListener(flow, lobby, teleport, config,
                Logger.getLogger("LobbyReturnListenerTest"));

        listener.accept(new SessionEventView.MemberLeft(new SessionId("s1"), U1, "KICK", 0L));

        assertEquals(List.of(U1), teleport.teleported(), "单人接引恰一笔传送");
        assertEquals(List.of(config.spawn()), teleport.locations(), "落点==cfg.spawn");
        assertTrue(lobby.present().contains(U1), "账本恢复:回大厅即入账(§4.6 回大厅=传送+记账)");
    }

    @Test
    void accept_MemberLeft_KICK离线_传送失败不入账() {
        final LobbyConfig config = newConfig();
        final StubHydrokarbsApi stub = new StubHydrokarbsApi(null);
        final RecordingTeleport teleport = new RecordingTeleport(null);
        teleport.markOffline(U1);
        final LobbyManager lobby = new LobbyManager(stub, teleport);
        final GameFlow flow = new GameFlow(stub, config, lobby, noopCycle(), teleport,
                Logger.getLogger("LobbyReturnListenerTest"));
        flow.launch();
        final LobbyReturnListener listener = new LobbyReturnListener(flow, lobby, teleport, config,
                Logger.getLogger("LobbyReturnListenerTest"));

        listener.accept(new SessionEventView.MemberLeft(new SessionId("s1"), U1, "KICK", 0L));

        assertTrue(teleport.teleported().isEmpty(), "传送失败零记录(toSpawn 返 false 跳过)");
        assertFalse(lobby.present().contains(U1), "传送失败不恢复账本");
    }

    @Test
    void accept_MemberLeft_QUIT_零传送零入账() {
        final LobbyConfig config = newConfig();
        final StubHydrokarbsApi stub = new StubHydrokarbsApi(null);
        final RecordingTeleport teleport = new RecordingTeleport(null);
        final LobbyManager lobby = new LobbyManager(stub, teleport);
        final GameFlow flow = new GameFlow(stub, config, lobby, noopCycle(), teleport,
                Logger.getLogger("LobbyReturnListenerTest"));
        flow.launch();
        final LobbyReturnListener listener = new LobbyReturnListener(flow, lobby, teleport, config,
                Logger.getLogger("LobbyReturnListenerTest"));

        listener.accept(new SessionEventView.MemberLeft(new SessionId("s1"), U1, "QUIT", 0L));

        assertTrue(teleport.teleported().isEmpty(), "QUIT 离线宽限语义零传送(宽限满静默移除者由 onJoin 自查接回)");
        assertFalse(lobby.present().contains(U1), "零入账");
    }

    @Test
    void accept_MemberLeft_流程未运行_零传送() {
        final LobbyConfig config = newConfig();
        final StubHydrokarbsApi stub = new StubHydrokarbsApi(null);
        final RecordingTeleport teleport = new RecordingTeleport(null);
        final LobbyManager lobby = new LobbyManager(stub, teleport);
        final GameFlow flow = new GameFlow(stub, config, lobby, noopCycle(), teleport,
                Logger.getLogger("LobbyReturnListenerTest")); // 未 launch:流程未运行
        final LobbyReturnListener listener = new LobbyReturnListener(flow, lobby, teleport, config,
                Logger.getLogger("LobbyReturnListenerTest"));

        listener.accept(new SessionEventView.MemberLeft(new SessionId("s1"), U1, "KICK", 0L));

        assertTrue(teleport.teleported().isEmpty(), "流程未运行零传送(运行闸)");
        assertFalse(lobby.present().contains(U1), "零入账");
    }

    @Test
    void accept_ModeSwitchCompleted_limbo_记WARN待散() {
        final LobbyConfig config = newConfig();
        final StubHydrokarbsApi stub = new StubHydrokarbsApi(null);
        final RecordingTeleport teleport = new RecordingTeleport(null);
        final LobbyManager lobby = new LobbyManager(stub, teleport);
        final GameFlow flow = new GameFlow(stub, config, lobby, noopCycle(), teleport,
                Logger.getLogger("LobbyReturnListenerTest"));
        flow.launch();
        final Logger log = Logger.getLogger("LobbyReturnListenerTest");
        final WarningLog warnings = new WarningLog();
        log.addHandler(warnings);
        final LobbyReturnListener listener = new LobbyReturnListener(flow, lobby, teleport, config, log);
        try {
            listener.accept(new SessionEventView.ModeSwitchCompleted(new SessionId("s-limbo"),
                    "bosswar", "limbo", true, 0L));
        } finally {
            log.removeHandler(warnings);
        }

        assertEquals(1, warnings.messages.size(), "恰一条 WARN 待散信号");
        assertTrue(warnings.messages.get(0).contains("s-limbo"), "消息带会话上下文");
        assertTrue(teleport.teleported().isEmpty(), "limbo 只记信号不传送(散场引导归 MatchEndReturn 阶段5)");
        assertFalse(lobby.present().contains(U1), "零入账");
    }

    @Test
    void accept_ModeSwitchCompleted_非limbo_不记待散() {
        final LobbyConfig config = newConfig();
        final StubHydrokarbsApi stub = new StubHydrokarbsApi(null);
        final RecordingTeleport teleport = new RecordingTeleport(null);
        final LobbyManager lobby = new LobbyManager(stub, teleport);
        final GameFlow flow = new GameFlow(stub, config, lobby, noopCycle(), teleport,
                Logger.getLogger("LobbyReturnListenerTest"));
        flow.launch();
        final Logger log = Logger.getLogger("LobbyReturnListenerTest");
        final WarningLog warnings = new WarningLog();
        log.addHandler(warnings);
        final LobbyReturnListener listener = new LobbyReturnListener(flow, lobby, teleport, config, log);
        try {
            // 空串=无实例(正常切模);idlepad=宿主自注册空转模式:均非 limbo 兜底
            listener.accept(new SessionEventView.ModeSwitchCompleted(new SessionId("s1"),
                    "bosswar", "idlepad", true, 0L));
            listener.accept(new SessionEventView.ModeSwitchCompleted(new SessionId("s1"),
                    "idlepad", "", true, 1L));
        } finally {
            log.removeHandler(warnings);
        }

        assertTrue(warnings.messages.isEmpty(), "非 limbo 去向零 WARN");
        assertTrue(teleport.teleported().isEmpty(), "零传送");
        assertFalse(lobby.present().contains(U1), "零入账");
    }
}
