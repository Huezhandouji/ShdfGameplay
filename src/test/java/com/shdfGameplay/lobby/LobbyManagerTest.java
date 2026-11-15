package com.shdfGameplay.lobby;

import com.shdfGameplay.config.LobbyConfig;
import org.bukkit.Location;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * LobbyManager 运行态闸测试(设计文档 §9 用例 11-13):未 open 恒空不入账、
 * open 后记账与清账、close 回未 open 态。LobbyConfig record 直接构造(paper-api
 * 测试类路径在案,Location 可直接 new)。
 * 出自 本次任务(shdf-launchstop-流程开关)设计文档 §9。
 */
class LobbyManagerTest {

    private static final UUID U1 = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private static LobbyConfig newConfig() {
        return new LobbyConfig(null, null, new Location(null, 0, 0, 0), 600L, false,
                new LobbyConfig.MapVoteSetting(true, 3), 2);
    }

    @Test
    void 未open_enter不入账present恒空() {
        final LobbyManager lobby = new LobbyManager();

        assertFalse(lobby.isOpen(), "构造后即未 open:插件加载完成不进大厅运行态");
        lobby.enter(U1);
        assertTrue(lobby.present().isEmpty(), "未 open:enter 无操作");
        lobby.leave(U1);
        assertTrue(lobby.present().isEmpty(), "未 open:leave 无操作");
    }

    @Test
    void open后_enter入账present含之_leave清之() {
        final LobbyManager lobby = new LobbyManager();
        lobby.open(newConfig());

        assertTrue(lobby.isOpen());
        lobby.enter(U1);
        assertTrue(lobby.present().contains(U1), "open 后 enter 入账");
        lobby.leave(U1);
        assertFalse(lobby.present().contains(U1), "leave 清账");
    }

    @Test
    void close_账本清空回到未open态() {
        final LobbyManager lobby = new LobbyManager();
        lobby.open(newConfig());
        lobby.enter(U1);

        lobby.close();
        assertFalse(lobby.isOpen(), "close 弃配置回到未 open");
        assertTrue(lobby.present().isEmpty(), "close 清账本");
        lobby.enter(U1);
        assertTrue(lobby.present().isEmpty(), "close 后 enter 无操作");
    }
}
