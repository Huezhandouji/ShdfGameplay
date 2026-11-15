package com.shdfGameplay.bridge.role;

import java.util.OptionalInt;
import java.util.UUID;

/**
 * 角色数据的类型化读写:把 executeComponentOperation 的字符串协议包成 energy/sante/buffs 七具名动词与下标重载;
 * 「读不到」用 OptionalInt 显式建模,绝不抛、不回哨兵 -1。纪律:所有方法只准主线程调用(会触碰玩家状态,08 §8.3);
 * 协议串(含 "#" 下标)只住本层私有常量;写型口径=operate 回非 null(含 "")即成功。
 */
public final class RoleStateAccess {

    /** 桥接(构造注入,08 §8.3)。 */
    private final RoleFacade role;

    /** 组件 id 常量(08 §8.3;id 清单出处框架侧 00-假设与体检 §0.1"角色数据读写"行:energy/sante/buffs/vitals/hotbarRender/tasks/faction)。 */
    private static final String ENERGY = "energy";
    private static final String SANTE = "sante";
    private static final String BUFFS = "buffs";

    /** 动词 token(08 §8.3 逐字:读 current;能量写 set/add/consume v,理智写 set/gain v)。 */
    private static final String VERB_CURRENT = "current";
    private static final String VERB_SET = "set";
    private static final String VERB_ADD = "add";
    private static final String VERB_CONSUME = "consume";
    private static final String VERB_GAIN = "gain";

    /** 构造注入桥接(08 §8.3)。 */
    public RoleStateAccess(RoleFacade role) {
        this.role = role;
    }

    // ── 能量面(08 §8.3 逐条)──────────────────────────────────────

    /** ★主线程。读当前能量(payload current,08 §8.3);读不到 ⇒ empty。 */
    public OptionalInt energy(UUID uuid) {
        return readInt(uuid, ENERGY, VERB_CURRENT);
    }

    /** ★主线程。读第 instanceIndex 份实例的能量(组件 id 拼为 energy#N;下标拼接只住本层,§14.14)。 */
    public OptionalInt energy(UUID uuid, int instanceIndex) {
        return readInt(uuid, indexedId(ENERGY, instanceIndex), VERB_CURRENT);
    }

    /** ★主线程。设置能量(payload set v,08 §8.3);经 readInt 回写后规范化值。 */
    public OptionalInt setEnergy(UUID uuid, int value) {
        return readInt(uuid, ENERGY, VERB_SET + " " + value);
    }

    /** ★主线程。设置第 instanceIndex 份实例的能量。 */
    public OptionalInt setEnergy(UUID uuid, int value, int instanceIndex) {
        return readInt(uuid, indexedId(ENERGY, instanceIndex), VERB_SET + " " + value);
    }

    /** ★主线程。增加能量(payload add v,08 §8.3);经 readInt 回写后规范化值。 */
    public OptionalInt addEnergy(UUID uuid, int value) {
        return readInt(uuid, ENERGY, VERB_ADD + " " + value);
    }

    /** ★主线程。增加第 instanceIndex 份实例的能量。 */
    public OptionalInt addEnergy(UUID uuid, int value, int instanceIndex) {
        return readInt(uuid, indexedId(ENERGY, instanceIndex), VERB_ADD + " " + value);
    }

    /** ★主线程。消耗能量(payload consume v,08 §8.3);回 true 只代表「角色系统认识这条操作」,不代表余额真的扣够(08 §8.7,桥接不猜测)。 */
    public boolean consumeEnergy(UUID uuid, int value) {
        return operateAcked(uuid, ENERGY, VERB_CONSUME + " " + value);
    }

    /** ★主线程。消耗第 instanceIndex 份实例的能量。 */
    public boolean consumeEnergy(UUID uuid, int value, int instanceIndex) {
        return operateAcked(uuid, indexedId(ENERGY, instanceIndex), VERB_CONSUME + " " + value);
    }

    // ── 理智面(与能量面对称,08 §8.3)─────────────────────────────

    /** ★主线程。读当前理智(payload current,08 §8.3);读不到 ⇒ empty。 */
    public OptionalInt sanTe(UUID uuid) {
        return readInt(uuid, SANTE, VERB_CURRENT);
    }

    /** ★主线程。读第 instanceIndex 份实例的理智(组件 id 拼为 sante#N;下标拼接只住本层,§14.14)。 */
    public OptionalInt sanTe(UUID uuid, int instanceIndex) {
        return readInt(uuid, indexedId(SANTE, instanceIndex), VERB_CURRENT);
    }

