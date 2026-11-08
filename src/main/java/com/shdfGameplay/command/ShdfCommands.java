package com.shdfGameplay.command;

import com.hydrokarbs.api.MiniGameApi;
import com.shdfGameplay.lobby.LobbyManager;
import com.shdfGameplay.lobby.LobbyVote;
import com.shdfGameplay.lobby.ReadyState;
import org.bukkit.plugin.Plugin;

/**
 * /shdf 根命令面(Brigadier):lobby 传送进大厅 / vote <n> / ready / leave(→leaveSession KICK)/ debug session(api.sessions() 快照)/ debug role info 转调 DebugCommand。
 *
 * <p>来源:2.5 步骤10 命令面;04 §4.2;§14.6。</p>
 */
public final class ShdfCommands {

    // TODO(2.5): Brigadier LifecycleEvents.COMMANDS 代码注册,plugin.yml 不写 commands 段;leave→api.leaveSession(u, KICK) 不自传送(传送归 LobbyReturnListener);QUIT 仅框架用宿主不自调 —— 出自 2.5 步骤10、§14.6
    // TODO(3.5): debug role info <玩家> 子命令转调 DebugCommand 读口(同包 com.shdfGameplay.command.DebugCommand,角色组件模块交付) —— 出自 2.5 步骤10

    /** 挂载 /shdf 命令树:lobby|vote <n>|ready|leave|debug session|debug role info <player>。 */
    public static void register(Plugin plugin, MiniGameApi api, LobbyManager lobby, LobbyVote vote, ReadyState ready) {
    }
}
