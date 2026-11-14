# bridge/role — 角色系统防腐层 Javadoc 归档

> 源码包:`src/main/java/com/shdfGameplay/bridge/role/`
> 保留定位:防腐层唯一入口,角色插件类型不泄漏出本包。

---

## RoleFacade

**源文件**:`src/main/java/com/shdfGameplay/bridge/role/RoleFacade.java`
**保留**:唯一 import 角色插件之处、1:1 覆盖 RoleAPI 冻结面、四条纪律压缩版。

**归档(原 javadoc 详细段落)**:

> 纪律(08 §8.2 类头):
> ①签名只用 JDK/Bukkit/Adventure 类型,绝不泄漏 RoleAPI 的类型(如 RoleInfo 与 api/event 事件类——
> 事件流经 `subscribeRoleEvents` 翻译为 `RoleSignal` 宿主词汇),角色系统升级换签名时只改本文件;
> ②全部方法只准主线程调用(懒解析不引入异步,08 §8.8 尾注;角色插件事件同样主线程同步分发);
> ③懒解析 null 也缓存,失效只经 `invalidate()`;
> ④插件缺席时读方法回空值(空集合/null/UNKNOWN/AIR)、写方法回 false,全类不抛
> (订阅面缺席=登记在册不抛,恢复后自动补订)。
>
> 来源:08-角色桥接 §8.1/§8.2/§8.8/§8.10;角色插件 RoleAPI#subscribeEvents 事件订阅面;
> 施工计划 3.1-RoleFacade防腐层;README 对齐注①(宿主独立顶级包映射)适用。

补充细节:公开面原为「15 方法 = 原 14 方法(含备用 lastDamager)+ 事件订阅面 subscribeEvents」。

---

## RoleSignal

**源文件**:`src/main/java/com/shdfGameplay/bridge/role/RoleSignal.java`
**保留**:翻译载体定位、见序合约要点、只读+主线程。

**归档(原 javadoc 详细段落)**:

> 见序合约(逐字来自角色插件事件 javadoc,下游可依赖):
> DEATH=持角玩家死亡,roleId 恒为「死亡那一刻」所持角色(角色插件赶在清角之前发布),
> 随后必跟一条 CLEAR(死者);换角=CLEAR(旧角)+SET(新角)按到达顺序;
> 掉线/组件故障隔离/角色插件停用回收只发 CLEAR。均为只读信号(无取消位),主线程同步送达。
>
> 来源:角色插件 RoleAPI#subscribeEvents 事件订阅面;08-角色桥接 §8.1/§8.2/§8.8;README 对齐注①。

补充细节:对应角色插件正式事件流 RoleSetEvent/RoleClearEvent/RoleDeathEvent;下游消费方为 component/role 与 L0。

---

## RoleStateAccess

**源文件**:`src/main/java/com/shdfGameplay/bridge/role/RoleStateAccess.java`
**保留**:字符串协议类型化包装、OptionalInt 建模「读不到」、三条纪律压缩版。

**归档(原 javadoc 详细段落)**:

> 把「读不到」用 OptionalInt 显式建模(null 三重含义压扁必炸,§14.14)。
> 纪律:①所有方法只准主线程调用(会触碰玩家状态,08 §8.3 类头);
> ②组件 id 与动词 token 只住本层私有常量,上层禁止拼协议串(含 "#" 下标);
> ③写型口径=operate 回非 null(含 "")即成功、null 即失败(""=已识别无回值也是成功)。
>
> 来源:08-角色桥接 §8.3/§8.4;施工计划 3.2-类型化访问与策略数据件 步骤1/步骤5;README 对齐注①适用。

---

## RoleEventHandle

**源文件**:`src/main/java/com/shdfGameplay/bridge/role/RoleEventHandle.java`
**保留**:句柄定位、令牌不出防腐层、自动重订语义。

**归档(原 javadoc 详细段落)**:

> 生命周期语义(深模块口径,调用方只需 onEnable 订阅一次、onDisable close):
> 角色插件中途装/卸(reload)时订阅不跨其 disable→enable 存活——RoleFacade.invalidate()
> 会退订旧令牌并对在册 handler 自动重订,调用方持同一句柄无需重订。
>
> 来源:角色插件 RoleAPI#subscribeEvents 生命周期合约;08-角色桥接 §8.8;README 对齐注①。

补充细节:包住的角色插件订阅令牌类型为 RoleEventSubscription(不出防腐层,08 §8.2 纪律①)。

---

## RolePolicy

**源文件**:`src/main/java/com/shdfGameplay/bridge/role/RolePolicy.java`
**保留**:纯数据定位、声明不是命令、解释执行在 Loadout facet。

**归档(来源行)**:

> 来源:08-角色桥接 §8.6;施工计划 3.2 步骤3;README 对齐注①适用。

---

## RoleFaction

**源文件**:`src/main/java/com/shdfGameplay/bridge/role/RoleFaction.java`
**保留**:枚举四值、读写方向映射口径、core.Faction 不参与。

**归档(来源行)**:

> 来源:08-角色桥接 §8.2 映射口径/§8.10(二);施工计划 3.1 步骤2;README 对齐注①适用。

---

## RoleDescriptor

**源文件**:`src/main/java/com/shdfGameplay/bridge/role/RoleDescriptor.java`
**保留**:自家值对象、恰四字段不含阵营。

**归档(原 javadoc 详细段落与来源)**:

> RoleInfo 只有这四个成分,角色目录无阵营来源——要"某角色声明的默认阵营"
> 只能从正持该角色的玩家实例读 declaredFaction。
>
> 来源:08-角色桥接 §8.2(自家值对象);施工计划 3.1 步骤3;README 对齐注①适用。

---

## RoleCatalog

**源文件**:`src/main/java/com/shdfGameplay/bridge/role/RoleCatalog.java`
**保留**:只读快照、TTL 语义、永不缓存当前角色/能量。

**归档(原 javadoc 详细段落与来源)**:

> 玩家当前角色/能量永不缓存、永远现问(角色系统权威,08 §8.5 表第 1 行)。
>
> 来源:08-角色桥接 §8.5;施工计划 3.2 步骤2;README 对齐注①适用。

---

## RoleBridge

**源文件**:`src/main/java/com/shdfGameplay/bridge/role/RoleBridge.java`
**保留**:进程级持有器、唯一合规注入点。

**归档(原 javadoc 详细段落与来源)**:

> svc 三成员封闭(框架侧 14-陷阱清单 §14.25)与 ★23 分层夹逼下的唯一合规注入点。
>
> 来源:施工计划 3.4-RoleComponent四facet 步骤1/§14.25 待裁决;README 对齐注①(包映射)适用。
