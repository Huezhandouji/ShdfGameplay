package com.shdfGameplay.command;

import com.shdfGameplay.component.role.RoleComponent;
import com.shdfGameplay.component.role.RoleComponentDirectory;
import java.util.UUID;
import org.bukkit.command.CommandSender;

/**
 * 调试命令:/sm debug role info 的宿主等价实现(挂 /shdf debug role info):经 RoleComponentDirectory.lookup→RoleComponent 读口打印角色 id/能量/SanTE,零桥接 import(★23 活标本)。
 * 来源:施工计划/阶段3 3.5-监听器与击杀归属 步骤5;16-路线图与验收 §16.1-P2 验收方式第 1 条;README 对齐注①⑤。
 */
public final class DebugCommand {

    private DebugCommand() {
    }

    /**
     * 打印目标玩家角色信息:角色 id(roleIdOf,empty 显示"无角色")/能量(energyOf,empty 显示"—")/SanTE(sanTeOf,同上)。
     */
    public static void printRoleInfo(CommandSender source, UUID target) {
        // TODO(3.5): 路由经 RoleComponentDirectory.lookup(target)→RoleComponent 读口打印(HydrokarbsApi 无玩家→实例→组件通道,sessionOf 仅回会话句柄、info() 现取的快照仍扁平 SessionInfo);缺席场景(未在会话/未挂组件/无角色)提示不炸——未挂角色组件的模式是合法状态;debug 输出含玩家名与角色显示名一律 Placeholder.unparsed(§14.10 文本注入红线);经 ShdfCommands 挂 /shdf debug role info,阶段3 出口验收文写 /sm debug role info——以 /shdf 等价实现对照验收 —— 出自 3.5-监听器与击杀归属 步骤5(:70-77);14-陷阱清单 §14.10。
    }
}
