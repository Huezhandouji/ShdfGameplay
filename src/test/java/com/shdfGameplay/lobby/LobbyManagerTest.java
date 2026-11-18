package com.shdfGameplay.lobby;

import com.shdfGameplay.config.LobbyConfig;
import com.shdfGameplay.map.regionNotation.CubeRegion;
import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * LobbyManager 运行态闸与三事件口径测试(设计文档 §9 用例 11-13 + 04 §4.2):未 open 恒空不入账、
 * open 后记账与清账、close 回未 open 态;onMove 区域进出场(区域内入账/区域外清账/回区再入账)、
 * onJoin 自查接回(不在账本且不在会话→传回出生点并记账;在会话/已记账/传送失败不接回)。
 * LobbyConfig record 直接构造(paper-api 测试类路径在案,Location 可直接 new;bounds 世界与
 * 落点世界同以 null 构造,LobbyManager 以 Objects.equals 对齐)。会话在场经 StubHydrokarbsApi
 * 预置 sessionOf,传送经 RecordingTeleport 记笔。跨方块节流住监听器(hasChangedBlock),不在此测。
 * 出自 本次任务(shdf-launchstop-流程开关)设计文档 §9;04-大厅模式 §4.2。
 */
class LobbyManagerTest {

    private static final UUID U1 = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private static LobbyConfig newConfig() {
        return new LobbyConfig(null, CubeRegion.ofCorners(new Vector(0, 0, 0), new Vector(100, 100, 100)),
                new Location(null, 0, 0, 0), 600L, false, new LobbyConfig.MapVoteSetting(true, 3), 2);
    }

    @Test
    void 未open_enter不入账present恒空() {
        final LobbyManager lobby = new LobbyManager(new StubHydrokarbsApi(null), new RecordingTeleport(null));

        assertFalse(lobby.isOpen(), "构造后即未 open:插件加载完成不进大厅运行态");
        lobby.enter(U1);
        assertTrue(lobby.present().isEmpty(), "未 open:enter 无操作");
        lobby.leave(U1);
        assertTrue(lobby.present().isEmpty(), "未 open:leave 无操作");
    }

    @Test
    void open后_enter入账present含之_leave清之() {
        final LobbyManager lobby = new LobbyManager(new StubHydrokarbsApi(null), new RecordingTeleport(null));
        lobby.open(newConfig());

        assertTrue(lobby.isOpen());
        lobby.enter(U1);
        assertTrue(lobby.present().contains(U1), "open 后 enter 入账");
        lobby.leave(U1);
        assertFalse(lobby.present().contains(U1), "leave 清账");
    }

    @Test
    void close_账本清空回到未open态() {
        final LobbyManager lobby = new LobbyManager(new StubHydrokarbsApi(null), new RecordingTeleport(null));
        lobby.open(newConfig());
        lobby.enter(U1);

        lobby.close();
        assertFalse(lobby.isOpen(), "close 弃配置回到未 open");
        assertTrue(lobby.present().isEmpty(), "close 清账本");
        lobby.enter(U1);
        assertTrue(lobby.present().isEmpty(), "close 后 enter 无操作");
    }

    // ── onMove 进出场判定(判定住本类,节流住监听器) ──

    @Test
    void 未open_onMove不记账_onJoin不接回() {
        final RecordingTeleport teleport = new RecordingTeleport(null);
        final LobbyManager lobby = new LobbyManager(new StubHydrokarbsApi(null), teleport);

        lobby.onMove(U1, new Location(null, 50, 64, 50));
        assertTrue(lobby.present().isEmpty(), "未 open:onMove 无操作");
        lobby.onJoin(U1);
        assertTrue(teleport.teleported().isEmpty(), "未 open:onJoin 不接回");
    }

    @Test
    void open后_onMove区域内入账_区域外清账() {
        final LobbyManager lobby = new LobbyManager(new StubHydrokarbsApi(null), new RecordingTeleport(null));
        lobby.open(newConfig());

        lobby.onMove(U1, new Location(null, 50, 64, 50));
        assertTrue(lobby.present().contains(U1), "区域内位移入账");
        lobby.onMove(U1, new Location(null, 200, 64, 50));
        assertFalse(lobby.present().contains(U1), "区域外位移清账");
        lobby.onMove(U1, new Location(null, 50, 64, 50));
        assertTrue(lobby.present().contains(U1), "回到区域内再入账");
    }

    // ── onJoin 自查接回 ──

    @Test
    void open后_onJoin不在账本不在会话_传回出生点并记账() {
        final LobbyConfig config = newConfig();
        final RecordingTeleport teleport = new RecordingTeleport(null);
        final LobbyManager lobby = new LobbyManager(new StubHydrokarbsApi(null), teleport);
        lobby.open(config);

        lobby.onJoin(U1);
        assertEquals(List.of(U1), teleport.teleported(), "不在大厅且不在任何会话:传回大厅");
        assertEquals(List.of(config.spawn()), teleport.locations(), "落点==cfg.spawn");
        assertTrue(lobby.present().contains(U1), "接回即记账");
    }

    @Test
    void open后_onJoin在任一会话_不接回不入账() {
        final StubHydrokarbsApi stub = new StubHydrokarbsApi(null);
        stub.putSessionOf(U1, new FakeSessionHandle("s1", List.of(U1)));
        final RecordingTeleport teleport = new RecordingTeleport(null);
        final LobbyManager lobby = new LobbyManager(stub, teleport);
        lobby.open(newConfig());

        lobby.onJoin(U1);
        assertTrue(teleport.teleported().isEmpty(), "在任一会话(对局重连):不接回");
        assertFalse(lobby.present().contains(U1), "会话成员不入大厅账");
    }

    @Test
    void open后_onJoin已在账本_不重复接回() {
        final RecordingTeleport teleport = new RecordingTeleport(null);
        final LobbyManager lobby = new LobbyManager(new StubHydrokarbsApi(null), teleport);
        lobby.open(newConfig());
        lobby.enter(U1);

        lobby.onJoin(U1);
        assertTrue(teleport.teleported().isEmpty(), "已在账本:不接回");
    }

    @Test
    void open后_onJoin传送失败_不入账() {
        final RecordingTeleport teleport = new RecordingTeleport(null);
        teleport.markOffline(U1);
        final LobbyManager lobby = new LobbyManager(new StubHydrokarbsApi(null), teleport);
        lobby.open(newConfig());

        lobby.onJoin(U1);
        assertFalse(lobby.present().contains(U1), "传送失败不记账");
    }
}
