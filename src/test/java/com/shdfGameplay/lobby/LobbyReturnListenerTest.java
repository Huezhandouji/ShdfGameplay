package com.shdfGameplay.lobby;

import com.hydrokarbs.api.SessionEventView;
import com.hydrokarbs.engine.session.SessionId;
import com.shdfGameplay.config.LobbyConfig;
import org.bukkit.Location;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * LobbyReturnListener Dissolved 路由测试(设计文档 §9 用例 16):Dissolved 事件
 * (record 公开构造,SessionId 同)按 sessionId 路由进 GameFlow.onDissolved 触发第二段补传;
 * 其他事件(MemberJoined 等)零路由。路由效果经 RecordingTeleport 第二段补传副作用观测。
 * 出自 本次任务(shdf-launchstop-流程开关)设计文档 §9。
 */
class LobbyReturnListenerTest {

    private static final UUID U1 = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Test
    void accept_Dissolved_路由onDissolved() {
        final List<String> calls = new ArrayList<>();
        final StubHydrokarbsApi stub = new StubHydrokarbsApi(calls);
        final RecordingTeleport teleport = new RecordingTeleport(calls);
        final GameFlow flow = new GameFlow(stub,
                new LobbyConfig(null, null, new Location(null, 0, 0, 0),
                        600L, false, new LobbyConfig.MapVoteSetting(true, 3), 2),
                new LobbyManager(), teleport, Logger.getLogger("LobbyReturnListenerTest"));
        stub.setSessions(List.of(new FakeSessionHandle("x", List.of(U1))));
        flow.launch();
        flow.stop();
        final LobbyReturnListener listener = new LobbyReturnListener(flow);
        assertEquals(1, teleport.teleported().size(), "前置:第一段一笔");

        listener.accept(new SessionEventView.Dissolved(new SessionId("x"), "EMPTY", 0L));
        assertEquals(2, teleport.teleported().size(), "Dissolved 路由命中名单:第二段补传一笔(GameFlow 侧收到 \"x\")");

        listener.accept(new SessionEventView.Dissolved(new SessionId("x"), "EMPTY", 1L));
        assertEquals(2, teleport.teleported().size(), "名单已消费:重复 Dissolved 零路由副作用");

        listener.accept(new SessionEventView.MemberJoined(new SessionId("x"), U1, 2L));
        assertEquals(2, teleport.teleported().size(), "其他事件零路由(仍留 2.5)");
    }
}
