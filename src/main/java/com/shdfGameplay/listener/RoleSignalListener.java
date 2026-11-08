package com.shdfGameplay.listener;

import com.shdfGameplay.component.role.RoleComponent;
import com.shdfGameplay.component.role.RoleComponentDirectory;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.server.ServiceRegisterEvent;
import org.bukkit.event.server.ServiceUnregisterEvent;

/**
 * LOWEST 死亡信号监听器(只路由不合成)+ 服务动态感知监听(内嵌 ServiceWatchListener):死亡经 RoleComponentDirectory.lookup 路由给所在实例 RoleComponent.onVanillaDeath;服务注册/注销回调驱动 RoleFacade 懒解析缓存失效。
 * 来源:08-角色桥接 §8.9 树行+§8.8 动态感知行(:302/:313);施工计划/阶段3 3.5-监听器与击杀归属 步骤1;12-代码骨架 §12.1 listener/ 唯一允许 @EventHandler 的层(:202);README 对齐注①⑤。
 */
public final class RoleSignalListener implements Listener {

    /**
     * 原版死亡信号:只路由,不合成(合成在 RoleComponent/RoleEventSynth)。
     * <p>★ 必须 LOWEST(正确性的一部分,不是风格偏好)。因果链:角色系统的死亡/掉线清角在默认 NORMAL
     * (PlayerListener.java:21/:39)→ NORMAL 之后读 roleIdOf(受害者)拿到的恒是 null(角色已被清)→
     * 想读"死前是谁"只能在它之前。后人不得"顺手统一优先级"。
     * <p>PlayerDeathEvent 不可取消(不实现 Cancellable),ignoreCancelled 在此是 no-op——保留 §12.2 原样写法,但不得误以为它过滤了什么。
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onDeath(PlayerDeathEvent event) {
        // TODO(3.5): 只路由:RoleComponentDirectory.lookup(event.getEntity().getUniqueId())→ifPresent(component -> component.onVanillaDeath(event));会话外(路人)死亡或 lookup 无果(当前实例无 RoleComponent,如未挂角色组件的模式)时静默返回不产实例事件;主类 onEnable 一次性注册本监听器,不随模式切换反复 registerEvents —— 出自 3.5-监听器与击杀归属 步骤1(:35-40)与关键实现要点"击杀合成只发生在会话实例内"行;08-角色桥接 §8.9。
    }

    /**
     * 服务动态感知监听(内嵌件):角色插件中途装/卸(PlugMan 类)时失效 RoleFacade 懒解析缓存,使缺席读空/写 false 可恢复。
     * 回调经构造注入的 Runnable 进来(lambda 体住 L0 主类:() -> RoleBridge.facade().invalidate()),本类不 import 桥接类型。
     */
    public static final class ServiceWatchListener implements Listener {

        private final Runnable onInvalidate;

        public ServiceWatchListener(Runnable onInvalidate) {
            this.onInvalidate = onInvalidate;
        }

        // TODO(3.1): 本内嵌件随阶段3.1 先行接线(早于死亡路由施工):主类 onEnable 注册并接 () -> RoleBridge.facade().invalidate()(lambda 体住 L0);只路由 invalidate 回调不做别的事 —— 出自 08-角色桥接 §8.8 动态感知行(:313);施工计划/阶段3 3.1-RoleFacade防腐层。

        @EventHandler
        public void onServiceRegister(ServiceRegisterEvent event) {
            // onInvalidate.run()——角色插件注册即失效缓存(只路由回调,不做服务定位) —— 出自 08-角色桥接 §8.8 动态感知行。
        }

        @EventHandler
        public void onServiceUnregister(ServiceUnregisterEvent event) {
            // onInvalidate.run()——角色插件注销即失效缓存 —— 出自 08-角色桥接 §8.8 动态感知行。
        }
    }
}
