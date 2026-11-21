package com.shdfGameplay;

import com.hydrokarbs.api.EventSubscription;
import com.hydrokarbs.api.HydrokarbsApi;
import com.hydrokarbs.api.SessionRecipe;
import com.hydrokarbs.component.session.EmptyArenaComponent;
import com.shdfGameplay.bridge.role.RoleBridge;
import com.shdfGameplay.bridge.role.RoleEventHandle;
import com.shdfGameplay.bridge.role.RoleFacade;
import com.shdfGameplay.command.ShdfCommands;
import com.shdfGameplay.config.DefaultResources;
import com.shdfGameplay.config.GameConfigManager;
import com.shdfGameplay.config.LobbyConfig;
import com.shdfGameplay.config.SpecificConfigParsers;
import com.shdfGameplay.listener.LobbyEventsListener;
import com.shdfGameplay.listener.ServiceWatchListener;
import com.shdfGameplay.lobby.BukkitLobbyTeleport;
import com.shdfGameplay.lobby.BukkitMatchCycle;
import com.shdfGameplay.lobby.GameFlow;
import com.shdfGameplay.lobby.LobbyManager;
import com.shdfGameplay.lobby.LobbyReturnListener;
import com.shdfGameplay.lobby.LobbyTeleport;
import com.shdfGameplay.lobby.LobbyView;
import com.shdfGameplay.lobby.LobbyVote;
import com.shdfGameplay.lobby.MatchBootstrap;
import com.shdfGameplay.lobby.MatchCycle;
import com.shdfGameplay.lobby.ReadyState;
import com.shdfGameplay.lobby.ui.BukkitLobbyView;
import com.shdfGameplay.lang.Lang;
import com.shdfGameplay.mode.bosswar.BosswarModes;
import com.shdfGameplay.mode.conquest.ConquestModes;
import com.shdfGameplay.session.SessionRecipes;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

/**
 * 宿主装配器:全工程唯一 L0 装配点(框架 API 在场检查→配置装配→大厅接线→显式注册→桥接接线;
 * {@link #disable} 幂等收尾)。主类 ShdfGameplay 只剩生命周期壳(onEnable 构造本类并委托 assemble,onDisable 委托 disable)。
 * 装配序各步拆为模块装配函数(loadApi/releaseDefaultResources/assembleConfigs/assembleLobby/registerModes/wireRoleBridge),
 * {@link #assemble} 只作目录序。当前状态:大厅接线(流程开关+开局周期编排+命令面+三事件路由+散场订阅)与
 * 桥接接线(L0 显式 attach+服务动态感知)已落地,其余装配序随施工子阶段落地。
 */
public final class Assembly {

    /** 全工程唯一 subscribeSessionEvents 订阅句柄(LobbyReturnListener),disable close 幂等。 */
    private EventSubscription sessionEvents;

    /** 全工程唯一角色事件订阅句柄(官方事件流→RoleComponentDirectory.dispatch),disable close 幂等。 */
    private RoleEventHandle roleEvents;

    /** 框架 API 引用(assemble 在场检查通过后持有,模式注册/反注册用;disable 摘除后置 null)。 */
    private HydrokarbsApi api;

    /** 会话配方登记簿(配置装配后先构造注入 MatchBootstrap,登记在 registerModes 与模式集同集落账)。 */
    private SessionRecipes sessionRecipes;

    /** 玩法专用配置解析器登记簿(L0 构造注入 MatchBootstrap;2.5 暂无注册项,bosswar 随 6.3)。 */
    private SpecificConfigParsers parsers;

    /** 开局周期(GameFlow launch/stop 联动;disable 先停再取消全部宿主任务)。 */
    private MatchCycle cycle;

    /** 宿主插件(生命周期与资源口:getServer/getLogger/jarFile/getDataFolder/setEnabled,注册归属方)。 */
    private final ShdfGameplay plugin;

