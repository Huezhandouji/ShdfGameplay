package com.shdfGameplay;

import com.hydrokarbs.api.EventSubscription;
import com.hydrokarbs.api.MiniGameApi;
import com.shdfGameplay.listener.LobbyEventsListener;
import com.shdfGameplay.lobby.LobbyManager;
import com.shdfGameplay.lobby.LobbyReturnListener;
import com.shdfGameplay.map.GameConfigs;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * 宿主独立插件主类:全工程唯一 L0 装配点(框架 API 在场检查→大厅接线→显式注册→桥接接线→监听器与命令注册)。
 *
 * <p>当前状态:仅大厅接线(部署闭环,零功能),其余装配序随施工子阶段落地。</p>
 *
 * <p>来源:README 对齐注②③⑧⑨;施工计划/2.5 步骤1;阶段3 README(L0 装配点);08-角色桥接 §8.8(:313)。</p>
 */
public final class ShdfGameplay extends JavaPlugin {

    /** 全工程唯一 subscribeSessionEvents 订阅句柄(LobbyReturnListener),onDisable close 幂等。 */
    private EventSubscription sessionEvents;

    @Override
    public void onEnable() {
        // 1. 首行在场检查(v1.14 口径:api==null 拒载不崩服,无 API_VERSION 数字比对)
        final MiniGameApi api = getServer().getServicesManager().load(MiniGameApi.class);
        if (api == null) {
            getLogger().severe("MiniGameApi 不在场:Hydrokarbs 未启用或未注册服务,ShdfGameplay 拒载");
            setEnabled(false);
            return;
        }
        saveDefaultConfig();

        // 2. 配置扫描(mode_config/ 缺失=空集不炸;地图 registerAll 随 2.5 首启资源释放后接入)
        final GameConfigs games = GameConfigs.scan(getDataFolder().toPath());
        getLogger().info("配置扫描完成:" + games.modeIds().size() + " 个模式 / " + games.all().size() + " 张图");

        // 3. 大厅接线(零功能):LobbyManager 空账本 + 大厅三事件路由 + 散场订阅
        // TODO(2.5): LobbyConfig 自 config.yml lobby 块装填注入;LobbyVote/ReadyState/MatchBootstrap(五门+托管)与 ShdfCommands(/shdf)接线 —— 出自 2.5 步骤2/3/5/10
        final LobbyManager lobby = new LobbyManager();
        this.sessionEvents = api.subscribeSessionEvents(new LobbyReturnListener());
        getServer().getPluginManager().registerEvents(new LobbyEventsListener(lobby), this);

        // TODO(2.5): registerMode × 2(idlepad/idlepark,IdleModes 双工厂)+ registerSessionComponents(空)—— 出自 2.5 步骤8
        // TODO(3.1/3.4/3.5): RoleBridge.attach(new RoleFacade)+注册 RoleSignalListener.ServiceWatchListener 并接 () -> RoleBridge.facade().invalidate()(动态感知,lambda 体住 L0);向 MatchBootstrap 注入角色在场 BooleanSupplier(() -> RoleBridge.facade().available(),2.5 先恒真占位);监听器一次性注册(全部 @EventHandler 件住 listener/ 包)、ShdfCommands 经 LifecycleEvents.COMMANDS 挂 /shdf —— 出自 08-角色桥接 §8.8(:313);施工计划/阶段3 README(:54)
    }

    @Override
    public void onDisable() {
        // 幂等收尾:订阅句柄 close(反注册三连随注册物落地后启用)
        if (this.sessionEvents != null) {
            this.sessionEvents.close();
            this.sessionEvents = null;
        }
        // TODO(2.5): 反注册三连(unregisterMode/unregisterMap/unregisterSessionComponents)+取消全部宿主 BukkitScheduler 任务 —— 出自 施工计划/2.5 步骤1
    }
}
