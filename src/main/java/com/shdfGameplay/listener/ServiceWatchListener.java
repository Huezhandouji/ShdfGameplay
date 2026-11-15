package com.shdfGameplay.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServiceRegisterEvent;
import org.bukkit.event.server.ServiceUnregisterEvent;

/**
 * 服务动态感知监听器:角色插件中途装/卸(PlugMan 类)时回调失效 RoleFacade 懒解析缓存
 * (含事件订阅重订),使缺席读空/写 false 与订阅断流可恢复。
 * 回调经构造注入的 {@code Runnable} 进来,本类不 import 桥接类型。
 */
public final class ServiceWatchListener implements Listener {

    private final Runnable onInvalidate;

    public ServiceWatchListener(Runnable onInvalidate) {
        this.onInvalidate = onInvalidate;
    }

    @EventHandler
    public void onServiceRegister(ServiceRegisterEvent event) {
        onInvalidate.run();    // 角色插件注册即失效缓存并触发懒解析重订(只路由回调,不做服务定位) —— 出自 08-角色桥接 §8.8 动态感知行。
    }

    @EventHandler
    public void onServiceUnregister(ServiceUnregisterEvent event) {
        onInvalidate.run();    // 角色插件注销即失效缓存(旧订阅令牌随角色插件 onDisable 一并作废,退订无害) —— 出自 08-角色桥接 §8.8 动态感知行。
    }
}