    /**
     * @param plugin 宿主插件(onEnable 构造传入,本类不落盘不读盘,资源口均经插件)
     */
    public Assembly(final ShdfGameplay plugin) {
        this.plugin = plugin;
    }

    /**
     * 装配序目录:⓪语言装填→①在场检查→首启资源释放→②配置装配→③/3.5 大厅接线与命令面→④模式与会话配方注册→⑤桥接接线;
     * 拒载即中止(后续步骤不执行)。
     */
    public void assemble() {
        // 0. i18n 门面先行装填(全工程文案出口 Lang.render/plain 依赖语言模板,须早于任何文案消费)
        Lang.init(this.plugin);
        // 1. 首行在场检查(v1.14 口径:api==null 拒载不崩服,无 API_VERSION 数字比对)
        final HydrokarbsApi hk = loadApi();
        if (hk == null) {
            return; // 拒载已处理(SEVERE+setEnabled(false)),装配序到此中止
        }
        this.api = hk;
        releaseDefaultResources();                          // 首启资源释放
        final GameConfigManager games = assembleConfigs();  // 2. 配置装配
        // 会话配方登记簿提前构造(bootstrap 构造注入;pinned 开局补挂在运行期消费,登记在 ④ 与 registerMode 同集落账)
        this.sessionRecipes = new SessionRecipes();
        // 玩法专用配置解析器 L0 构造(2.5 暂无注册项,bosswar 随 6.3)—— 出自 2.5 步骤5
        this.parsers = new SpecificConfigParsers();
        assembleLobby(hk, games);                           // 3./3.5. 大厅接线+命令面
        registerModes();                                    // 4. 模式与会话配方注册
        wireRoleBridge();                                   // 5. 桥接接线
    }

    /**
     * 首行在场检查(v1.14 口径:api==null 拒载不崩服,无 API_VERSION 数字比对)。
     *
     * @return 框架 API;不在场时已 SEVERE + setEnabled(false) 拒载,返 null 由装配序中止
     */
    private HydrokarbsApi loadApi() {
        final HydrokarbsApi hk = this.plugin.getServer().getServicesManager().load(HydrokarbsApi.class);
        if (hk == null) {
            this.plugin.getLogger().severe(Lang.plain("log.assembly.api_absent"));
            this.plugin.setEnabled(false);
        }
        return hk;
    }

    /** 首启资源释放:jar 随包资源全量拷至数据目录(已存在不覆写;含 config.yml,覆盖 saveDefaultConfig 单文件口径)。 */
    private void releaseDefaultResources() {
        DefaultResources.release(this.plugin.jarFile(), this.plugin.getDataFolder().toPath());
    }

    /**
     * ②配置装配:一次性读取并缓存(config.yml 主配置带病响亮;图配置由 map_mode_config 标记文件点名读取,
     * 标记/条目带病跳过+warning 日志;地图 registerAll 随 2.5 接入——首启资源释放已由 DefaultResources 就位)。
     *
     * @return 已 init 的配置管理器(供大厅接线取 lobby 块)
     */
    private GameConfigManager assembleConfigs() {
        final GameConfigManager games = new GameConfigManager(this.plugin.getDataFolder().toPath());
        games.init();
        this.plugin.getLogger().info(Lang.plain("log.assembly.configs_loaded",
                Placeholder.unparsed("mode_count", String.valueOf(games.modeIds().size())),
                Placeholder.unparsed("map_count", String.valueOf(games.all().size()))));
        return games;
    }

