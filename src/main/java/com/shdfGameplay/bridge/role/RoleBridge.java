package com.shdfGameplay.bridge.role;

/**
 * 桥接进程级持有器:svc 三成员封闭与 ★23 分层夹逼下的唯一合规注入点
 * ——组件(component/role 为 ★23 豁免包)经它取桥接,不经 Services、不经 Spec 构造参数。
 */
public final class RoleBridge {

    /** 进程级唯一实例(显式 attach 保持单一实例;volatile 仅为防御性——装配与消费均在主线程)。 */
    private static volatile RoleFacade instance;

    private RoleBridge() {
    }

    /** L0 显式装配入口(主类 onEnable 调,推荐——保持单一实例)。 */
    public static void attach(RoleFacade facade) {
        instance = facade;
    }

    /** 取进程级桥接;未 attach 时懒构造默认 RoleFacade(其只依赖 Bukkit 静态门,无插件句柄需求)。 */
    public static RoleFacade facade() {
        if (instance == null) {
            instance = new RoleFacade();
        }
        return instance;
    }
}
