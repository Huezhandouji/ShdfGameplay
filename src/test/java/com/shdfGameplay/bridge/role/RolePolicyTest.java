package com.shdfGameplay.bridge.role;

import com.shdfGameplay.component.role.RolePolicies;
import org.junit.jupiter.api.Test;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * RolePolicy/RolePolicies 策略数据件测试:照施工计划3.2「测试与验收」第 2 组写全——
 * 反射断言 RolePolicy 恰 8 组件防增删改名、NONE 逐字段、BOMB/TDM/BOSS 三常量逐字段与 08 §8.6 示例一致
 * (防抄错池子/人数/换角上限)、Map.of/Set.of 只读与传入 map 原引用改行不污染冻结常量。
 * 纯数据断言+反射,不 import 角色插件类型、离线可跑。
 * 出自 施工计划3.2 测试与验收 第 2 组;08-角色桥接 §8.6。
 */
class RolePolicyTest {

    // ── 组1 反射:恰八组件,名字与顺序照 §8.6 逐字(顺序即 new RolePolicy(...) 构造位) ──

    @Test
    void 反射_恰八组件防增删改名() {
        List<String> names = Arrays.stream(RolePolicy.class.getRecordComponents())
                .map(RecordComponent::getName)
                .collect(Collectors.toList());
        assertEquals(List.of(
                "allowDuplicates", "poolByTeam", "globallyBanned", "minParticipantsBeforeSelect",
                "applyOnDeployOnly", "clearOnDeath", "autoAssign", "maxReselectsPerPlayer"), names,
                "恰八字段且名字与 08 §8.6 逐字一致");

        assertEquals(List.of("NONE", "RANDOM_AVAILABLE", "FIRST_AVAILABLE", "BALANCED"),
                Arrays.stream(RolePolicy.AssignmentStrategy.values()).map(Enum::name).collect(Collectors.toList()),
                "AssignmentStrategy 恰四值(08 §8.6)");
    }

    // ── 组2 NONE 常量逐字段(§8.6 定义:大厅/无角色模式的「不介入」策略) ──

    @Test
    void NONE常量_逐字段() {
        RolePolicy none = RolePolicy.NONE;
        assertTrue(none.allowDuplicates());
        assertTrue(none.poolByTeam().isEmpty(), "空池 = 全员池(不限制)");
        assertTrue(none.globallyBanned().isEmpty());
        assertEquals(0, none.minParticipantsBeforeSelect());
        assertFalse(none.applyOnDeployOnly());
        assertFalse(none.clearOnDeath());
        assertEquals(RolePolicy.AssignmentStrategy.NONE, none.autoAssign());
        assertEquals(-1, none.maxReselectsPerPlayer(), "-1 = 不限");
    }

    // ── 组3~5 三策常量逐字段(§8.6 示例值逐项照抄,防抄错池子/人数/换角上限) ──

    @Test
    void BOMB_POLICY_逐字段() {
        RolePolicy p = RolePolicies.BOMB_POLICY;
        assertFalse(p.allowDuplicates(), "炸弹攻防:同一角色全场唯一(含跨队)");
        assertEquals(Map.of(
                "ATTACKER", Set.of("red", "canglu"),
                "DEFENDER", Set.of("sinThorn", "remoteness")), p.poolByTeam(), "按队各有池子");
        assertEquals(Set.of("selfUpdateExample"), p.globallyBanned(), "全局禁用");
        assertEquals(4, p.minParticipantsBeforeSelect(), "人数门 4(防单人偷跑)");
        assertTrue(p.applyOnDeployOnly(), "选角只记录,部署/复活才 setRole");
        assertTrue(p.clearOnDeath(), "死亡立刻 clearRole");
        assertEquals(RolePolicy.AssignmentStrategy.RANDOM_AVAILABLE, p.autoAssign());
        assertEquals(1, p.maxReselectsPerPlayer(), "整局换角上限 1");
    }

