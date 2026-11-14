# listener — 事件路由层 Javadoc 归档

> 源码包:`src/main/java/com/shdfGameplay/listener/`
> 本包为全工程唯一允许 @EventHandler 的层(12-代码骨架.md:202 与 3.5:108)。

---

## ServiceWatchListener

**源文件**:`src/main/java/com/shdfGameplay/listener/ServiceWatchListener.java`
**保留**:动态感知定位、回调注入、不 import 桥接类型。

**归档(原 javadoc 详细段落)**:

> 沿革:本类由原 RoleSignalListener(内嵌 ServiceWatchListener)扁平化而来。
>
> ★ 原宿主侧死亡信号探测(LOWEST 抢读 PlayerDeathEvent——赶在角色系统 NORMAL 清角前读"死时是谁")已删:
> 角色插件正式事件流自带该事实(RoleDeathEvent 在清角前发布且载荷即死时所持角色),
> 死亡等角色信号改经 RoleFacade#subscribeRoleEvents 订阅→RoleComponentDirectory.dispatch 路由,
> 接线住 L0 主类,本类不再挂任何 PlayerDeathEvent。
>
> 回调细节:lambda 体住 L0 主类(`() -> RoleBridge.facade().invalidate()`)。
>
> 来源:08-角色桥接 §8.8 动态感知行(:313);施工计划/阶段3 3.1-RoleFacade防腐层;
> 3.5-监听器与击杀归属 步骤1 改造(角色插件 RoleAPI 事件订阅面);
> 12-代码骨架 §12.1 listener/ 唯一允许 @EventHandler 的层(:202);README 对齐注①⑤。

---

## LobbyEventsListener

**源文件**:`src/main/java/com/shdfGameplay/listener/LobbyEventsListener.java`
**保留**:三事件路由分工、唯一 @EventHandler 层。

**归档(原 javadoc 详细段落与来源)**:

> 沿革:本类为方案新增裁决。
>
> 来源:04 §4.2(:55-58);2.5 步骤2;12-代码骨架.md:202 与 3.5:108
> (listener/ 唯一 @EventHandler 层);本方案新增裁决。
