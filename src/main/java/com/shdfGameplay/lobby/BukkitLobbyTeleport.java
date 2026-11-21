package com.shdfGameplay.lobby;

import com.shdfGameplay.lang.Lang;
import java.util.UUID;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * 大厅回传缝生产适配器:Bukkit.getPlayer 解析(离线 null→false),同步 teleport
 * (对齐 MapTeleportComponent.teleportTo 既有口径),异常 catch→warning→false(防御不抛)。
 * —— 出自 本次任务(shdf-launchstop-流程开关)设计文档 §4.3。
 */
public final class BukkitLobbyTeleport implements LobbyTeleport {

    @Override
    public boolean toSpawn(final UUID player, final Location spawn) {
        final Player online = Bukkit.getPlayer(player);
        if (online == null) {
            return false;
        }
        try {
            return online.teleport(spawn);
        } catch (final Exception e) {
            // 极端兜底(如 spawn 世界运行期被卸载):不向上抛,调用方按离线口径跳过
            Bukkit.getLogger().warning(Lang.plain("log.lobby.teleport_failed",
                    Placeholder.unparsed("player", player.toString()),
                    Placeholder.unparsed("error", String.valueOf(e))));
            return false;
        }
    }
}
