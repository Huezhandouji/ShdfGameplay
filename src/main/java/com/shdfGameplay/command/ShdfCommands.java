package com.shdfGameplay.command;

import com.hydrokarbs.api.HydrokarbsApi;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.shdfGameplay.config.LobbyConfig;
import com.shdfGameplay.lang.Lang;
import com.shdfGameplay.lobby.GameFlow;
import com.shdfGameplay.lobby.LobbyManager;
import com.shdfGameplay.lobby.LobbyTeleport;
import com.shdfGameplay.lobby.LobbyVote;
import com.shdfGameplay.lobby.ReadyState;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import java.util.List;
import java.util.UUID;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * /shdf 根命令面(Brigadier LifecycleEvents.COMMANDS 代码注册,plugin.yml 不写 commands 段):
 * launch|stop(游戏流程开关;权限 shdf.launch/shdf.stop 挂子命令节点,根不挂 requires 为玩家子命令留开放根)
 * + lobby|vote &lt;n&gt;|ready|leave 四玩家子命令(不挂权限;全部 Adventure Component,回执不含
 * 玩家可控字符串,零注入面)。leave 走 hk.leaveSession(u) ★v1.16 玩家域单口(固定 KICK 即时移除,
 * 不自传送——回大厅传送归 LobbyReturnListener MemberLeft 分支);lobby 在任一会话者拒入(与
 * LobbyManager.onJoin 自查同口径,守账本-名册同步点纪律),传送成功即 enter 记账
 * (候选行不自发,周期壳差集 1 tick 内补发,防双发);ready 触发的提前收票住周期判定核
 * (enableReady 语义),命令面只翻账本。
 * —— 出自 2.5 大厅逻辑(04-大厅模式 §4.2/§4.6;2.5 步骤10);launch/stop 出自 本次任务(shdf-launchstop-流程开关)。
 */
public final class ShdfCommands {

    // TODO(3.5): debug session(api.sessions() 句柄 info() 快照)与 debug role info <玩家> 子命令转调 DebugCommand 读口(同包 com.shdfGameplay.command.DebugCommand,角色组件模块交付) —— 出自 2.5 步骤10

