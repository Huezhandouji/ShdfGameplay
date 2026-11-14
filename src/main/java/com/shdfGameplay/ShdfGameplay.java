package com.shdfGameplay;

import com.hydrokarbs.api.EventSubscription;
import com.hydrokarbs.api.HydrokarbsApi;
import com.shdfGameplay.bridge.role.RoleEventHandle;
import com.shdfGameplay.listener.LobbyEventsListener;
import com.shdfGameplay.lobby.LobbyManager;
import com.shdfGameplay.lobby.LobbyReturnListener;
import com.shdfGameplay.config.GameConfigManager;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * 宿主独立插件主类:全工程唯一 L0 装配点(框架 API 在场检查→大厅接线→显式注册→桥接接线→监听器与命令注册)。
 * 当前状态:仅大厅接线(部署闭环,零功能),其余装配序随施工子阶段落地。
 */
public final class ShdfGameplay extends JavaPlugin {

    /** 全工程唯一 subscribeSessionEvents 订阅句柄(LobbyReturnListener),onDisable close 幂等。 */
    private EventSubscription sessionEvents;

    /** 全工程唯一角色事件订阅句柄(官方事件流→RoleComponentDirectory.dispatch),onDisable close 幂等。 */
    private RoleEventHandle roleEvents;

    @Override
    public void onEnable() {
        // 1. 首行在场检查(v1.14 口径:api==null 拒载不崩服,无 API_VERSION 数字比对)
        final HydrokarbsApi hk = getServer().getServicesManager().load(HydrokarbsApi.class);
        if (hk == null) {
            getLogger().severe("HydrokarbsApi 不在场:Hydrokarbs 未启用或未注册服务,ShdfGameplay 拒载");
            setEnabled(false);
            return;
        }
        saveDefaultConfig();

        // 2. 配置管理器:一次性读取并缓存(mode_config/ 缺失=空集不炸;单文件失败跳过+warning 日志;地图 registerAll 随 2.5 首启资源释放后接入)
        final GameConfigManager games = new GameConfigManager(getDataFolder().toPath());
        games.init();
        getLogger().info("配置读取完成:" + games.modeIds().size() + " 个模式 / " + games.all().size() + " 张图");

        // 3. 大厅接线(零功能):LobbyManager 空账本 + 大厅三事件路由 + 散场订阅
        // TODO(2.5): LobbyConfig 自 config.yml lobby 块装填注入;LobbyVote/ReadyState/MatchBootstrap(五门+托管)与 ShdfCommands(/shdf)接线;
        //             SpecificConfigParsers 在此 L0 构造并逐模式显式注册(bosswar 随 6.3,idle 无解析器),注入 MatchBootstrap
        //             (玩法专用配置开局前解析、随 MatchConfigComponent 会话组件入局——替代 MapDefinition.metadata 词料通道,该通道已弃用) —— 出自 2.5 步骤2/3/5/10
        final LobbyManager lobby = new LobbyManager();
        this.sessionEvents = hk.subscribeSessionEvents(new LobbyReturnListener());
        getServer().getPluginManager().registerEvents(new LobbyEventsListener(lobby), this);

        // TODO(2.5): registerMode × 2(idlepad/idlepark,IdleModes 双工厂)+ registerSessionComponents(空)—— 出自 2.5 步骤8
        // TODO(3.1/3.4/3.5): RoleBridge.attach(new RoleFacade)+注册 ServiceWatchListener 并接 () -> RoleBridge.facade().invalidate()(动态感知,lambda 体住 L0)
        // +订阅角色插件正式事件流(死亡信号官方驱动的替换,不再挂 PlayerDeathEvent 探测):
        // roleEvents = RoleBridge.facade().subscribeRoleEvents(RoleComponentDirectory::dispatch)(回调体住 L0);
        // 向 MatchBootstrap 注入角色在场 BooleanSupplier(() -> RoleBridge.facade().available(),2.5 先恒真占位);
        // 监听器一次性注册(全部 @EventHandler 件住 listener/ 包)、ShdfCommands 经 LifecycleEvents.COMMANDS 挂 /shdf
        // —— 出自 08-角色桥接 §8.8(:313);角色插件 RoleAPI#subscribeEvents;施工计划/阶段3 README(:54)
    }

    @Override
    public void onDisable() {
        // 幂等收尾:订阅句柄 close(反注册三连随注册物落地后启用)
        if (this.sessionEvents != null) {
            this.sessionEvents.close();
            this.sessionEvents = null;
        }
        if (this.roleEvents != null) {
            this.roleEvents.close();
            this.roleEvents = null;
        }
        // TODO(2.5): 反注册三连(unregisterMode/unregisterMap/unregisterSessionComponents)+取消全部宿主 BukkitScheduler 任务 —— 出自 施工计划/2.5 步骤1
    }
}
