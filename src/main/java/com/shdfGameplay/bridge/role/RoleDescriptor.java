package com.shdfGameplay.bridge.role;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;

import java.util.List;

/**
 * 自家角色值对象:由 RoleInfo 转换而来,不泄漏角色系统类型;恰四字段且不含阵营
 * (RoleInfo 只有这四个成分,角色目录无阵营来源——要"某角色声明的默认阵营"只能从正持该角色的玩家实例读 declaredFaction)。
 * 来源:08-角色桥接 §8.2(自家值对象);施工计划 3.1 步骤3;README 对齐注①适用。
 */
public record RoleDescriptor(
        String id,
        Component displayName,
        List<Component> description,
        Material icon
) {

    // TODO(3.1): 由 RoleInfo 转换构造(转换住 RoleFacade.catalog(),本 record 自身零行为);icon 缺失回 Material.AIR —— 出自 08-角色桥接 §8.2/施工计划3.1 步骤3
}
