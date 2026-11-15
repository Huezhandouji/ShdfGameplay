package com.shdfGameplay.bridge.role;

/**
 * 阵营枚举:SHADOW/HUNTER/UNKNOWN/OTHER(OTHER 兜住角色系统未来的新阵营名);
 * 读方向=组件操作面返回字符串经 parse 映射,写方向=转阵营名(OTHER 无对应名即拒绝且不发起操作);
 * 外部 core.Faction 枚举不参与(不泄漏其类型,只认字符串)。
 */
public enum RoleFaction {

    SHADOW,
    HUNTER,
    UNKNOWN,
    OTHER;

    /**
     * 读方向映射:"SHADOW"/"HUNTER" 大小写敏感同名映射(角色系统 core.Faction 枚举名原样);
     * 认不出的非空串 ⇒ OTHER;null/空串 ⇒ UNKNOWN。
     */
    public static RoleFaction parse(String raw) {
        if (raw == null || raw.isEmpty()) {
            return UNKNOWN;                    // null/空串 ⇒ UNKNOWN(无实例/读不到口径)—— 出自 08-角色桥接 §8.2/施工计划3.1 步骤2
        }
        switch (raw) {
            case "SHADOW":                     // 大小写敏感同名(角色系统 FactionComponent:183 set 只接三名原样)—— 出自 08-角色桥接 §8.10(二)/施工计划3.1 步骤2
                return SHADOW;
            case "HUNTER":
                return HUNTER;
            default:
                return OTHER;                  // 认不出的非空串 ⇒ OTHER(兜未来新阵营名)—— 出自 08-角色桥接 §8.2/施工计划3.1 步骤2
        }
    }

    /**
     * 写方向映射:转组件操作面的阵营名——OTHER 无对应阵营名 ⇒ 返回 null
     * (调用方据此拒绝且不发起操作,08 §8.10(二) set 行);其余回自己的 name()。
     */
    public String wireName() {
        // 出自 08-角色桥接 §8.2/§8.10(二)/施工计划3.1 步骤2
        return this == OTHER ? null : name();
    }
}
