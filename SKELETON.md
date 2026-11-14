# SHDF 宿主插件骨架总览(SKELETON.md)

> 本文是对工作区**最终骨架**的解说文档,事实底座为 2026-11-07 对工作区的逐文件通读
> (52 个 Java 桩 + 7 个资源文件,含 `build.gradle.kts`,共 60 个方案交付文件)与骨架方案(60 文件清单)。
> 设计文档根:`C:/Users/ROG/Desktop/mcplugin/hydrokarbs/docs/设计文档/Shdf具体玩法设计案`
> (其 README.md 头部对齐注 ①~⑩ 优先级高于正文旧口径;下文「README 对齐注」均指该文件)。
> 骨架纪律:每文件头部中文 Javadoc(一句职责 + 来源行),体内**零实现逻辑**,仅 TODO 标记与空方法/record 声明。

---

## ① 包结构与各包职责表

宿主根包 `com.shdfGameplay`(设计文档宿主包 `com.shdf.*` 的工程映射);框架类一律 `com.hydrokarbs.*` 原样引用。
六模块边界(文件互不越界,**★23 分层纪律**:除 `bridge/role` 与 `component/role` 外任何类不得 import 桥接类型,
出处 16-路线图与验收.md:65)。

### 1.1 目录树(实测 `find src -type f`)

```
src/main/java/com/shdfGameplay/
├── ShdfGameplay.java              # 主类(唯一 L0 装配点)
├── ShdfGameplayLoader.java        # Paper PluginLoader(维持空)
├── bridge/role/                   # 角色桥接(防腐层,9 件)
├── command/                       # 命令面(2 件)
├── component/
│   ├── MatchConfigComponent.java  # 本局玩法配置会话组件
│   ├── MatchEndReturn.java        # 散场引导会话组件
│   ├── display/                   # 表现层宿主组件(2 件)
│   └── role/                      # 角色组件层(9 件)
├── config/                        # 玩法配置读取与解析(6 件)
│   └── matchSpecificConfig/       # 各模式专用配置 record(1 件)
├── listener/                      # 全工程唯一允许 @EventHandler 的层(4 件)
├── lobby/                         # 大厅(5 件)
├── map/                           # 地图装配(4 件:装配器+区域表示法三件)
└── mode/
    ├── idle/                      # 空转测试模式(1 件,双工厂)
    ├── tdm/                       # 团队死斗(3 件)
    ├── bomb/                      # 炸弹攻防(6 件)
    └── boss/                      # 首领战(5 件)
src/main/resources/
├── plugin.yml  config.yml
├── maps/                            # 世界存档(example_world/ 完整世界目录)
└── mode_config/{bosswar/, conquest/}   # 详见 ③
```

### 1.2 各包职责表(职责逐字摘自各文件头部 Javadoc;来源=该头部的「来源」行)

