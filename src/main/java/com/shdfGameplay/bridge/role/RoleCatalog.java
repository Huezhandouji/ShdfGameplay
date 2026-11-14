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

    /** 经 role.knownRoleIds() + role.catalog() 取,并对入参做防御性拷贝(Set.copyOf/List.copyOf)。 */
    public static RoleCatalog fetch(RoleFacade role) {
        // TODO(3.2): fetch 经 knownRoleIds()+catalog() 取并入参防御性拷贝(改原集合不影响快照);只读快照无写口,刷新=重新 fetch 整体替换 —— 出自 08-角色桥接 §8.5/施工计划3.2 步骤2
        return new RoleCatalog(Set.of(), List.of(), 0L);
    }

    /** 纯函数判过期(nowMillis 距 fetchedAtMillis 超过 ttlMillis 即过期)。 */
    public boolean isExpired(long ttlMillis, long nowMillis) {
        return false;
    }
}