    /**
     * 挂载 /shdf 命令树:launch|stop|lobby|vote &lt;n&gt;|ready|leave 六子命令。
     *
     * @param plugin   宿主插件(命令生命周期归属方)
     * @param flow     游戏流程开关(launch/stop 与 lobby 的运行闸)
     * @param lobby    大厅账本(lobby/vote/ready 的在场校验与记账)
     * @param vote     投票状态机(vote 的记票与候选面)
     * @param ready    准备账本(ready 的翻账)
     * @param hk       框架 API(leave 的玩家域单口)
     * @param config   大厅配置(spawn 传送落点与 enableReady 门)
     * @param teleport 大厅回传缝(lobby 子命令传送)
     */
    public static void register(final Plugin plugin, final GameFlow flow, final LobbyManager lobby,
                                final LobbyVote vote, final ReadyState ready, final HydrokarbsApi hk,
                                final LobbyConfig config, final LobbyTeleport teleport) {
        plugin.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event ->
                event.registrar().register(root(flow, lobby, vote, ready, hk, config, teleport),
                        Lang.plain("command.root.description"), List.of()));
    }

    /** /shdf 根节点(无 requires):launch/stop 各挂权限,玩家四子命令不挂权限保持开放。 */
    private static LiteralCommandNode<CommandSourceStack> root(final GameFlow flow, final LobbyManager lobby,
                                                               final LobbyVote vote, final ReadyState ready,
                                                               final HydrokarbsApi hk, final LobbyConfig config,
                                                               final LobbyTeleport teleport) {
        return Commands.literal("shdf")
                .then(Commands.literal("launch")
                        .requires(source -> source.getSender().hasPermission("shdf.launch"))
                        .executes(context -> {
                            if (flow.launch()) {
                                context.getSource().getSender().sendMessage(
                                        Lang.render("command.launch.started"));
                            } else {
                                context.getSource().getSender().sendMessage(
                                        Lang.render("command.launch.already_running"));
                            }
                            return Command.SINGLE_SUCCESS;
                        }))
                .then(Commands.literal("stop")
                        .requires(source -> source.getSender().hasPermission("shdf.stop"))
                        .executes(context -> {
                            if (flow.stop()) {
                                context.getSource().getSender().sendMessage(
                                        Lang.render("command.stop.stopped"));
                            } else {
                                context.getSource().getSender().sendMessage(
                                        Lang.render("command.stop.not_running"));
                            }
                            return Command.SINGLE_SUCCESS;
                        }))
                .then(lobbyNode(flow, lobby, hk, config, teleport))
                .then(voteNode(lobby, vote))
                .then(readyNode(lobby, ready, config))
                .then(leaveNode(hk))
                .build();
    }

    /**
     * /shdf lobby:传送进大厅(流程运行中才可;在任一会话的名册成员拒传送——不经 leaveSession 硬拉出对局,
     * 且守住「账本与框架名册同步点只有 join 成功/MemberLeft/Dissolved 三处」纪律,与 LobbyManager.onJoin
     * 自查同口径;传送成功即 enter 记账,候选行由周期壳差集补发不自发)。
     */
    private static LiteralCommandNode<CommandSourceStack> lobbyNode(final GameFlow flow, final LobbyManager lobby,
                                                                    final HydrokarbsApi hk, final LobbyConfig config,
                                                                    final LobbyTeleport teleport) {
        return Commands.literal("lobby")
                .executes(context -> {
                    final Player player = playerOf(context.getSource());
                    if (player == null) {
                        return Command.SINGLE_SUCCESS;
                    }
                    if (!flow.isRunning()) {
                        player.sendMessage(Lang.render("command.lobby.not_running"));
                        return Command.SINGLE_SUCCESS;
                    }
                    if (hk.sessionOf(player.getUniqueId()).isPresent()) {
                        player.sendMessage(Lang.render("command.lobby.in_session"));
                        return Command.SINGLE_SUCCESS;
                    }
                    if (teleport.toSpawn(player.getUniqueId(), config.spawn())) {
                        lobby.enter(player.getUniqueId());
                        player.sendMessage(Lang.render("command.lobby.entered"));
                    } else {
                        player.sendMessage(Lang.render("command.lobby.teleport_failed"));
                    }
                    return Command.SINGLE_SUCCESS;
                })
                .build();
    }

    /** /shdf vote <n>:大厅在场才可投;序号 1 起(IntegerArgumentType 下界),越界红字;记票可改票(绿字回执同口径)。 */
    private static LiteralCommandNode<CommandSourceStack> voteNode(final LobbyManager lobby, final LobbyVote vote) {
        // 参数名 i18n:树构建时 Lang.plain 取值一次存局部常量,argument 注册与 getInteger 取参两处复用(同键同值)
        final String argName = Lang.plain("command.vote.arg");
        return Commands.literal("vote")
                .then(Commands.argument(argName, IntegerArgumentType.integer(1))
                        .executes(context -> {
                            final Player player = playerOf(context.getSource());
                            if (player == null) {
                                return Command.SINGLE_SUCCESS;
                            }
                            final UUID uuid = player.getUniqueId();
                            if (!lobby.present().contains(uuid)) {
                                player.sendMessage(Lang.render("lobby.vote.not_in_lobby"));
                                return Command.SINGLE_SUCCESS;
                            }
                            final int choice = IntegerArgumentType.getInteger(context, argName);
                            final List<String> candidates = vote.candidates();
                            if (choice > candidates.size()) {
                                player.sendMessage(Lang.render("lobby.vote.out_of_range",
                                        Placeholder.unparsed("count", String.valueOf(candidates.size()))));
                                return Command.SINGLE_SUCCESS;
                            }
                            vote.castVote(uuid, choice);
                            player.sendMessage(Lang.render("lobby.vote.cast",
                                    Placeholder.unparsed("choice", String.valueOf(choice)),
                                    Placeholder.unparsed("candidate", candidates.get(choice - 1))));
                            return Command.SINGLE_SUCCESS;
                        }))
                .build();
    }

    /** /shdf ready:大厅在场才可;未启用准备黄字;翻准备账本(全员 ready 的提前开局住周期判定核)。 */
    private static LiteralCommandNode<CommandSourceStack> readyNode(final LobbyManager lobby, final ReadyState ready,
                                                                    final LobbyConfig config) {
        return Commands.literal("ready")
                .executes(context -> {
                    final Player player = playerOf(context.getSource());
                    if (player == null) {
                        return Command.SINGLE_SUCCESS;
                    }
                    final UUID uuid = player.getUniqueId();
                    if (!lobby.present().contains(uuid)) {
                        player.sendMessage(Lang.render("lobby.ready.not_in_lobby"));
                        return Command.SINGLE_SUCCESS;
                    }
                    if (!config.enableReady()) {
                        player.sendMessage(Lang.render("lobby.ready.disabled"));
                        return Command.SINGLE_SUCCESS;
                    }
                    final boolean nowReady = !ready.isReady(uuid);
                    ready.setReady(uuid, nowReady);
                    player.sendMessage(nowReady ? Lang.render("lobby.ready.toggle_on")
                            : Lang.render("lobby.ready.toggle_off"));
                    return Command.SINGLE_SUCCESS;
                })
                .build();
    }

    /** /shdf leave:玩家域单口离场(固定 KICK 即时移除);不自传送,回大厅传送归 LobbyReturnListener MemberLeft 分支。 */
    private static LiteralCommandNode<CommandSourceStack> leaveNode(final HydrokarbsApi hk) {
        return Commands.literal("leave")
                .executes(context -> {
                    final Player player = playerOf(context.getSource());
                    if (player == null) {
                        return Command.SINGLE_SUCCESS;
                    }
                    if (hk.leaveSession(player.getUniqueId())) {
                        player.sendMessage(Lang.render("command.leave.left"));
                    } else {
                        player.sendMessage(Lang.render("command.leave.not_in_session"));
                    }
                    return Command.SINGLE_SUCCESS;
                })
                .build();
    }

    /** 玩家限定子命令的发送方提取:非玩家回红字返 null(玩家命令不挂权限,以发送方类型收口)。 */
    private static Player playerOf(final CommandSourceStack source) {
        if (source.getSender() instanceof Player player) {
            return player;
        }
        source.getSender().sendMessage(Lang.render("command.player_only"));
        return null;
    }
}
