package com.shdfGameplay;

import com.hydrokarbs.api.EventSubscription;
import com.hydrokarbs.api.HydrokarbsApi;
import com.shdfGameplay.bridge.role.RoleBridge;
import com.shdfGameplay.bridge.role.RoleEventHandle;
import com.shdfGameplay.bridge.role.RoleFacade;
import com.shdfGameplay.command.ShdfCommands;
import com.shdfGameplay.listener.LobbyEventsListener;
import com.shdfGameplay.listener.ServiceWatchListener;
import com.shdfGameplay.lobby.BukkitLobbyTeleport;
import com.shdfGameplay.lobby.GameFlow;
import com.shdfGameplay.lobby.LobbyManager;
import com.shdfGameplay.lobby.LobbyReturnListener;
import com.shdfGameplay.config.DefaultResources;
import com.shdfGameplay.config.GameConfigManager;
import com.shdfGameplay.mode.bosswar.BosswarModes;
import com.shdfGameplay.mode.conquest.ConquestModes;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * 宿主独立插件主类:全工程唯一 L0 装配点(框架 API 在场检查→大厅接线→显式注册→桥接接线→监听器与命令注册)。
 * 当前状态:大厅接线与桥接接线(L0 显式 attach+服务动态感知)已落地(部署闭环,零功能),其余装配序随施工子阶段落地。
 */
public final class ShdfGameplay extends JavaPlugin {

    /** 全工程唯一 subscribeSessionEvents 订阅句柄(LobbyReturnListener),onDisable close 幂等。 */
    private EventSubscription sessionEvents;

    /** 全工程唯一角色事件订阅句柄(官方事件流→RoleComponentDirectory.dispatch),onDisable close 幂等。 */
    private RoleEventHandle roleEvents;

    /** 框架 API 引用(onEnable 在场检查通过后持有,模式注册/反注册用;onDisable 摘除后置 null)。 */
    private HydrokarbsApi api;

    @Override
    public void onEnable() {
        // 1. 首行在场检查(v1.14 口径:api==null 拒载不崩服,无 API_VERSION 数字比对)
        final HydrokarbsApi hk = getServer().getServicesManager().load(HydrokarbsApi.class);
        if (hk == null) {
            getLogger().severe("HydrokarbsApi 不在场:Hydrokarbs 未启用或未注册服务,ShdfGameplay 拒载");
            setEnabled(false);
            return;
        }
        this.api = hk;
        // 首启资源释放:jar 随包资源全量拷至数据目录(已存在不覆写;含 config.yml,覆盖 saveDefaultConfig 单文件口径)
        DefaultResources.release(getFile().toPath(), getDataFolder().toPath());

        // 2. 配置管理器:一次性读取并缓存(config.yml 主配置带病响亮;图配置由 map_mode_config 标记文件点名读取,
        //    标记/条目带病跳过+warning 日志;地图 registerAll 随 2.5 接入——首启资源释放已由 DefaultResources 就位)
        final GameConfigManager games = new GameConfigManager(getDataFolder().toPath());
        games.init();
        getLogger().info("配置读取完成:" + games.modeIds().size() + " 个模式 / " + games.all().size() + " 张图");

        // 3. 大厅接线(零功能→流程开关就位):GameFlow 构造即静默,launch 前不进任何运行态(配置读取/模式注册仍在 onEnable 一次性完成)
        // TODO(2.5): LobbyVote/ReadyState/MatchBootstrap(五门+托管)接线;
        //             SpecificConfigParsers 在此 L0 构造并逐模式显式注册(bosswar 随 6.3,idle 无解析器),注入 MatchBootstrap
        //             (玩法专用配置开局前解析、随 MatchConfigComponent 会话组件入局——替代 MapDefinition.metadata 词料通道,该通道已弃用) —— 出自 2.5 步骤2/3/5/10
        final LobbyManager lobby = new LobbyManager();
        final GameFlow flow = new GameFlow(hk, games.lobby(), lobby, new BukkitLobbyTeleport(), getLogger());
        this.sessionEvents = hk.subscribeSessionEvents(new LobbyReturnListener(flow));
        getServer().getPluginManager().registerEvents(new LobbyEventsListener(lobby), this);
        // 3.5 命令面:/shdf launch|stop 两个子命令(Brigadier LifecycleEvents.COMMANDS 代码注册;不自动 launch)
        ShdfCommands.register(this, flow);

        // 4. bosswar/conquest 空实现注册——仅进出场传送(map_teleport 组件:开局传副本世界出生点/收摊传回主世界出生点),
        //    玩法逻辑随施工阶段(首领战 6.3;占领战编号待路线图补定)
        this.api.registerMode(this, new BosswarModes());
        this.api.registerMode(this, new ConquestModes());

        // 5. 桥接接线(软依赖缺席不炸:懒解析构造期不取 API):L0 显式 attach 保持单一桥接实例(未 attach 才走懒构造)
        //    —— 出自 施工计划3.4 步骤1(§14.25 待裁决,L0 显式 attach 为推荐形态)
        RoleBridge.attach(new RoleFacade());
        // 服务动态感知:角色插件中途装/卸时失效懒解析缓存并触发事件订阅重订(lambda 体住 L0)
        // —— 出自 08-角色桥接 §8.8 动态感知行;施工计划3.1(先于订阅面施工)
        getServer().getPluginManager().registerEvents(new ServiceWatchListener(() -> RoleBridge.facade().invalidate()), this);

        // TODO(2.5): registerSessionComponents(空)—— 出自 2.5 步骤8(idle 空转测试双工厂已随遗留模式清理删除)
        // TODO(2.5/3.5): 订阅角色插件正式事件流(死亡信号官方驱动的替换,不再挂 PlayerDeathEvent 探测):
        // roleEvents = RoleBridge.facade().subscribeRoleEvents(RoleComponentDirectory::dispatch)(回调体住 L0);
        // 向 MatchBootstrap 注入角色在场 BooleanSupplier(() -> RoleBridge.facade().available(),2.5 先恒真占位);
        // 监听器一次性注册(全部 @EventHandler 件住 listener/ 包;ShdfCommands 挂线已随本次任务落地)
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
        // 摘除本次注册的两模式(幂等:id 不存在 = no-op,直调不 try/catch——对齐框架示例 TheirPlugin onDisable 纪律)
        if (this.api != null) {
            this.api.unregisterMode(this, BosswarModes.MODE_ID);
            this.api.unregisterMode(this, ConquestModes.MODE_ID);
            this.api = null;
        }
        // TODO(2.5): 反注册三连(unregisterMode/unregisterMap/unregisterSessionComponents)+取消全部宿主 BukkitScheduler 任务 —— 出自 施工计划/2.5 步骤1
    }
}