    /** ★主线程。设置理智(payload set v,08 §8.3);经 readInt 回写后规范化值。 */
    public OptionalInt setSanTe(UUID uuid, int value) {
        return readInt(uuid, SANTE, VERB_SET + " " + value);
    }

    /** ★主线程。设置第 instanceIndex 份实例的理智。 */
    public OptionalInt setSanTe(UUID uuid, int value, int instanceIndex) {
        return readInt(uuid, indexedId(SANTE, instanceIndex), VERB_SET + " " + value);
    }

    /** ★主线程。获得理智(payload gain v,08 §8.3);经 readInt 回写后规范化值。 */
    public OptionalInt gainSanTe(UUID uuid, int value) {
        return readInt(uuid, SANTE, VERB_GAIN + " " + value);
    }

    /** ★主线程。获得第 instanceIndex 份实例的理智。 */
    public OptionalInt gainSanTe(UUID uuid, int value, int instanceIndex) {
        return readInt(uuid, indexedId(SANTE, instanceIndex), VERB_GAIN + " " + value);
    }

    // ── buff 面(动词表以角色系统 BuffComponent#onOperationCommand javadoc 为准)──

    /** ★主线程。施加 buff(走 buffs 组件动词面:add &lt;buffType&gt; &lt;ticks&gt;,回写后剩余刻 —— 出自 角色插件 BuffComponent#onOperationCommand javadoc);buffType 为 null ⇒ false 且不发起操作。 */
    public boolean applyBuff(UUID uuid, String buffType, int ticks) {
        return writeBuff(uuid, BUFFS, buffType, ticks);
    }

    /** ★主线程。对第 instanceIndex 份实例施加 buff。 */
    public boolean applyBuff(UUID uuid, String buffType, int ticks, int instanceIndex) {
        return writeBuff(uuid, indexedId(BUFFS, instanceIndex), buffType, ticks);
    }

    /** 下标消歧拼接:componentId#N(# 后为 0 基下标 —— 出自 角色插件 RoleAPI#executeComponentOperation javadoc;同 id 命中多份而未给下标则角色系统拒并回 null,绝不静默取第一份,§14.14;拼接只住本层,上层禁止自己拼 #)。 */
    private static String indexedId(String componentId, int instanceIndex) {
        return componentId + "#" + instanceIndex;
    }

    /** 写型成功口径:operate 回非 null(含 "" = 已识别无回值)即 true、null 即 false(08 §8.3 返回值三态)。 */
    private boolean operateAcked(UUID uuid, String componentId, String payload) {
        return role.operate(uuid, componentId, payload) != null;
    }

    /** 施加 buff 核心:拼「add &lt;buffType&gt; &lt;ticks&gt;」并按写型口径回成败;buffType 为 null ⇒ 回 false 且不发起操作(防御,全类不抛)。 */
    private boolean writeBuff(UUID uuid, String componentId, String buffType, int ticks) {
        if (buffType == null) {
            return false;                          // null 无名可拼即拒绝且不发起操作(同 3.1 setPlayerFaction 对 OTHER 的防御)
        }
        return operateAcked(uuid, componentId, VERB_ADD + " " + buffType + " " + ticks);
    }

    /** 读整数原语(08 §8.3 readInt 逐字:null/空串/NumberFormatException ⇒ OptionalInt.empty,不抛不给上层 NPE 机会)。 */
    private OptionalInt readInt(UUID uuid, String componentId, String payload) {
        String raw = role.operate(uuid, componentId, payload);
        if (raw == null || raw.isEmpty()) {
            return OptionalInt.empty();            // null(未识别/拒绝/内部异常被吞,三重含义 §14.14)与 ""(已识别无回值)都读不到值
        }
        try {
            return OptionalInt.of(Integer.parseInt(raw.trim()));
        } catch (NumberFormatException notAnInt) {
            return OptionalInt.empty();            // 非整数回值(协议漂移)吞掉 ⇒ empty,绝不抛
        }
    }

    /** 采样差分帧(08 §8.4 diffSample 载体;形状在桥接、状态持有者在 component/role 的组件,§9.2/§14.26;buffs 不入帧)。 */
    public record RoleSnapshot(OptionalInt energy, OptionalInt sanTe) {
    }
}
