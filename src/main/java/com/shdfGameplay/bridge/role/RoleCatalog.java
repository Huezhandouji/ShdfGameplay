package com.shdfGameplay.bridge.role;

import java.util.List;
import java.util.Set;

/**
 * 角色目录只读快照:awake 期解析缓存,显式 TTL(默认 30s,用到快照处须写明「数据可能滞后 ≤ TTL」);
 * 无写口,刷新=重新 fetch 整体替换不做增量;玩家当前角色/能量永不缓存、永远现问(角色系统权威)。
 */
public record RoleCatalog(
        Set<String> roleIds,
        List<RoleDescriptor> roles,
        long fetchedAtMillis
) {

    /** 默认 TTL 30s:文档化承诺而非摆设;消费方(3.4 awake)可传参覆写。 */
    public static final long DEFAULT_TTL_MILLIS = 30_000L;

    /**
     * ★主线程(内部调 RoleFacade,Facade 全主线程,§8.8/§14.1)。经 role.knownRoleIds() + role.catalog() 取目录,
     * 并对入参做防御性拷贝(Set.copyOf/List.copyOf,改原集合不影响快照);取时刻记在发起取数之前(保守:取数窗口计入 TTL)。
     * role 为 null 或角色插件缺席 ⇒ 空目录快照,不抛(缺席读空值口径)。
     * —— 出自 08-角色桥接 §8.5/§8.10(一)/施工计划3.2 步骤2
     */
    public static RoleCatalog fetch(RoleFacade role) {
        long fetchedAt = System.currentTimeMillis();
        if (role == null) {
            return new RoleCatalog(Set.of(), List.of(), fetchedAt);    // 防御:null 视同缺席,回空目录快照(全类不抛)
        }
        return new RoleCatalog(
                Set.copyOf(role.knownRoleIds()),
                List.copyOf(role.catalog()),
                fetchedAt);
    }

    /** 纯函数判过期、无线程要求(nowMillis 距 fetchedAtMillis 严格超过 ttlMillis 即过期,恰等于不算)—— 出自 08-角色桥接 §8.5/施工计划3.2 步骤2。 */
    public boolean isExpired(long ttlMillis, long nowMillis) {
        return nowMillis - fetchedAtMillis > ttlMillis;
    }
}