    /**
     * ③/3.5 大厅接线+命令面(投票/准备/开局单闸/开局周期/散场订阅/流程开关/三事件路由全链就位):
     * GameFlow 构造即静默,launch 前不进任何运行态(配置读取/模式注册仍在 assemble() 一次性完成);
     * LobbyManager 保持两参账本职责不聚合三件,组合住本装配点与周期壳(2.5 裁决)。
     *
     * @param hk    框架 API(订阅会话事件与账本自查用)
     * @param games 已 init 的配置管理器(取 lobby 块装填各件)
     */
    private void assembleLobby(final HydrokarbsApi hk, final GameConfigManager games) {
        final LobbyConfig lobbyConfig = games.lobby();
        // 大厅回传缝单实例:GameFlow stop 两段传送、LobbyManager join 接回、命令传送与 MemberLeft 接引共用(无状态适配器)
        final LobbyTeleport teleport = new BukkitLobbyTeleport();
        final LobbyManager lobby = new LobbyManager(hk, teleport);
        final LobbyVote vote = new LobbyVote(games, new Random());
        final ReadyState ready = new ReadyState();
        // 开局单闸八参:第五门角色在场经 lambda 注入(体住 L0,lobby 包不 import bridge.role;★23),
        // join 成功回写账本=lobby::leave(反向引用,零循环依赖)—— 出自 04 §4.3 托管时序
        // TODO(3.5): 第五门换真实现(按玩法判定:需要角色的玩法才查 RoleFacade 在场性,04 §4.3 门表/§4.7 不陪葬);
        // 2.5 本阶段无角色玩法,恒真 lambda 占位——真实现提前接会让角色插件缺席陪葬全部模式开局 —— 出自 2.5 步骤4「第五门留接口」
        final MatchBootstrap bootstrap = new MatchBootstrap(hk, vote, games, this.parsers, this.sessionRecipes,
                lobbyConfig.minPlayers(), () -> true, lobby::leave);
        // 交互层生产壳(物品/投票菜单/侧边计分板,ZeoliteLib 菜单与物品支持库):随周期起停、每拍差集驱动 —— 出自 本次任务(大厅交互层)
        final LobbyView view = new BukkitLobbyView(lobby, vote, ready, games, lobbyConfig);
        // 开局周期生产壳:判定核参数(人数门/准备门/倒计时)取 lobby 块,起票口径 map_vote_setting
        this.cycle = new BukkitMatchCycle(this.plugin, games, lobbyConfig, lobby, vote, ready, bootstrap, view, new Random());
        final GameFlow flow = new GameFlow(hk, lobbyConfig, lobby, this.cycle, teleport, this.plugin.getLogger());
        // 全工程唯一订阅接线(散场单闸回调体;五参构造)—— 出自 04 §4.6/§4.8
        this.sessionEvents = hk.subscribeSessionEvents(
                new LobbyReturnListener(flow, lobby, teleport, lobbyConfig, this.plugin.getLogger()));
        // 大厅三事件路由(PlayerMove 节流进出场/Quit 清账/Join 自查接回)一次性注册,不随流程开关反复
        this.plugin.getServer().getPluginManager().registerEvents(new LobbyEventsListener(lobby), this.plugin);
        // 3.5 命令面:/shdf launch|stop|lobby|vote <n>|ready|leave 六子命令(Brigadier 代码注册;不自动 launch)
        ShdfCommands.register(this.plugin, flow, lobby, vote, ready, hk, lobbyConfig, teleport);
    }

    /**
     * ④模式与会话配方注册:bosswar/conquest 空实现——仅进出场传送(map_teleport 组件:开局传副本世界出生点/收摊传回主世界出生点),
     * 玩法逻辑随施工阶段(首领战 6.3;占领战编号待路线图补定);配方登记集与 registerMode 模式集一致。
     */
    private void registerModes() {
        this.api.registerMode(this.plugin, new BosswarModes());
        this.api.registerMode(this.plugin, new ConquestModes());
        // 会话配方登记:与上面 registerMode 模式集一致(静态模板=空场 ~10s 解散组件;钉图与 MatchConfigComponent
        // 由 MatchBootstrap 开局经 pinned 补挂)——tdm/bomb 配方随 5.1/6.1 模式上线补登 —— 出自 04 §4.3 冻结形态
        this.sessionRecipes.register(BosswarModes.MODE_ID, singleMatchRecipe(BosswarModes.MODE_ID));
        this.sessionRecipes.register(ConquestModes.MODE_ID, singleMatchRecipe(ConquestModes.MODE_ID));
        // registerSessionComponents 本阶段无全局贡献(空实现位)—— 出自 2.5 步骤8(idle 空转测试双工厂已随遗留模式清理删除)
    }

