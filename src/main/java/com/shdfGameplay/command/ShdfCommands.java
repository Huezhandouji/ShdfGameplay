package com.shdfGameplay.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.shdfGameplay.lobby.GameFlow;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.plugin.Plugin;

/**
 * /shdf 根命令面(Brigadier LifecycleEvents.COMMANDS 代码注册,plugin.yml 不写 commands 段):
 * 本次实装 launch|stop(游戏流程开关;权限 shdf.launch/shdf.stop 挂子命令节点,根不挂 requires 为玩家子命令留开放根);
 * 其余子命令留 TODO——lobby 传送进大厅 / vote <n> / ready / leave(→leaveSession 玩家域单口,KICK 即时移除)/
 * debug session(api.sessions() 句柄 info() 快照)/ debug role info 转调 DebugCommand。
 * —— 出自 本次任务(shdf-launchstop-流程开关)设计文档 §4.4/§7;2.5 步骤10。
 */
public final class ShdfCommands {

    // TODO(2.5): launch/stop 已随本次任务(shdf-launchstop-流程开关)实装;其余子命令不实装——
    //             lobby 传送进大厅 / vote <n> / ready / leave(leave→api.leaveSession(u) ★v1.16 玩家域单口 boolean,
    //             固定 KICK 即时移除;不自传送,传送归 LobbyReturnListener;QUIT 仅框架用宿主不自调)—— 出自 2.5 步骤10、§14.6
    // TODO(3.5): debug session(api.sessions() 句柄 info() 快照)与 debug role info <玩家> 子命令转调 DebugCommand 读口(同包 com.shdfGameplay.command.DebugCommand,角色组件模块交付) —— 出自 2.5 步骤10

    /** 挂载 /shdf 命令树:本次实装 launch|stop;lobby|vote <n>|ready|leave|debug session|debug role info <player> 留 TODO(2.5/3.5)。 */
    public static void register(Plugin plugin, GameFlow flow) {
        plugin.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event ->
                event.registrar().register(root(flow), "SHDF 游戏流程主命令", List.of()));
    }

    /** /shdf 根节点(无 requires):launch/stop 子命令各自挂 shdf.launch/shdf.stop 权限,回话按转换与否二分。 */
    private static LiteralCommandNode<CommandSourceStack> root(GameFlow flow) {
        return Commands.literal("shdf")
                .then(Commands.literal("launch")
                        .requires(source -> source.getSender().hasPermission("shdf.launch"))
                        .executes(context -> {
                            if (flow.launch()) {
                                context.getSource().getSender().sendMessage(
                                        Component.text("游戏流程已启动,大厅运行中", NamedTextColor.GREEN));
                            } else {
                                context.getSource().getSender().sendMessage(
                                        Component.text("游戏流程已在运行中,无需重复启动", NamedTextColor.YELLOW));
                            }
                            return Command.SINGLE_SUCCESS;
                        }))
                .then(Commands.literal("stop")
                        .requires(source -> source.getSender().hasPermission("shdf.stop"))
                        .executes(context -> {
                            if (flow.stop()) {
                                context.getSource().getSender().sendMessage(
                                        Component.text("游戏流程已停止,会话已解散,玩家已送回大厅", NamedTextColor.GREEN));
                            } else {
                                context.getSource().getSender().sendMessage(
                                        Component.text("游戏流程未在运行", NamedTextColor.YELLOW));
                            }
                            return Command.SINGLE_SUCCESS;
                        }))
                .build();
    }
}