| 包 / 位置 | 文件(嵌套件) | 职责 | 来源文档(篇目 §节) |
|---|---|---|---|
| **装配模块** | | | |
| `com.shdfGameplay`(根) | `ShdfGameplay` | 宿主独立插件主类:全工程唯一 L0 装配点(onEnable 首行框架 API 在场检查→显式注册→桥接接线→监听器与命令注册;onDisable 反注册三连) | README 对齐注②③⑧⑨;施工计划/2.5 步骤1;阶段3 README(L0 装配点);08-角色桥接 §8.8(:313) |
| `com.shdfGameplay`(根) | `ShdfGameplayLoader` | Paper 插件类路径加载器:维持空实现,禁止动态依赖加载 | 工程现状;README 对齐注②;2.5(:29 禁 shade 行) |
| `listener` | `SessionSignalListeners`(内嵌 `InteractionListener`/`ProtectionListener`) | 宿主事件面两占位监听器(L0 豁免层):交互/防护占位桩,框架线阶段4 4.2「填实而非新建」 | 3.5 步骤4(:61-65);§15.1;框架阶段4 README 4.2 行(:43)/§3(:69) |
| `resources`/根 | `plugin.yml`、`build.gradle.kts` | 插件声明(depend/softdepend)与 Gradle 构建(mavenLocal+角色坐标) | README 对齐注③⑧;施工计划 §4.2;08 §8.8 表 |
| **大厅模块** | | | |
| `lobby` | `LobbyManager`(内嵌 `LobbyConfig`) | 主世界大厅管理器:区域进出判定、present 账本、聚合大厅三件(LobbyVote/ReadyState/MatchBootstrap) | 04-大厅模式 §4.2(:55-58);2.5 步骤2;README 对齐注⑩ |
| `listener`(模块归大厅) | `LobbyEventsListener` | 大厅三事件路由件(方案新增):PlayerMove 节流进出场/PlayerQuit 清账/PlayerJoin 自查接回——只路由,业务住 LobbyManager | 04 §4.2(:55-58);2.5 步骤2;12-代码骨架.md:202 与 3.5:108(listener 唯一 @EventHandler 层) |
| `lobby` | `LobbyVote` | 大厅『下一场玩什么』投票:候选集 mode@map、多数胜/平局 TieBreak.RANDOM、零票轮换兜底;纯宿主状态零框架类型 | 04 §4.2 表;2.5 步骤3;§14.10 |
| `lobby` | `ReadyState` | 大厅成员准备账本:全员 ready=提前收票/立即开局信号(人数门槛仍要过) | 04 §4.2;2.5 大厅三件之一 |
| `lobby` | `MatchBootstrap` | 开局单闸:五门判定后解析玩法专用配置(SpecificConfigParsers,带病开局前拒收)→SessionRecipe 钉图+extras 两件恒配置(MatchConfigComponent.Spec 首位+EmptyArenaComponent.Spec 空场 ~10s 解散)→api.createSession(★v1.15 返 SessionHandle)→逐人句柄 join 托管;**全工程唯一 createSession 调用点**;第五门『角色在场』经主类 L0 注入的 BooleanSupplier | 04 §4.3/§4.7;2.5 步骤5(:39)/关键实现要点;★23=16-路线图与验收.md:65 |
| `lobby` | `LobbyReturnListener` | 散场单闸:**全工程唯一 subscribeSessionEvents 订阅点**——Dissolved 接引收尾、MemberLeft 单人传送回大厅、ModeSwitchCompleted 记待散信号 | 04 §4.6/§4.8 命题②;2.5 步骤6 |
| `component` | `MatchConfigComponent`(内嵌 `Spec`) | 本局玩法配置宿主会话组件:开局前 MatchBootstrap 解析装入(解析结果或无配置模式的 null),随 SessionRecipe.extraComponents 恒入局;**跨模式切换存活**(bosswar 选队/选角/倒计时等复用准备大厅图的 mode 同样可读);模式组件经 svc().self().session().component() 只读,类型化读口 config(Class) 响亮 | 本次方案调整(会话组件传配置);框架 SessionView 软接入与 SessionComponent 跨切换存活(jar 已核) |
| `component` | `MatchEndReturn`(内嵌 `Spec`) | 散场引导宿主会话组件:本插件无续局,对局一结束即消费局末信号→逐人 leaveSession(玩家域单口,KICK 即时移除)+传送回大厅→AFK 宽限强制离场;全员离场后空场解散收尾 | 04 §4.4;2.5 步骤5(:39/:53 阶段5 随 TDM 落地) |
| `command` | `ShdfCommands` | /shdf 根命令面(Brigadier):lobby/vote \<n\>/ready/leave/debug session/debug role info 转调 | 2.5 步骤10 命令面;04 §4.2;§14.6 |
| **配置模块** | | | |
| `config` | `GameConfig` | 单图配置载体:mode_config/\<模式\>/\<图\>.yaml 自解析结果(modeId/mapId/map_path/展示三键/metadata),**已实现真实解析**;metadata 开局前经 SpecificConfigParser 解析为玩法专用 record、随会话组件入局(**不写入 MapDefinition.metadata,词料通道已弃用**) | 资源目录 mode_config/ 现行结构;docs/玩法设计/bosswar.md;04 §4.5 分工表 |
| `config` | `GameConfigManager` | mode_config/ 读取与缓存管理器:init() 一次性扫描按 modeId 归组(单文件失败跳过+warning 不阻整体,重复 init 整体换缓存);byMode/all/modeIds/find 读口,MatchBootstrap 与 LobbyVote 候选集数据源 | 资源目录 mode_config/ 现行结构;04 §4.3 字段 |
| `config` | `ConfigUtil` | 配置解析工具:metadata 嵌套映射→区域(parseRegion 按 type 分发 cube/sphere)与标量/列表值对象;非法一律响亮 IllegalStateException | docs/玩法设计/bosswar.md |
| `config` | `SpecificConfigParser`(接口) | 玩法专用配置解析器契约:GameConfig→玩法专用 record;纯函数、带病响亮拒收(开局前,不进会话) | 本次方案调整(会话组件传配置,替代 MapDefinition.metadata) |
| `config` | `SpecificConfigParsers` | modeId→解析器注册处:L0 构造逐模式显式注册(无 SPI)、注入 MatchBootstrap 开局前 find 取用 | 同上 |
| `config.matchSpecificConfig` | `BosswarSpecificConfig`(内嵌 `Parser`) | bosswar 专用配置 record:三区域(红/蓝出生点、游玩限定区)/首领生命数/三 tick 计时/重复角色开关/双方可用角色 id,全必填无缺省 | docs/玩法设计/bosswar.md |
| `map` | `ShdfMapAssembler` | 地图装配器:取 GameConfigManager 缓存→按 map_path 去重合并(同世界多模式一份)→构造**两字段** MapDefinition(世界目录+默认 TEMPLATE_RESTORE,词料恒 AttributeBag.empty)→先落盘后 registerMap;响亮容错 | 04 §4.5 代码块;2.5 步骤7;README 对齐注⑦⑧ |
| `map.regionNotation` | `Region`/`CubeRegion`/`SphereRegion` | 区域表示法值对象:cube(corners/origin_and_size 按轴归一)与 sphere,负长度/NaN/Infinity 构造期拒收;BosswarSpecificConfig 等专用配置的复用底座 | docs/玩法设计/bosswar.md |
| `resources` | `config.yml`、`maps/**`、`mode_config/**` | 默认配置模板与世界存档(详见 ③) | 2.5 步骤9;10-表现层组件 display 配置块 |
| **模式模块** | | | |
| `mode.idle` | `IdleModes`(内嵌 `IdlePadFactory`/`IdleParkFactory`) | 空转测试模式双工厂(2.5 出口闸门用):Builder 最小声明,组件表空,互为 100 次切换往返对象 | 2.5 步骤8;README 对齐注⑧(禁 SPI) |
| `mode.tdm` | `TeamDeathmatchModes`(内嵌 `TdmEndConditions`/`TdmDisplays`) | TDM 模式工厂(首模式先例,声明序/测试形态供 bomb/boss 照抄):2~4 队击杀计分、定时重生;三判定器容器(100/90/85) | 05 §5.2-B/§5.5/§5.8;5.1 步骤5(:82-88 表);README 对齐注⑩ |
| `mode.tdm` | `TdmRules`(内嵌 `KillScoreRule`) | TDM 规则件容器:击杀计分规则(消费 RoleKillEvent,自杀不计、≥5 连杀加成);ScoreObjective 为框架侧交付物占位 | 05 §5.5 矩阵 TDM 行;5.1 步骤4;框架阶段4 README:42 |
| `mode.tdm` | `SuddenDeathComponent` | TDM 加时可选件:关重生/缩边界/双倍分(剩余时间<阈值且分差<阈值触发) | 05 §5.5-#20(SHDF 专用件住宿主 mode/**)、§5.2-B 阶段链 |
| `mode.bomb` | `BombDefusalModes`(内嵌 `BombDisplays`) | 炸弹攻防模式工厂:Builder 声明全部组件零生命周期代码;阶段链 Warmup→Prep(5s)→Live(115s)→RoundEnd(5s)→循环→MatchEnd | 05 §5.2-A/§5.5/§5.8;6.1 步骤8;README 对齐注①⑨ |
| `mode.bomb` | `BombConfig`(内嵌 `BombRules`) | 炸弹平衡参数载体(best-of 13/半场换边 6/安放 3s/引信 40s/拆除 7s/kit 3.5s/半径 6.0)+声明式规则簿数据 | 05 §5.4/§5.8;6.1 步骤2;README 对齐注⑧ |
| `mode.bomb` | `BombObjective`(内嵌 `BombObjectiveFactory`) | 炸弹目标状态机 UNPLANTED→PLANTED→EXPLODED\|DEFUSED;bomb-site 区域配置读会话 MatchConfigComponent(开局前解析装入,词料通道已弃用) | 05 §5.2-A;6.1 步骤3;README 对齐注⑨ |
| `mode.bomb` | `PlantInteraction` | 安放/拆除读条交互件:3s/7s+kit 减半,MOVE/DAMAGE/RELEASE/ITEM_SWITCH 四类打断 | 05 §5.7;6.2 步骤1~3;README 对齐注① |
| `mode.bomb` | `RoundLimitEndCondition` | 唯一挂 endgate 的整场判定:比分到顶 WIN/ROUND_LIMIT;回合级判定住框架 RoundComponent 内 | 05 §5.2-A 判定 100 行;6.1 步骤5/6(Q1 方案A);框架阶段4 README §5 待裁决-1 |
| `mode.bomb` | `BombRoundEndJudges` | 回合级四判定器容器:BombExploded(90)/AllDefused(80)/TimeExpired(70)/TeamWiped(60)——事件驱动不挂 endgate | 05 §5.2-A;6.1 步骤6;框架阶段4 README §5 待裁决-1 |
| `mode.boss` | `BossRaidModes`(内嵌 `BossEndConditions`/`BossDisplays`) | 首领战模式工厂:RAIDERS vs BOSS、限 0~1 命重生、阶段链 Warmup→P1/P2/P3(血线 66%/33%)→Resolution(30s)→MatchEnd;三判定器容器(100/90/80) | 05 §5.2-C/§5.5/§5.6;6.3 步骤1/6(:106-114 表);README 对齐注①⑨ |
| `mode.boss` | `BossComponent` | Boss 实体生成与血线阶段推进:start 期生成、update() 轮询血量(R2 裁决)、死亡广播;checkMap 前置校验 boss 出生点 | 05 §5.2-C/§5.5-#21;6.3 步骤4(R2 裁决/Q4 口径) |
| `mode.boss` | `BossPhaseGate`(内嵌 `BossHealthGate`) | Boss 血线阶段门(66%/33% 按 HP 阈值升序)+血线进度 Objective | 05 §5.2-C;6.3 步骤5;overview 条目 |
| `mode.boss` | `WipeCounter` | 存活数归零团灭判定素材:Resolution 阶段 30s 宽限防同 tick 竞态 | 05 §5.2-C;6.3 步骤5;overview 条目 |
| `mode.boss` | `AggroComponent` | Boss 仇恨表/目标选择/仇恨转移可选件(§5.5-#22 SHDF 专用件) | 05 §5.5-#22;6.3 步骤9 |
| `component.display` | `DisplayComponent`(内嵌 `Spec`) | 表现层宿主组件(自框架 v1.3 迁入):数据驱动渲染——上一帧 diff、参与者显示登记、stop 全员清除;Spec 收构建函数方法引用 | 10-表现层组件 §10.2;05 §5.6;README 对齐注头部 R2 |
| `component.display` | `DisplayModel`(内嵌 `DisplayLine`/`Countdown`/`Progress`) | 中立表现模型五分量(宿主资产,框架 jar 零表现层) | 10-表现层组件 §10.1;05 §5.6 |
| **角色桥接模块**(★23 豁免包之一) | | | |
| `bridge.role` | `RoleFacade` | 防腐层唯一类:公开面照自有名(恰 15 个 RoleAPI 映射方法=原 14 方法含备用 lastDamager+事件订阅面 subscribeRoleEvents)+阵营五动词类型化;懒解析(null 也缓存)、缺席读空/写 false 全类不抛(订阅面缺席登记在册、恢复自动补订);**全工程唯一 import 角色插件(`com.shadowHunterRolesPlugin.api.RoleAPI` 与 api/event 事件类)之处** | 08 §8.1/§8.2/§8.8/§8.10;角色插件 RoleAPI#subscribeEvents;3.1 步骤5 表;README 对齐注① |
| `bridge.role` | `RoleStateAccess`(内嵌 `RoleSnapshot`) | 字符串操作面→类型化读写(energy/sante/buffs 七具名动词+下标重载)+采样差分帧;OptionalInt 显式『读不到』,下标消歧只住本层 | 08 §8.3/§8.4;3.2 步骤1(:37-44) |
| `bridge.role` | `RoleDescriptor` | 自家角色值对象(不泄漏 RoleInfo),四字段不含阵营 | 08 §8.2(:97-104);3.1 步骤3 |
| `bridge.role` | `RoleFaction` | 阵营枚举 SHADOW/HUNTER/UNKNOWN/OTHER 与 parse 口径 | 08 §8.2/§8.10(二);3.1 步骤2 |
| `bridge.role` | `RoleCatalog` | 角色目录只读快照:awake 期解析缓存,显式 TTL(默认 30s),无写口、刷新整体替换 | 08 §8.5;3.2 步骤2(:47-53) |
| `bridge.role` | `RolePolicy`(内嵌 `AssignmentStrategy`) | 模式对角色系统的规则请求纯数据(八字段+策略枚举+NONE 常量,字段名与 08 §8.6 逐字一致),零行为,解释执行在 LoadoutFacet | 08 §8.6(:215-238);3.2 步骤3(:55-62) |
| `bridge.role` | `RoleBridge` | 桥接进程级持有器:static volatile+attach/facade(未 attach 懒构造);★23 夹逼下的唯一合规注入点 | 3.4 步骤1/§14.25 待裁决(L0 显式 attach 为推荐形态) |
| `bridge.role` | `RoleSignal`(内嵌 `Kind`) | 宿主词汇的角色生命周期信号:角色插件事件流(SET/CLEAR/DEATH)经 RoleFacade 订阅面翻译后的只读载体,角色插件事件类型不泄漏出防腐层;DEATH 载荷=死时所持角色 | 角色插件 RoleAPI#subscribeEvents 与 api/event 事件 javadoc;08 §8.1/§8.2 |
| `bridge.role` | `RoleEventHandle` | 角色事件订阅句柄:包住角色插件订阅令牌(类型不出防腐层),close 幂等;角色插件 reload 由 RoleFacade 自动重订,调用方无需关心 | 角色插件 RoleAPI#subscribeEvents 生命周期合约;08 §8.8 |
| **角色组件模块**(★23 豁免包之一) | | | |
| `component.role` | `RoleComponent`(内嵌 `AssignmentResult`/`Spec`) | 角色接入唯一落点(L3):MatchModeComponent 子类;对外强类型读口;状态唯一三表(occupied/lastSeen/reselects);stop() 全员 clearRole 唯一闸门 | 09 §9.2~§9.4;3.4 步骤6;README 对齐注⑤ |
| `component.role` | `RoleComponentDirectory` | 宿主侧角色组件登记簿(方案新增):awake 登记/stop 摘除,参与者只经实例事件维护;dispatch 把官方角色信号(DEATH)路由到所在实例 RoleComponent、lookup 供命令层路由玩家——弥补 HydrokarbsApi 无『玩家→实例→组件』通道 | 方案「玩家→组件路由裁决」(sessionOf 仅回会话句柄、info() 快照仍扁平 SessionInfo,javap 核);角色插件 RoleAPI#subscribeEvents;08 §8.9;09 §9.3 |
| `component.role` | `LoadoutFacet` | ①授权与占用 facet:解释 RolePolicy 的职责链(目录校验/全局禁用/按队池/占用去重/换角限额/人数门/记账-应用分离/善后/释放),包私有接缝 | 09 §9.3/§9.4;08 §8.6;3.4 步骤5①(:71-80) |
| `component.role` | `RoleFacets`(内嵌 `StateFacet`/`HostilityFacet`/`RewardsFacet`) | ②③④ facet 容器:采样差分/敌对口径唯一封装/事件→角色操作——均包私有接缝,构造于 awake,不是子组件 | 09 §9.3 决断/§9.4;3.4 步骤5②③④(:81-83) |
| `component.role` | `RoleEventSynth` | 无状态事件合成器:采样差分(值真变才产)/trustedKill 纯谓词(四条件);死亡合成(死前读序)已删——角色插件正式事件流自带死亡载荷,合成入口移 RoleComponent#onRoleDeath;采样差分保留(energy/sanTe 数值变更无正式通知面) | 08 §8.4;09 §9.5;3.4 步骤4、3.5 步骤2 改造(角色插件 RoleAPI#subscribeEvents) |
| `component.role` | `RoleKillEvent` | 击杀归属合成事件(record 五分量含 occurredAtMillis+紧凑工厂 of),不进 Bukkit 总线 | 08 §8.4 v3.1 注;3.4 步骤2(:45);框架 record 形态 javap 核 |
| `component.role` | `RoleStateChangedEvent` | 状态差分合成事件(值真的变了才发;含 occurredAtMillis 分量) | 同上 |
| `component.role` | `RoleReward`(内嵌 `RoleOpContext`) | 匹配谓词+效果奖励件与类型化操作上下文(奖励永不碰字符串协议) | 09 §9.6;08 §8.7;3.4 步骤3 |
| `component.role` | `RolePolicies` | 三策常量持有类:BOMB/TDM/BOSS_POLICY(可跨模式共享),值元组按 08 §8.6 字段序 | 08 §8.6 声明示例;09 §9.6;3.2 步骤4 |
| `listener`(模块归角色组件) | `ServiceWatchListener`(原 RoleSignalListener,死亡探测删除后扁平化) | 服务注册/注销动态感知(驱动 RoleFacade.invalidate:懒解析缓存失效+事件订阅重订);死亡等角色信号不再经 Bukkit 监听器探测——改官方事件流订阅(RoleFacade#subscribeRoleEvents→Directory.dispatch,接线住 L0) | 08 §8.8(:313);角色插件 RoleAPI#subscribeEvents;3.5 步骤1 改造;12-代码骨架:202 |
| `listener`(模块归角色组件) | `StatsSignalListener`(内嵌 `DamageLedger` seam) | MONITOR 伤害记账:onDamage 双侧记账 getFinalDamage();包私有 DamageLedger 过渡 seam(v3.1 拆分),框架线 4.4 填实 | 3.5 步骤3(seam 定义,:56);§12.2;§15.1 优先级表 |
| `command`(模块归角色组件) | `DebugCommand` | /shdf debug role info(/sm 等价实现):经 Directory.lookup→RoleComponent 读口打印,零桥接 import(★23 活标本) | 3.5 步骤5(:70-77);16-路线图 §16.1-P2 验收第 1 条 |

**listener/ 包纪律**:全工程 @EventHandler 只住 `com.shdfGameplay.listener`(12-代码骨架.md:202、3.5:108);模块归属可跨包、包位不可越;
主类与 bridge 包均不持 @EventHandler;监听器构造注入只进数据不进行为、不做服务定位。框架侧 `com.hydrokarbs.listener.SessionIngestListener`
为框架已有件(jar 已核),宿主不建同名占位。

**lobby 包框架 import 白名单**(恰七类):`HydrokarbsApi`/`SessionEventView`/`MapDefinition`/`JoinResult`/`LeaveReason`/`SessionRecipe`/`EmptyArenaComponent`;
createSession 返回的 SessionHandle 经 var 承接、不显式 import(快照 SessionInfo 经句柄 info() 现取,亦不显式 import)。lobby 包禁止 import bridge.role(第五门 BooleanSupplier 接法裁决)。

---

## ② 装配与注册链路(onEnable 时序)

装配唯一入口 = `ShdfGameplay.onEnable()`(全工程唯一 L0 装配点;`ShdfGameplay.java:13-14` 两条 TODO 即本时序)。
装载顺序由 `plugin.yml` 声明保证:`depend: [Hydrokarbs]`(框架硬依赖,先于宿主 enable)+ `softdepend: [ShadowHunterRolesPlugin]`
(角色系统软依赖)——注册全部在宿主 onEnable **显式**完成(时序反转 v1.6,无 SPI/META-INF/services)。

```
onEnable()
│
├─ 1.【首行·在场检查】getServer().getServicesManager().load(HydrokarbsApi.class)
│      api == null → SEVERE 日志 + setEnabled(false) 拒载不崩服(⑨ v1.14,无 API_VERSION 数字比对)
│      → 拔掉 Hydrokarbs 重启,宿主静默拒载、服务端照常
│
├─ 2.【配置装配】GameConfigManager.init(数据目录)          # mode_config/<模式>/<图>.yaml,缺目录空集不炸
│      └→ ShdfMapAssembler.registerAll(plugin, api, dataDir)  # 按 map_path 去重合并(同世界一份)
│                                                              # →MapDefinition 两字段(默认 TEMPLATE_RESTORE,词料恒空)→先落盘后 registerMap
│      └→ SpecificConfigParsers(解析器注册处):L0 构造,各模式解析器随模式落地显式注册(bosswar@6.3;idle 无)
│                                                              # 玩法配置不进 MapDefinition.metadata——开局前解析、随 MatchConfigComponent 会话组件入局
│
├─ 3.【注册模式】registerMode × 2(idlepad / idlepark)      # IdleModes 双工厂,Builder 最小声明
│
├─ 4.【注册会话组件】registerSessionComponents(空)          # 骨架期空集;MatchEndReturn 随 5.2 落
│
├─ 5.【桥接接线·阶段3.1】RoleBridge.attach(new RoleFacade)
│      └→ 注册 ServiceWatchListener,回调接 () -> RoleBridge.facade().invalidate()
│      └→ 订阅角色插件正式事件流:roleEvents = RoleBridge.facade().subscribeRoleEvents(RoleComponentDirectory::dispatch)
│         (死亡信号官方驱动,替代原 LOWEST 抢读探测;主线程同步送达;句柄供 onDisable close)
│         (动态感知:角色插件中途装/卸失效懒解析缓存;lambda 体住 L0)
│
├─ 6.【第五门注入】构造 BooleanSupplier ()->RoleBridge.facade().available() 注入 MatchBootstrap
│      (2.5 恒真 lambda 占位,3.5 换真实现;lobby 包只持 JDK 类型)
│
└─ 7.【大厅就绪】
       ├─ LobbyManager(LobbyConfig 自 config.yml lobby 块装填)
       ├─ LobbyReturnListener → api.subscribeSessionEvents(全工程唯一订阅点,句柄供 onDisable close)
       ├─ LobbyEventsListener / ServiceWatchListener / StatsSignalListener /
       │  SessionSignalListeners.registerAll —— 监听器一次性注册,不随模式切换反复 registerEvents
       └─ ShdfCommands 经 Brigadier LifecycleEvents.COMMANDS 挂 /shdf(plugin.yml 不写 commands 段)

onDisable()
└─ 反注册三连(unregisterMode / unregisterMap / unregisterSessionComponents)
   + 订阅句柄 close(幂等)+ 取消全部宿主 BukkitScheduler 任务
```

要点(出处如上各文件 TODO 行):
- **托管时序铁律**:先 `createSession`(★v1.15 返句柄)后逐人 `handle.join`;join 被拒按 `rej.detail()` 提示且留大厅;join 成功立即移出大厅账本。
- **玩法配置传递(本次方案)**:开局前 MatchBootstrap 经 SpecificConfigParsers 把选中 mode@map 的 GameConfig.metadata
  解析为玩法专用 record(带病响亮=开局失败,全员留大厅),装入 MatchConfigComponent.Spec 恒置 recipe extras 首位;
  组件跨模式切换存活,模式组件经 `svc().self().session().component(MatchConfigComponent.class)` 只读。
  原 MapDefinition.metadata 词料通道(ShdfMapKeys.mapDataKey)已弃用删除——bosswar 选队/选角/倒计时等 mode
  复用准备大厅图(REUSE_DIRTY),配置绑游戏图 metadata 时这些 mode 根本读不到,会话组件为唯一正解。
- **投票节拍**:`vote-seconds` 周期收票走宿主 BukkitScheduler runTaskTimer 自管节拍,不走框架 tick。
- **第五门接法(★23 裁决)**:MatchBootstrap 只持 `java.util.function.BooleanSupplier`,lambda 体住 L0——lobby 包零桥接 import。
- **玩家→组件路由**:RoleComponent awake 期在 RoleComponentDirectory 登记自身+空参与者集;参与者只经实例事件
  `ParticipantJoined`/`ParticipantLeft` 维护(禁绑 StateFacet 采样点——采样关闭模式不采样,绑了 Directory 恒空)。

---

## ③ maps/ 与 mode_config/ 配置目录说明

资源模板位于 `src/main/resources/`(骨架即默认模板;**2.5 落『首次启动从 jar 拷至宿主数据目录
`plugins/ShdfGameplay/`』逻辑**,此后宿主只读写数据目录份):

```
maps/                                 # 世界存档(完整 Minecraft 世界目录)
├── README.md                         # "在这个目录下存放地图存档"
└── example_world/                    # 示例世界(level.dat/region/entities/… 整目录)
mode_config/                          # 每个模式的地图配置(一文件一图)
├── README.md                         # "这个目录下包含每个模式的地图配置"
├── bosswar/example.yaml              # map_path + metadata(spawns / boss_life_number / play_region)
└── conquest/example.yaml             # map_path + metadata(hotpoints / spawns / play_region)
```

| 键 / 目录 | 消费方 | 说明 |
|---|---|---|
| `mode_config/<模式>/<图>.yaml` | `GameConfigManager.init`→`GameConfig` record | 一文件一图:文件名(去扩展名)= mapId,目录名 = modeId;**已实现真实解析**(Bukkit YamlConfiguration 递归取值) |
| `map_path` | `ShdfMapAssembler` | 世界目录相对数据目录的路径(如 `maps/example_world`);**去重合并键**:同 map_path 多模式合并为一份 MapDefinition(框架 id 按世界目录派生);目录未落盘→响亮 |
| `metadata` | `MatchBootstrap`→`SpecificConfigParser`(L0 注册于 `SpecificConfigParsers`)→`MatchConfigComponent`(会话组件) | 自由嵌套映射(bosswar: spawns/boss_life_number/play_region;conquest: hotpoints/spawns/play_region)开局前解析为玩法专用 record 装入会话组件,跨模式切换存活;**不再写入 MapDefinition.metadata**(原 ShdfMapKeys 词料通道已弃用删除) |
| 可选 `session-flow` | (已废弃,不再解析) | 本插件无续局:单场会话,对局结束即散场回大厅;session-flow 键与 SessionFlowPolicy 随续局方案一并移除,老配置带此键不炸(读取时忽略) |
| 重置策略 | `ShdfMapAssembler.DEFAULT_RESET_STRATEGY` | 恒 `TEMPLATE_RESTORE`(maps/ 为存档模板,每局模板恢复);per-yaml 覆盖键按需引入(TODO) |

- 装配响亮 `IllegalStateException`(不带病注册):yaml 空/语法错误、缺 `map_path` 键、世界目录未落盘(先落盘后注册)。
- `config.yml`(独立于两目录):`lobby` 块(bounds/spawn/vote-seconds/min-players)供 `LobbyManager.LobbyConfig`;
  `display` 块(refresh-interval=10、BossBar/Sidebar/Title 三开关、标题三段时长)5.1 生效。
- 后续扩容:新模式 = `mode_config/<新模式>/` 加 yaml + `maps/` 放对应世界存档;idle 两模式(idlepad/idlepark)配置随 2.5 最小闭环补。

---

## ④ 各模块 TODO 与施工计划子阶段对应表

TODO 编号 = 玩法线施工子阶段;跨线依赖显式写『框架线N.M』。前缀风格已全仓统一为
`TODO(<编号>)` 形态(如 `TODO(2.5)`、`TODO(3.1/3.4/3.5)`)。
grep 精确盘点(斜线合并编号拆分计数,如 `TODO(3.1/3.4/3.5)` 各计一处;续局方案移除后重盘):2.5=22、3.1=10、3.2=7、3.4=23、
3.5=10、5.1=7、5.2=3、6.1=8、6.2=4、6.3=7、
框架线4.1/4.2/4.4 各 1;6.4 与 7.3 为 0(见下)。

| 模块 | 2.5(大厅+idle 闭环) | 3.x(角色线) | 5.x(TDM 首模式) | 6.x(bomb/boss) | 框架线4.x(跨线回填) |
|---|---|---|---|---|---|
| **装配** | 主类装配序+API 在场检查+反注册三连(`ShdfGameplay`);Loader 维持空;plugin.yml 字段冻结 | 3.1:ServiceWatchListener 接线+第五门 BooleanSupplier 注入+testImplementation 角色坐标(`build.gradle.kts` TODO);3.5:监听器/命令注册行 | —(引擎零改动纪律,对本模块零改动) | — | 4.2:Interaction/Protection 占位桩填实(`SessionSignalListeners`);4.4:StatsComponent 直查删 DamageLedger seam(`StatsSignalListener`) |
| **大厅** | LobbyManager 三事件口径+七类白名单、LobbyEventsListener 路由、LobbyVote 自管节拍、ReadyState、MatchBootstrap 五门+托管铁律+extras 两件恒配置(MatchConfigComponent+EmptyArena 10s)、LobbyReturnListener 三同步点、ShdfCommands 挂载 | 3.5:debug role info 转调 DebugCommand(`ShdfCommands`) | 5.2:MatchEndReturn 消费 shdf.matchend(局末即散场,无续局)+逐人 KICK 传送 | — | — |
| **配置** | config/map 包已实现真实解析(读取缓存/单图载体/解析工具/专用配置解析器契约与注册处/装配器/区域表示法);2.5 剩:首启资源释放(mode_config/**+maps/** 拷至数据目录)+tags 与 per-yaml reset-strategy 可选键(`map` 包+`config.yml`)+MatchBootstrap 配置解析与 MatchConfigComponent 装入接线 | — | 5.1:display 块生效 | 6.1/6.3:各模式专用配置 record(BosswarSpecificConfig 已落,conquest 随模式补)+Parser 注册进 SpecificConfigParsers(L0),模式组件改读会话 MatchConfigComponent(词料通道已弃用) | — |
| **模式** | idle 双工厂显式注册+IdlePadRoundTripTest/IdleModeDeclarationTest(`IdleModes`) | — | 5.1:TDM 工厂声明序冻结+TdmEndConditions 三判定器+display 挂法+KillScoreRule+SuddenDeath;DisplayComponent/DisplayModel 随 5.1 落(`TeamDeathmatchModes`/`TdmRules`/`SuddenDeathComponent`/`DisplayComponent`/`DisplayModel`);5.2:2 人端到端+★18 战报 | 6.1:炸弹工厂挂法+目标状态机+整场判定+回合四判定+规则簿;6.2:读条交互+经济回填+半场换边;6.3:boss 工厂+血线门+WipeCounter+Aggro(`mode/bomb/**`、`mode/boss/**`) | 4.1:RoundComponent/EconomyComponent/ScoreObjective;TeamComponent/JoinControlComponent/ScoreComponent 等 FQN 回填(`TeamDeathmatchModes`/`BombDefusalModes`/`BossRaidModes`/`TdmRules`/`RoundLimitEndCondition`/`PlantInteraction`) |
| **角色桥接** | — | 3.1:RoleFacade 14 方法面+懒解析+语义硬点、RoleDescriptor、RoleFaction.parse;3.2:RoleStateAccess 七动词+下标、RoleCatalog.fetch、RolePolicy 八字段逐字(`bridge/role/**` 全 7 件) | — | — | — |
| **角色组件** | — | 3.2:RolePolicies 三策冻结;3.4:RoleComponent 逐钩子生命周期+四 facet+Directory 登记/参与者维护+两个合成事件第 5 分量口径+RoleReward.onCustom+RoleBridge.attach(`component/role/**`);3.5:RoleEventSynth.trustedKill 四条件与采样差分、Directory.dispatch 官方死亡信号路由→RoleComponent#onRoleDeath 击杀合成、DebugCommand 经 lookup 路由(LOWEST 死亡探测已删,改吃官方事件载荷) | — | — | 4.2:ObjectiveComponent(boss 进度/炸弹目标注册面);4.4:StatsComponent 直查 |

**6.4 与 7.3 说明**:两阶段在骨架中**无 TODO 载体**(全仓 grep 无 `TODO(6.4)`/`TODO(7.3)`),属施工期验收动作而非桩内标记——
6.4 = 引擎零改动验收(engine/** 与既有 com.hydrokarbs.component/** PR diff 为空留痕+差异表逐行回归);
7.3 = 全量验收收尾(编号空间见方案「TODO 风格」约定,验收项见 16-路线图与验收 ★5/★18/★21/★23/★31/★32)。

---

## ⑤ 构建验证方法与后续接入步骤

### 5.1 构建验证方法

- **编译门控命令**(由主脚本统一执行,子代理不跑 gradle;本文撰写者本轮**未运行** gradle,门控交主脚本):
  ```
  cmd /c gradlew.bat build --no-daemon
  ```
- Gradle Wrapper 已就位(工作区根实测:`gradlew`/`gradlew.bat`/`gradle/wrapper/gradle-wrapper.jar`+`gradle-wrapper.properties`),
  工具链 Java 21、run-paper 1.21.11(`build.gradle.kts:28,36`)。
- **编译前提三件**(任一缺失即构建失败,需先行就位):
  1. `mavenLocal()` 可解析 `com.shadowHunterRolesPlugin:ShadowHunterRolesPlugin:1.0.0`(`build.gradle.kts:8,24`);
  2. `../Hydrokarbs/build/libs/Hydrokarbs-3.0.0-all.jar` 存在(`build.gradle.kts:21`);
  3. `../ZeoliteLib/build/libs/ZeoliteLib-1.0-SNAPSHOT.jar` 存在(`build.gradle.kts:18`)。
- **骨架验收口径**:编译通过 + 每文件中文 Javadoc(职责+来源)与 TODO 齐备 + 无任何玩法逻辑实现(体内仅 TODO 与空方法/record 声明)。
- **构建纪律**(随骨架生效):宿主 jar 禁 META-INF/services(SPI 通道退场);禁 shade,三姊妹 jar 全部 compileOnly 不进产物;
  `processResources` 对 plugin.yml 做 `${version}` expand(`build.gradle.kts:40-45`)。

### 5.2 后续接入步骤(按子阶段序,内容出自方案 nextSteps 与各文件 TODO)

1. **2.5 大厅+idle 最小闭环**:填主类装配序与 onDisable;大厅七件+map 装配四件+idle 资源与 config/轮换兜底模板落位;
   worlds/testmap 随测试资源打包。DoD=runServer 闭环 ≥10 次:入厅→凑人数→投票→ready 自动开局 idlepad@testmap→
   `/shdf debug session`→`/shdf leave`→~10s 空场解散回大厅→切 idlepark 往返 ≥10 次→拔 Hydrokarbs 重启拒载不崩服;
   IdlePadRoundTripTest(100 次往返)。
2. **3.1 RoleFacade 防腐层**:15 映射方法(原 14 方法含备用 lastDamager(Player)+事件订阅面 subscribeRoleEvents/RoleSignal/RoleEventHandle)+懒解析+缺席降级+订阅重订;ServiceWatchListener 接线;
   RoleDescriptor/RoleFaction 落位;补 testImplementation 角色坐标(compileOnly 不进测试类路径)。
3. **3.2 类型化访问与策略数据件**:RoleStateAccess 七具名动词+下标重载、RoleCatalog、RolePolicy 八字段逐字(反射断言)、
   RolePolicies 三策;离线测试族(RoleFacadeContractTest 等)全部假 RoleAPI 打桩。
4. **3.4 RoleComponent 四 facet**:逐钩子生命周期(awake=fetch+Directory 登记+订阅实例事件/start 空/update 采样门/
   onEvent 死亡善后+奖励/onHandoff/stop 全员清角幂等);RoleComponentDirectory 参与者维护;两合成事件第 5 分量+of 工厂。
5. **3.5 监听器与击杀归属**:官方事件订阅接线(L0 subscribeRoleEvents→Directory.dispatch→RoleComponent#onRoleDeath,
   原 LOWEST 死亡探测已删)、StatsSignalListener 记账、
   DebugCommand 读口、RoleEventSynth.trustedKill 四条件;第五门 BooleanSupplier 换真实现。出口四闸门(真机):
   /sm(=/shdf)debug role info 可打印、实例内击杀合成 RoleKillEvent(箭矢反例不产)、stop 全员清角每人恰一次、
   拔角色插件重启大厅照常且需角色玩法开局被拒。
6. **5.1 TDM 模式工厂**:Builder 声明序冻结(TdmDeclarationTest=§13.1-8 模板);三判定器;DisplayComponent+构建函数方法引用;
   display 配置块生效;`mode_config/<tdm 模式>/` 配置目录与 TDM 专用配置 Parser(SpecificConfigParsers 注册)。
7. **5.2 TDM 端到端**:2 人实测(开局托管→击杀计分→阈值→WIN→局末散场回大厅战报 ★18);MatchEndReturn 局末即散场(本插件无续局)。
8. **6.1 炸弹攻防**:回合制与目标(rounds 挂框架 RoundComponent);`mode_config/<炸弹模式>/` 与炸弹专用配置 Parser(bomb-site 校验开局前响亮)。
9. **6.2 炸弹交互与经济**:PlantInteraction(InteractionComponent 回填)、EconomyComponent 可选挂载、半场换边;
   回大厅战报(★18)。
10. **6.3 首领战**:BossComponent 生成/血线轮询、BossPhaseGate、三判定器、Aggro 可选件;`mode_config/bosswar/` 与
   BosswarSpecificConfig.Parser 注册(SpecificConfigParsers,出生点等配置开局前解析、模式组件读会话 MatchConfigComponent)。
11. **6.4 引擎零改动验收**:engine/** 与既有 com.hydrokarbs.component/** PR diff 为空留痕;每次模式接入复测。
12. **7.3 全量验收收尾**:对照 16-路线图与验收(★5/★18/★21/★23/★31/★32)逐项核销。

### 5.3 框架线阶段4 交付后的全局回填点(回填前一律 TODO 占位,禁止硬引/禁止宿主同名自建)

| 待交付件 | 现状(方案本轮核实) | 受影响宿主文件 |
|---|---|---|
| TeamComponent / JoinControlComponent / VoteComponent / ScoreComponent / StatsComponent | unzip grep NO_MATCH | TeamDeathmatchModes、BombDefusalModes、BossRaidModes、RoleComponent.Spec(requiresOptional)、StatsSignalListener |
| InteractionComponent | NO_INTERACTION(框架 4.2 交付) | PlantInteraction、SessionSignalListeners.InteractionListener |
| RoundComponent / EconomyComponent / ScoreObjective | 框架 4.1 落 component/score/ | BombDefusalModes、RoundLimitEndCondition(回合结果入口)、TdmRules |
| SessionIngestListener | 框架已有件(jar 已核) | **永不回填**,宿主零接触 |

若 6.1 时框架 RoundComponent 无回合结果入口,按 6.1 文档:66 escalate,不得自改 component/**;
若框架线后续开放『玩家→实例→组件』查询 API,回方案评估替换 RoleComponentDirectory 路由通道。

---

*本文只描述骨架现状与接线图;各桩的逐条施工口径以文件内 TODO 注释及其引用的设计文档篇目为准。*
