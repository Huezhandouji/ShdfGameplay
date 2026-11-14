package com.shdfGameplay.command;

import com.hydrokarbs.api.HydrokarbsApi;
import com.shdfGameplay.lobby.LobbyManager;
import com.shdfGameplay.lobby.LobbyVote;
import com.shdfGameplay.lobby.ReadyState;
import org.bukkit.plugin.Plugin;

/**
 * /shdf 根命令面(Brigadier):lobby 传送进大厅 / vote <n> / ready / leave(→leaveSession 玩家域单口,KICK 即时移除)/ debug session(api.sessions() 句柄 info() 快照)/ debug role info 转调 DebugCommand。
 */
public final class ShdfCommands {

    // TODO(2.5): Brigadier LifecycleEvents.COMMANDS 代码注册,plugin.yml 不写 commands 段;leave→api.leaveSession(u)(★v1.16 玩家域单口 boolean,固定 KICK 即时移除)不自传送(传送归 LobbyReturnListener);QUIT 仅框架用宿主不自调 —— 出自 2.5 步骤10、§14.6
    // TODO(3.5): debug role info <玩家> 子命令转调 DebugCommand 读口(同包 com.shdfGameplay.command.DebugCommand,角色组件模块交付) —— 出自 2.5 步骤10

    /** 挂载 /shdf 命令树:lobby|vote <n>|ready|leave|debug session|debug role info <player>。 */
    public static void register(Plugin plugin, HydrokarbsApi hk, LobbyManager lobby, LobbyVote vote, ReadyState ready) {
    }
}
