package com.shdfGameplay.lobby;

import com.shdfGameplay.config.LobbyConfig;
import org.bukkit.Location;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * GameFlow 游戏流程开关测试(设计文档 §9 用例 1-15):状态机幂等可重入、stop 两段传送
 * (先 leaveSession 后第一段 toSpawn、Dissolved 后第二段补传、名单消费/未命中/残留清理)、
 * 多会话独立处理与逐会话容错、成员离线跳过。HydrokarbsApi 用桩基类、传送用记录假件
 * (二者共享调用序日志断言次序),LobbyManager 用真实例,LobbyConfig record 直接构造
 * (paper-api 测试类路径在案,Location 可直接 new)。
 * 服务端「有会话时 stop」全链因 createSession 尚未实装(仅 TODO)无法自然执行,由本组
 * stub sessions() 预置假会话覆盖(设计文档 §11 走查清单如实标注项)。
 * 出自 本次任务(shdf-launchstop-流程开关)设计文档 §9。
 */
class GameFlowTest {

    private static final UUID U1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID U2 = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID U3 = UUID.fromString("00000000-0000-0000-0000-000000000003");

    /** 共享调用序日志(StubHydrokarbsApi 与 RecordingTeleport 同写,次序断言用)。 */
    private final List<String> calls = new ArrayList<>();

    private LobbyConfig newConfig() {
        return new LobbyConfig(null, null, new Location(null, 0, 0, 0), 600L, false,
                new LobbyConfig.MapVoteSetting(true, 3), 2);
    }

    /** 装配被测 GameFlow(真实 LobbyManager + 桩 API + 记录传送,均注入缓存配置)。 */
    private GameFlow newFlow(final LobbyConfig config, final StubHydrokarbsApi stub, final RecordingTeleport teleport) {
        return new GameFlow(stub, config, new LobbyManager(), teleport, Logger.getLogger("GameFlowTest"));
    }

    private static int frequency(final List<String> log, final String entry) {
        return Collections.frequency(log, entry);
    }

    // ── 1/2 launch 状态机 ──

    @Test
    void launch_首次_开大厅置运行() {
        final LobbyConfig config = newConfig();
        final LobbyManager lobby = new LobbyManager();
        final GameFlow flow = new GameFlow(new StubHydrokarbsApi(null), config, lobby,
                new RecordingTeleport(null), Logger.getLogger("GameFlowTest"));

        assertTrue(flow.launch(), "首次 launch 应完成启动");
        assertTrue(flow.isRunning());
        assertTrue(lobby.isOpen(), "launch 应开大厅");
    }

    @Test
    void launch_重复_幂等零副作用() {
        final GameFlow flow = newFlow(newConfig(), new StubHydrokarbsApi(this.calls), new RecordingTeleport(this.calls));

        assertTrue(flow.launch());
        assertFalse(flow.launch(), "重复 launch 应无副作用返回 false");
        assertTrue(flow.isRunning(), "状态不变:仍在运行");
        assertTrue(this.calls.isEmpty(), "零动作(无 leave 无传送)");
    }

    // ── 3-8 stop 全链 ──

    @Test
    void stop_运行中_解散传送关大厅() {
        final LobbyConfig config = newConfig();
        final LobbyManager lobby = new LobbyManager();
        final StubHydrokarbsApi stub = new StubHydrokarbsApi(this.calls);
        final RecordingTeleport teleport = new RecordingTeleport(this.calls);
        final GameFlow flow = new GameFlow(stub, config, lobby, teleport, Logger.getLogger("GameFlowTest"));
        stub.setSessions(List.of(new FakeSessionHandle("s1", List.of(U1, U2))));
        flow.launch();
        lobby.enter(U3); // 大厅账本有残留,stop 应关账

        assertTrue(flow.stop(), "运行中 stop 应完成停止");
        assertFalse(flow.isRunning());
        assertFalse(lobby.isOpen(), "stop 应关大厅");
        assertEquals(1, frequency(this.calls, "leave:" + U1), "每成员恰一次 leaveSession");
        assertEquals(1, frequency(this.calls, "leave:" + U2), "每成员恰一次 leaveSession");
        assertEquals(List.of(U1, U2), teleport.teleported(), "第一段逐成员传送");
        assertEquals(List.of(config.spawn(), config.spawn()), teleport.locations(), "落点==cfg.spawn");
    }

