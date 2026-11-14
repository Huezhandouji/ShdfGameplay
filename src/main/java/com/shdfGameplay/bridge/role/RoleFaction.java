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
        // TODO(3.1): parse 口径(认不出非空→OTHER、null/空→UNKNOWN、SHADOW/HUNTER 大小写敏感);写方向 OTHER 无对应阵营名→拒绝且不发起操作(3.1 步骤2 wireName 口径,施工时落) —— 出自 08-角色桥接 §8.2/§8.10(二)/施工计划3.1 步骤2
        return UNKNOWN;
    }
}
