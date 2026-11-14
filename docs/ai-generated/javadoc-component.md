# component — 会话组件层 Javadoc 归档

> 源码包:`src/main/java/com/shdfGameplay/component/`、`src/main/java/com/shdfGameplay/component/role/`
> component/role 为 ★23 分层豁免包(允许触碰 bridge/role)。

---

## RoleComponentDirectory

**源文件**:`src/main/java/com/shdfGameplay/component/role/RoleComponentDirectory.java`
**保留**:登记簿定位、玩家→组件路由。

**归档(原 javadoc 详细段落与来源)**:

> 弥补 MiniGameApi 无"玩家→实例→组件"通道(sessionOf 仅回扁平 SessionInfo,javap 已核)。
>
> 来源:方案「玩家→组件路由裁决」;08-角色桥接 §8.9;09-角色组件层 §9.3;
> 角色插件 RoleAPI 事件订阅面(3.5 步骤1 改造);README 对齐注⑤。

---

## RoleEventSynth

**源文件**:`src/main/java/com/shdfGameplay/component/role/RoleEventSynth.java`
**保留**:无状态协作者定位、采样差分保留原因、死亡合成已删。

**归档(原 javadoc 详细段落与来源)**:

> ★ 原"死亡合成(死前读序)"已删:角色插件正式事件流自带死亡事实与死时所持角色
> (RoleDeathEvent 在清角之前发布,载荷即 roleId),LOWEST 抢读时序不再存在;
> 击杀归因与 RoleKillEvent 合成入口移至 RoleComponent#onRoleDeath(官方死亡信号唯一落点)。
> 采样差分保留:官方事件流只有生命周期三事件(SET/CLEAR/DEATH),energy/sanTe 数值变更无正式通知面
> (角色插件侧能量变更无转发入口),差分采样仍是唯一手段。
>
> 来源:08-角色桥接 §8.4;09-角色组件层 §9.5;施工计划/阶段3 3.4-RoleComponent四facet 步骤4、
> 3.5-监听器与击杀归属 步骤2 改造(角色插件 RoleAPI 事件订阅面);README 对齐注①⑤。

---

## MatchEndReturn

**源文件**:`src/main/java/com/shdfGameplay/component/MatchEndReturn.java`
**保留**:散场引导组件职责。

**归档(来源行)**:

> 来源:04 §4.4;2.5 步骤5(:39/:53 阶段5 随 TDM 落地)。