    @Test
    void stop_传送次序_先离开后传送() {
        final StubHydrokarbsApi stub = new StubHydrokarbsApi(this.calls);
        final GameFlow flow = newFlow(newConfig(), stub, new RecordingTeleport(this.calls));
        stub.setSessions(List.of(new FakeSessionHandle("s1", List.of(U1, U2))));
        flow.launch();
        flow.stop();

        for (final UUID member : List.of(U1, U2)) {
            assertTrue(this.calls.indexOf("leave:" + member) < this.calls.indexOf("tp:" + member),
                    "成员 " + member + " 的 leaveSession 应先于其第一段 toSpawn(快照先行)");
        }
    }

    @Test
    void stop_未运行_幂等零动作() {
        final GameFlow flow = newFlow(newConfig(), new StubHydrokarbsApi(this.calls), new RecordingTeleport(this.calls));

        assertFalse(flow.stop(), "未运行 stop 应无副作用返回 false");
        assertTrue(this.calls.isEmpty(), "零解散零传送");
        assertFalse(flow.isRunning());
    }

    @Test
    void stop_多会话_逐会话独立处理() {
        final StubHydrokarbsApi stub = new StubHydrokarbsApi(this.calls);
        final RecordingTeleport teleport = new RecordingTeleport(this.calls);
        final GameFlow flow = newFlow(newConfig(), stub, teleport);
        stub.setSessions(List.of(
                new FakeSessionHandle("s1", List.of(U1, U2)),
                new FakeSessionHandle("s2", List.of(U2, U3))));
        flow.launch();

        assertTrue(flow.stop());
        for (final UUID member : List.of(U1, U2, U3)) {
            assertTrue(this.calls.contains("leave:" + member), "并集成员 " + member + " 均被 leave");
            assertTrue(teleport.teleported().contains(member), "并集成员 " + member + " 均被第一段传送");
        }
        assertEquals(2, frequency(this.calls, "leave:" + U2), "跨会话成员按名册各收一次");
    }

    @Test
    void stop_成员离线_传送跳过不炸() {
        final StubHydrokarbsApi stub = new StubHydrokarbsApi(this.calls);
        final RecordingTeleport teleport = new RecordingTeleport(this.calls);
        teleport.markOffline(U2);
        final GameFlow flow = newFlow(newConfig(), stub, teleport);
        stub.setSessions(List.of(new FakeSessionHandle("s1", List.of(U1, U2))));
        flow.launch();

        assertTrue(flow.stop(), "离线跳过不阻断 stop");
        assertEquals(1, frequency(this.calls, "leave:" + U2), "离线者名册仍被清(leave 照调)");
        assertFalse(teleport.teleported().contains(U2), "离线者无传送面");
        assertEquals(List.of(U1), teleport.teleported(), "在线者照传");
    }

    @Test
    void stop_单会话异常_容错继续() {
        final StubHydrokarbsApi stub = new StubHydrokarbsApi(this.calls);
        final RecordingTeleport teleport = new RecordingTeleport(this.calls);
        final GameFlow flow = newFlow(newConfig(), stub, teleport);
        stub.setSessions(List.of(
                new FakeSessionHandle("s1", List.of(U1), new RuntimeException("info 炸了")),
                new FakeSessionHandle("s2", List.of(U2))));
        flow.launch();

        assertTrue(flow.stop(), "单会话异常不阻断整体停止");
        assertFalse(this.calls.contains("leave:" + U1), "异常会话成员零处理");
        assertTrue(this.calls.contains("leave:" + U2), "其余会话仍被解散");
        assertEquals(List.of(U2), teleport.teleported());
    }

    // ── 9/10 launch 边界与可重入 ──

    @Test
    void launch_不碰会话() {
        final StubHydrokarbsApi stub = new StubHydrokarbsApi(this.calls);
        final GameFlow flow = newFlow(newConfig(), stub, new RecordingTeleport(this.calls));
        stub.setSessions(List.of(new FakeSessionHandle("s1", List.of(U1))));

        assertTrue(flow.launch());
        assertEquals(List.of("s1"), stub.sessions().stream().map(handle -> handle.sessionId()).toList(),
                "既有会话原样在场");
        assertFalse(this.calls.contains("leave:" + U1), "launch 只开控制面,不解散不接管");
    }

    @Test
    void 重入循环_stop_launch_stop_launch() {
        final StubHydrokarbsApi stub = new StubHydrokarbsApi(this.calls);
        final GameFlow flow = newFlow(newConfig(), stub, new RecordingTeleport(this.calls));

        assertTrue(flow.launch());
        assertTrue(flow.stop());
        assertTrue(flow.launch(), "stop 后可再次 launch");
        assertTrue(flow.isRunning());
        assertTrue(flow.stop());
        assertFalse(flow.isRunning());
        assertTrue(this.calls.isEmpty(), "无会话周期零解散零传送");
    }