    @Test
    void TDM_POLICY_逐字段() {
        RolePolicy p = RolePolicies.TDM_POLICY;
        assertTrue(p.allowDuplicates(), "团队死斗:允许重复");
        assertTrue(p.poolByTeam().isEmpty(), "空池 = 全员池");
        assertTrue(p.globallyBanned().isEmpty());
        assertEquals(2, p.minParticipantsBeforeSelect());
        assertFalse(p.applyOnDeployOnly(), "复活即应用(快速迭代手感)");
        assertFalse(p.clearOnDeath());
        assertEquals(RolePolicy.AssignmentStrategy.FIRST_AVAILABLE, p.autoAssign());
        assertEquals(-1, p.maxReselectsPerPlayer(), "-1 = 换角不限");
    }

    @Test
    void BOSS_POLICY_逐字段() {
        RolePolicy p = RolePolicies.BOSS_POLICY;
        assertFalse(p.allowDuplicates(), "首领战:不允许重复");
        assertEquals(Map.of("RAIDERS", Set.of("meiqihezi", "sinThorn")), p.poolByTeam(),
                "锁定池(保证 Boss 机制可解)");
        assertTrue(p.globallyBanned().isEmpty());
        assertEquals(3, p.minParticipantsBeforeSelect());
        assertTrue(p.applyOnDeployOnly());
        assertFalse(p.clearOnDeath());
        assertEquals(RolePolicy.AssignmentStrategy.NONE, p.autoAssign(), "未选角不兜底分配");
        assertEquals(0, p.maxReselectsPerPlayer(), "禁换角");
    }

    // ── 组6 不可变性:Map.of/Set.of 只读;传入 map 原引用改行不影响冻结常量(3.2「关键实现要点」) ──

    @Test
    void 冻结常量_MapOfSetOf只读() {
        for (RolePolicy p : List.of(RolePolicies.BOMB_POLICY, RolePolicies.TDM_POLICY,
                RolePolicies.BOSS_POLICY, RolePolicy.NONE)) {
            assertThrows(UnsupportedOperationException.class, () -> p.poolByTeam().put("NEW_TEAM", Set.of()),
                    "外层 poolByTeam 不可写");
            assertThrows(UnsupportedOperationException.class, () -> p.globallyBanned().add("sneaky"),
                    "globallyBanned 不可写");
        }
        assertThrows(UnsupportedOperationException.class,
                () -> RolePolicies.BOMB_POLICY.poolByTeam().get("ATTACKER").add("polluted"),
                "内层池同样是 Set.of 只读");
        assertThrows(UnsupportedOperationException.class,
                () -> RolePolicies.BOSS_POLICY.poolByTeam().get("RAIDERS").add("polluted"));
    }

    @Test
    void 冻结常量_原引用改行不影响() {
        // 模拟模式工厂手里的可变池子(HashMap/HashSet):常量以 Map.of/Set.of 字面量构造,
        // 构造期即定值,此后与任何外部可变结构互不为别名——对「原引用」改行自然污染不到常量。
        Set<String> attackPool = new HashSet<>(Set.of("red", "canglu"));
        Map<String, Set<String>> factoryPool = new HashMap<>();
        factoryPool.put("ATTACKER", attackPool);
        factoryPool.put("DEFENDER", new HashSet<>(Set.of("sinThorn", "remoteness")));

        attackPool.add("meiqihezi");                         // 改行①:池内塞角色
        factoryPool.put("RAIDERS", new HashSet<>(Set.of())); // 改行②:加新队键
        assertEquals(3, attackPool.size(), "自检:原引用的改行确实生效");
        assertEquals(3, factoryPool.size(), "自检:原引用的改行确实生效");

        assertEquals(Map.of(
                "ATTACKER", Set.of("red", "canglu"),
                "DEFENDER", Set.of("sinThorn", "remoteness")), RolePolicies.BOMB_POLICY.poolByTeam(),
                "外部可变结构改行不影响冻结常量(Map.of 拷贝语义)");
        assertEquals(Set.of("selfUpdateExample"), RolePolicies.BOMB_POLICY.globallyBanned());
    }
}
