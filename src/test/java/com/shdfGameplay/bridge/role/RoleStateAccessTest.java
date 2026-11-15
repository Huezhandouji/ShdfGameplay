package com.shdfGameplay.bridge.role;

import org.junit.jupiter.api.Test;

import java.util.OptionalInt;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * RoleStateAccess 类型化访问契约测试:照施工计划3.2「测试与验收」第 1 组逐条写全——
 * readInt 三态(回 null/""/"abc" 一律 empty 且不抛,回 "12"/" 7 " 解析)、动词面 payload 逐字透传
 * (current/set v/add v/consume v/gain v)、下标消歧(energy#N 只由本层拼,§14.14)、
 * 写型口径(回 "" ⇒ true、回 null ⇒ false)、applyBuff 透传组件 id "buffs" 且不抛
 * (动词面以角色系统 BuffComponent#onOperationCommand javadoc 为准,断言只锁组件 id)、
 * sanTe 面三方法与能量面对称;另补缺席语义(读 empty、写 false,全类不抛)。
 * 复用 3.1 FakeRoleApi + RoleFacade 包私有 Supplier 构造缝,离线可跑、不碰 Bukkit 静态门。
 * 出自 施工计划3.2 测试与验收第 1 组;08-角色桥接 §8.3;14-陷阱清单 §14.14。
 */
class RoleStateAccessTest {

    /** 便捷缝:假 RoleAPI 经包私有 Supplier 构造 Facade,再包一层被测 RoleStateAccess(3.1 唯一测试缝复用)。 */
    private static RoleStateAccess accessOver(FakeRoleApi fake) {
        return new RoleStateAccess(new RoleFacade(() -> fake));
    }

    // ── 组1 readInt 三态:回 null/""/"abc" 一律 empty 且不抛,回 "12"/" 7 " 解析(§8.3/§14.14)──

    @Test
    void readInt三态_null空串非数字一律empty不抛() {
        FakeRoleApi fake = new FakeRoleApi();
        fake.operateHandler = call -> null;    // null = 未识别/无实例/内部异常被吞(三重含义,§14.14)
        RoleStateAccess access = accessOver(fake);
        UUID u = UUID.randomUUID();

        OptionalInt got = assertDoesNotThrow(() -> access.energy(u), "回 null 绝不外抛");
        assertTrue(got.isEmpty(), "回 null ⇒ empty(不回哨兵 -1)");

        fake.operateHandler = call -> "";      // "" = 已识别无回值
        assertTrue(access.energy(u).isEmpty(), "回 \"\" ⇒ empty");

        fake.operateHandler = call -> "12";
        assertEquals(OptionalInt.of(12), access.energy(u), "回 \"12\" ⇒ of(12)");

        fake.operateHandler = call -> " 7 ";
        assertEquals(OptionalInt.of(7), access.energy(u), "回 \" 7 \" ⇒ trim 后 of(7)");

        fake.operateHandler = call -> "abc";   // 非整数回值
        assertTrue(assertDoesNotThrow(() -> access.energy(u)).isEmpty(), "回 \"abc\" ⇒ NumberFormatException 吞掉 ⇒ empty");
    }

    // ── 组2 动词面 payload 逐字:current/set v/add v/consume v/gain v,sanTe 面与能量面对称(§8.3)──

    @Test
    void 动词面payload逐字透传_sanTe面与能量面对称() {
        FakeRoleApi fake = new FakeRoleApi();
        fake.operateHandler = call -> "1";     // 写类回写后状态/读类回值,这里只为不空手返回
        RoleStateAccess access = accessOver(fake);
        UUID u = UUID.randomUUID();

        access.energy(u);
        assertEquals(new FakeRoleApi.OperateCall(u, "energy", "current"), fake.operateCalls.get(0), "energy ⇒ current");

        access.setEnergy(u, 5);
        assertEquals(new FakeRoleApi.OperateCall(u, "energy", "set 5"), fake.operateCalls.get(1), "setEnergy ⇒ set 5");

        access.addEnergy(u, 7);
        assertEquals(new FakeRoleApi.OperateCall(u, "energy", "add 7"), fake.operateCalls.get(2), "addEnergy ⇒ add 7");

        access.consumeEnergy(u, 3);
        assertEquals(new FakeRoleApi.OperateCall(u, "energy", "consume 3"), fake.operateCalls.get(3), "consumeEnergy ⇒ consume 3");

        access.sanTe(u);
        assertEquals(new FakeRoleApi.OperateCall(u, "sante", "current"), fake.operateCalls.get(4), "sanTe ⇒ current(与能量面对称)");

        access.setSanTe(u, 9);
        assertEquals(new FakeRoleApi.OperateCall(u, "sante", "set 9"), fake.operateCalls.get(5), "setSanTe ⇒ set 9");

        access.gainSanTe(u, 10);
        assertEquals(new FakeRoleApi.OperateCall(u, "sante", "gain 10"), fake.operateCalls.get(6), "gainSanTe ⇒ gain 10");
    }

    // ── 组3 下标消歧:带下标拼 <id>#N、无下标裸 id;下标只改组件 id 不改动词(§14.14)──────────

    @Test
    void 下标消歧_带下标拼idN_无下标裸id() {
        FakeRoleApi fake = new FakeRoleApi();
        fake.operateHandler = call -> "1";
        RoleStateAccess access = accessOver(fake);
        UUID u = UUID.randomUUID();

        access.energy(u);
        assertEquals("energy", fake.operateCalls.get(0).componentId(), "无下标重载组件 id 为裸 energy");

        access.energy(u, 2);
        assertEquals("energy#2", fake.operateCalls.get(1).componentId(), "带下标重载组件 id 拼为 energy#2");
        assertEquals("current", fake.operateCalls.get(1).payload(), "下标只改组件 id 不改动词");

        access.setEnergy(u, 5, 2);
        assertEquals("energy#2", fake.operateCalls.get(2).componentId());
        access.addEnergy(u, 7, 2);
        assertEquals("energy#2", fake.operateCalls.get(3).componentId());
        access.consumeEnergy(u, 3, 2);
        assertEquals("energy#2", fake.operateCalls.get(4).componentId());

        access.sanTe(u, 1);
        assertEquals("sante#1", fake.operateCalls.get(5).componentId());
        access.setSanTe(u, 9, 1);
        assertEquals("sante#1", fake.operateCalls.get(6).componentId());
        access.gainSanTe(u, 10, 1);
        assertEquals("sante#1", fake.operateCalls.get(7).componentId());

        access.applyBuff(u, "STUN", 100, 3);
        assertEquals("buffs#3", fake.operateCalls.get(8).componentId());
    }

    // ── 组4 写型口径:回 "" ⇒ true(已识别无回值也是成功)、回 null ⇒ false(§8.3)──────────

    @Test
    void 写型口径_空串即成功_null即失败() {
        FakeRoleApi fake = new FakeRoleApi();
        RoleStateAccess access = accessOver(fake);
        UUID u = UUID.randomUUID();

        fake.operateHandler = call -> "";
        assertTrue(access.consumeEnergy(u, 3), "回 \"\" = 已识别无回值也是成功 ⇒ true");
        assertTrue(access.applyBuff(u, "STUN", 100), "applyBuff 同口径:回 \"\" ⇒ true");

        fake.operateHandler = call -> null;
        assertFalse(access.consumeEnergy(u, 3), "回 null ⇒ false");
        assertFalse(access.applyBuff(u, "STUN", 100), "applyBuff 回 null ⇒ false");
    }

    // ── 组5 applyBuff:透传组件 id "buffs" 且不抛(动词面以 BuffComponent javadoc 为准,断言只锁组件 id)──

    @Test
    void applyBuff透传buffs组件id不抛() {
        FakeRoleApi fake = new FakeRoleApi();
        fake.operateHandler = call -> "100";   // buffs 组件回写后剩余刻(BuffComponent javadoc)
        RoleStateAccess access = accessOver(fake);
        UUID u = UUID.randomUUID();

        assertTrue(assertDoesNotThrow(() -> access.applyBuff(u, "STUN", 100)), "施加不抛且回 true(非 null)");
        assertEquals(u, fake.operateCalls.get(0).uuid(), "操作 uuid 原样透传");
        assertEquals("buffs", fake.operateCalls.get(0).componentId(), "只锁组件 id:buffs");

        assertFalse(access.applyBuff(u, null, 100), "buffType 为 null ⇒ false(防御不发起操作)");
        assertEquals(1, fake.operateCalls.size(), "null 防御不得发起多余 operate");
    }

    // ── 组6 角色插件缺席:读 empty、写 false,全类不抛(缺席语义统一)────────────────────

    @Test
    void 缺席_读empty_写false_不抛() {
        RoleStateAccess access = new RoleStateAccess(new RoleFacade(() -> null));    // Supplier 恒 null = 角色插件缺席
        UUID u = UUID.randomUUID();

        assertDoesNotThrow(() -> {
            assertTrue(access.energy(u).isEmpty(), "缺席读能量 ⇒ empty");
            assertTrue(access.sanTe(u).isEmpty(), "缺席读理智 ⇒ empty");
            assertTrue(access.setEnergy(u, 5).isEmpty(), "缺席写能量 ⇒ empty(读不到写后状态)");
            assertTrue(access.setSanTe(u, 5).isEmpty(), "缺席写理智 ⇒ empty");
            assertFalse(access.consumeEnergy(u, 3), "缺席消耗 ⇒ false");
            assertFalse(access.applyBuff(u, "STUN", 100), "缺席施加 buff ⇒ false");
        }, "缺席期全部方法不抛");
    }
}