    /**
     * 单场会话配方(配方构造住 L0 调用方,SessionRecipes 登记簿只管登记回查):本插件无续局,不钉图
     * (MatchBootstrap 开局按 cfg.mapId 补)、空 mapPool(无图池轮换),仅携带空场解散组件(空场 ~10s 解散,DissolveReturnTest 依赖)。
     *
     * @param modeId 模式 id(注册表唯一键,与 mode_config/ 目录名一致)
     * @return 该模式的静态配方(MatchBootstrap 开局在其 extraComponents 前补 MatchConfigComponent)
     */
    private static SessionRecipe singleMatchRecipe(final String modeId) {
        return new SessionRecipe(modeId, Optional.empty(), List.of(),
                List.of(new EmptyArenaComponent.Spec(Duration.ofSeconds(10))));
    }

    /**
     * ⑤桥接接线(软依赖缺席不炸:懒解析构造期不取 API):L0 显式 attach 保持单一桥接实例(未 attach 才走懒构造)
     * + 服务动态感知。
     */
    private void wireRoleBridge() {
        // —— 出自 施工计划3.4 步骤1(§14.25 待裁决,L0 显式 attach 为推荐形态)
        RoleBridge.attach(new RoleFacade());
        // 服务动态感知:角色插件中途装/卸时失效懒解析缓存并触发事件订阅重订(lambda 体住 L0)
        // —— 出自 08-角色桥接 §8.8 动态感知行;施工计划3.1(先于订阅面施工)
        this.plugin.getServer().getPluginManager().registerEvents(
                new ServiceWatchListener(() -> RoleBridge.facade().invalidate()), this.plugin);
        // TODO(3.5): 订阅角色插件正式事件流(死亡信号官方驱动的替换,不再挂 PlayerDeathEvent 探测):
        // roleEvents = RoleBridge.facade().subscribeRoleEvents(RoleComponentDirectory::dispatch)(回调体住 L0);
        // 监听器一次性注册(全部 @EventHandler 件住 listener/ 包)
        // —— 出自 08-角色桥接 §8.8(:313);角色插件 RoleAPI#subscribeEvents;施工计划/阶段3 README(:54)
        // (MatchBootstrap 角色在场 BooleanSupplier 已随 2.5 大厅逻辑在 assembleLobby 注入:2.5 恒真占位,按玩法判定的真实现随本 TODO 换入)
    }

    /**
     * 幂等收尾:先停开局周期并取消全部宿主 BukkitScheduler 任务(防重载任务翻倍)→订阅句柄 close→
     * 模式与会话配方/解析器反注册清账。
     */
    public void disable() {
        // 先停开局周期(取消节拍任务+清票本/准备账本/判定核),再兜底取消全部宿主任务 —— 出自 2.5 步骤1、§14.6
        if (this.cycle != null) {
            this.cycle.stop();
            this.cycle = null;
        }
        this.plugin.getServer().getScheduler().cancelTasks(this.plugin);
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
            this.api.unregisterMode(this.plugin, BosswarModes.MODE_ID);
            this.api.unregisterMode(this.plugin, ConquestModes.MODE_ID);
            this.api = null;
        }
        // 会话配方登记簿清账(与 registerMode 反注册同口径)
        if (this.sessionRecipes != null) {
            this.sessionRecipes.clear();
            this.sessionRecipes = null;
        }
        // 玩法专用配置解析器登记簿清账(同口径)
        if (this.parsers != null) {
            this.parsers.clear();
            this.parsers = null;
        }
        // TODO(后续): unregisterMap/unregisterSessionComponents 随地图装配器与全局组件注册落地后启用 —— 出自 施工计划/2.5 步骤1
    }
}