    // ── 11-15 onDissolved 第二段补传与名单生命周期 ──

    @Test
    void onDissolved_名单命中_第二段补传() {
        final LobbyConfig config = newConfig();
        final StubHydrokarbsApi stub = new StubHydrokarbsApi(this.calls);
        final RecordingTeleport teleport = new RecordingTeleport(this.calls);
        final GameFlow flow = new GameFlow(stub, config, new LobbyManager(), teleport, Logger.getLogger("GameFlowTest"));
        stub.setSessions(List.of(new FakeSessionHandle("s1", List.of(U1, U2))));
        flow.launch();
        flow.stop();
        assertEquals(2, teleport.teleported().size(), "第一段两笔");

        flow.onDissolved("s1");
        assertEquals(List.of(U1, U2, U1, U2), teleport.teleported(), "名单命中:在线成员再各收一笔第二段补传");
        assertEquals(List.of(config.spawn(), config.spawn(), config.spawn(), config.spawn()),
                teleport.locations(), "两段落点均==cfg.spawn");
    }

    @Test
    void onDissolved_消费后名单移除() {
        final StubHydrokarbsApi stub = new StubHydrokarbsApi(this.calls);
        final RecordingTeleport teleport = new RecordingTeleport(this.calls);
        final GameFlow flow = newFlow(newConfig(), stub, teleport);
        stub.setSessions(List.of(new FakeSessionHandle("s1", List.of(U1))));
        flow.launch();
        flow.stop();
        flow.onDissolved("s1");
        final int afterFirst = teleport.teleported().size();

        flow.onDissolved("s1");
        assertEquals(afterFirst, teleport.teleported().size(), "同一 sessionId 二次 Dissolved:条目已消费,零补传");
    }

    @Test
    void onDissolved_未命中_noOp() {
        final StubHydrokarbsApi stub = new StubHydrokarbsApi(this.calls);
        final RecordingTeleport teleport = new RecordingTeleport(this.calls);
        final GameFlow flow = newFlow(newConfig(), stub, teleport);
        stub.setSessions(List.of(new FakeSessionHandle("s1", List.of(U1))));

        flow.onDissolved("stranger");
        assertTrue(teleport.teleported().isEmpty(), "未 stop 过(名单空):零传送");

        flow.launch();
        flow.stop();
        final int firstStage = teleport.teleported().size();
        flow.onDissolved("other-session");
        assertEquals(firstStage, teleport.teleported().size(), "陌生 sessionId:未命中 no-op");
    }

    @Test
    void onDissolved_成员离线_跳过不炸() {
        final StubHydrokarbsApi stub = new StubHydrokarbsApi(this.calls);
        final RecordingTeleport teleport = new RecordingTeleport(this.calls);
        teleport.markOffline(U2);
        final GameFlow flow = newFlow(newConfig(), stub, teleport);
        stub.setSessions(List.of(new FakeSessionHandle("s1", List.of(U1, U2))));
        flow.launch();
        flow.stop();
        assertEquals(List.of(U1), teleport.teleported());

        flow.onDissolved("s1");
        assertEquals(List.of(U1, U1), teleport.teleported(), "离线者第二段同样跳过,在线者补传");
    }

    @Test
    void launch_清残留名单() {
        final StubHydrokarbsApi stub = new StubHydrokarbsApi(this.calls);
        final RecordingTeleport teleport = new RecordingTeleport(this.calls);
        final GameFlow flow = newFlow(newConfig(), stub, teleport);
        stub.setSessions(List.of(new FakeSessionHandle("s1", List.of(U1))));
        flow.launch();
        flow.stop();
        // 不触发 onDissolved 直接进入下一周期(换新会话),旧名单应被 launch 兜底清掉
        stub.setSessions(List.of(new FakeSessionHandle("s2", List.of(U3))));
        flow.launch();
        flow.stop();
        final int beforeStale = teleport.teleported().size();

        flow.onDissolved("s1");
        assertEquals(beforeStale, teleport.teleported().size(), "陈旧名单不跨生命周期(launch 已清)");

        flow.onDissolved("s2");
        assertEquals(beforeStale + 1, teleport.teleported().size(), "本周期名单照常命中");
    }
}
