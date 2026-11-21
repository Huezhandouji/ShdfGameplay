package com.shdfGameplay.component.teleport;

import com.hydrokarbs.engine.component.MatchModeComponent;
import com.hydrokarbs.engine.event.MatchModeInstanceEvent;
import com.hydrokarbs.engine.instance.Participant;
import com.hydrokarbs.engine.port.MatchModeInstanceServices;
import com.shdfGameplay.lang.Lang;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

/**
 * 进出场传送组件(bosswar/conquest 空实现唯一挂件):实例生效/运行中加入逐人传入会话副本世界出生点,
 * 收摊逐人传回主世界出生点;副本世界名 = mapId + "-" + 会话短码(框架副本命名约定),解析失败 warning 跳过
 * 不抛(防御);钩子均主线程执行(引擎节拍/切换单 tick 主线程),直调 Bukkit API。
 * 来源:本次任务(bosswar/conquest 空实现);框架 SessionId#shortCode 与 WorldPortImpl 副本命名约定(§3.7);04-大厅模式 §4.6/§4.8 传送分工。
 */
public final class MapTeleportComponent extends MatchModeComponent {

    /**
     * 装配描述符:create 为唯一构造点(组件 id 由模式装配器 addComponent→bindId 绑入并经 create 交付)。
     */
    public static final class Spec extends MatchModeComponent.Specification<MapTeleportComponent> {

        public Spec() {
            super("map_teleport");
        }

        @Override
        public MapTeleportComponent create(String id, MatchModeInstanceServices svc) {
            return new MapTeleportComponent(id, svc);
        }
    }

    private MapTeleportComponent(String id, MatchModeInstanceServices svc) {
        super(id, svc);
    }

    /**
     * 实例生效路径(含经切换进入的首实例):startAll 名册原地位移先于组件 start 钩子,本钩子执行时参赛者表已满。
     */
    @Override
    public void start() {
        final World world = instanceWorld();
        if (world == null) {
            return;
        }
        for (final Participant participant : svc().self().participants()) {
            teleportTo(participant.uuid(), world);
        }
    }

    /**
     * 运行中加入路径(实例已 RUNNING 时玩家逐人 handle.join):框架 join 只登记名册/参赛者、不传送,传送由本组件承担。
     */
    @Override
    public void onEvent(final MatchModeInstanceEvent event) {
        if (!event.is(MatchModeInstanceEvent.ParticipantJoined.class)) {
            return;
        }
        final UUID joined = ((MatchModeInstanceEvent.ParticipantJoined) event).player();
        final World world = instanceWorld();
        if (world == null) {
            return;
        }
        teleportTo(joined, world);
    }

    /**
     * 收摊:当前参赛者逐人传回主世界出生点(口径 = 框架 HydrokarbsAssembly.hubSpawn():世界表首世界出生点;
     * 世界表空则跳过)。注:模式切换 B2 收摊先于 B3.5/B5 异图清场,提前传主世界会让切换经主世界中转一跳;
     * 本插件无续局(单场会话对局结束即散场),该口径可接受;解散时框架 hubRelocator/hubWorldClearer 亦兜底传主世界。
     */
    @Override
    public void stop() {
        final List<World> worlds = Bukkit.getWorlds();
        if (worlds.isEmpty()) {
            return;
        }
        final World hub = worlds.get(0);
        for (final Participant participant : svc().self().participants()) {
            teleportTo(participant.uuid(), hub);
        }
    }

    /** 副本世界解析:世界名 = mapId + "-" + 会话短码(框架 WorldPortImpl.instanceNameOf 命名,BukkitWorldLoader 以该名建世界);未就绪 warning 跳过(防御,不抛)。 */
    private World instanceWorld() {
        final String worldName = svc().self().map().id() + "-" + svc().self().session().id().shortCode();
        final World world = Bukkit.getWorld(worldName);
        if (world == null) {
            Bukkit.getLogger().warning(Lang.plain("log.config.instance_world_not_ready",
                    Placeholder.unparsed("world", worldName)));
        }
        return world;
    }

    /** 逐人传入指定世界出生点:玩家不在线(离线/已退场)跳过。 */
    private void teleportTo(final UUID uuid, final World world) {
        final Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            player.teleport(world.getSpawnLocation());
        }
    }
}
