package com.shdfGameplay.bridge.role;

/**
 * 角色事件订阅句柄:RoleFacade#subscribeRoleEvents 的返回物,包住角色插件订阅令牌(令牌类型不出防腐层),
 * {@link #close()} 幂等退订。角色插件中途装/卸时订阅不跨其 disable→enable 存活——invalidate() 自动重订,
 * 调用方持同一句柄无需重订(onEnable 订阅一次、onDisable close)。
 */
public final class RoleEventHandle implements AutoCloseable {

    /** 所属防腐层(close 经它回移除在册与退订;包私有构造,只由 RoleFacade#subscribeRoleEvents 造)。 */
    private final RoleFacade owner;

    RoleEventHandle(RoleFacade owner) {
        this.owner = owner;
    }

    // TODO(3.1): close=从 owner 在册表移除+退订当前令牌(幂等,重复 close 无害;令牌可能已随角色插件 reload 作废,退订作废令牌无害) —— 出自 角色插件 RoleAPI#subscribeEvents javadoc;08-角色桥接 §8.8。

    /** 退订并从 RoleFacade 在册表移除;幂等。 */
    @Override
    public void close() {
        // 3.1:见类头 TODO——移除在册+退订令牌,无其他状态。
    }
}
