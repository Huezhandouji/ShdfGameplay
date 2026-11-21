package com.shdfGameplay.bridge.role;

import com.shadowHunterRolesPlugin.api.RoleAPI;
import com.shadowHunterRolesPlugin.api.RoleInfo;
import com.shadowHunterRolesPlugin.api.event.RoleClearEvent;
import com.shadowHunterRolesPlugin.api.event.RoleDeathEvent;
import com.shadowHunterRolesPlugin.api.event.RoleEvent;
import com.shadowHunterRolesPlugin.api.event.RoleSetEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * RoleFacade 防腐层契约测试:照施工计划3.1「测试与验收」10 组断言逐组写全(懒解析/缺席语义/roleIcon AIR/
 * catalog 转换+四字段无阵营/阵营读方向/阵营写方向/areHostile 四态/invalidate 重取/1:1 完整性反射/operate 透传真值),
 * 外加订阅面的行为补充(翻译/未知跳过/close 幂等/缺席登记与 reload 换代补订)。
 * 只用包私有 Supplier 构造缝注入假 API(FakeRoleApi),离线可跑、不碰 Bukkit 静态门。
 * 出自 施工计划3.1 测试与验收 10 组;08-角色桥接 §8.2/§8.8/§8.10;角色插件 RoleAPI#subscribeEvents 合约。
 */
class RoleFacadeContractTest {

    /** 可换源的计数 Supplier:懒解析/失效重取断言用(记录 lookup 被取次数,当前应答实例可为 null=缺席)。 */
    private static final class SwitchableLookup implements Supplier<RoleAPI> {

        private final AtomicInteger calls = new AtomicInteger();
        private RoleAPI current;

        SwitchableLookup(RoleAPI initial) {
            this.current = initial;
        }

        void switchTo(RoleAPI next) {
            this.current = next;
        }

        int calls() {
            return calls.get();
        }

        @Override
        public RoleAPI get() {
            calls.incrementAndGet();
            return current;
        }
    }

    // ── 组1 懒解析:构造不取,首次调用取一次,之后命中缓存(§8.8)──────────

    @Test
    void 懒解析_构造不取_首次取一次_之后命中缓存() {
        FakeRoleApi fake = new FakeRoleApi();
        fake.allRoleIds = Set.of("red");
        SwitchableLookup lookup = new SwitchableLookup(fake);
        RoleFacade facade = new RoleFacade(lookup);

        assertEquals(0, lookup.calls(), "构造期不得触发 lookup");
        assertEquals(Set.of("red"), facade.knownRoleIds());
        assertEquals(1, lookup.calls(), "首次调用取一次");
        facade.knownRoleIds();
        facade.hasRole(UUID.randomUUID());
        facade.available();
        assertEquals(1, lookup.calls(), "之后命中缓存不再取");
    }

    // ── 组2 插件缺席(Supplier 恒 null):缺席读空/写 false,全类不抛(§14.22 规避面)──

    @Test
    void 缺席_读空值_写false_不抛() {
        SwitchableLookup lookup = new SwitchableLookup(null);
        RoleFacade facade = new RoleFacade(lookup);
        UUID u = UUID.randomUUID();

        assertTrue(facade.knownRoleIds().isEmpty(), "缺席 knownRoleIds 空集");
        assertTrue(facade.catalog().isEmpty(), "缺席 catalog 空表");
        assertEquals(Material.AIR, facade.roleIcon("red"), "缺席 roleIcon 回 AIR");
        assertEquals(RoleFaction.UNKNOWN, facade.playerFaction(u), "缺席 playerFaction 回 UNKNOWN");
        assertEquals(RoleFaction.UNKNOWN, facade.declaredFaction(u), "缺席 declaredFaction 回 UNKNOWN");
        assertFalse(facade.setRole(u, "red"), "缺席 setRole 回 false");
        assertFalse(facade.clearRole(u), "缺席 clearRole 回 false");
        assertFalse(facade.setPlayerFaction(u, RoleFaction.HUNTER), "缺席 setPlayerFaction 回 false");
        assertFalse(facade.resetPlayerFaction(u), "缺席 resetPlayerFaction 回 false");
        assertTrue(facade.areHostile(u, UUID.randomUUID()), "缺席 areHostile 回 true(发起方无实例口径,§8.10(二))");
        assertFalse(facade.available(), "缺席 available 回 false");
        assertNull(facade.roleIdOf(u));
        assertFalse(facade.hasRole(u));
        assertNull(facade.roleDisplayName("red"));
        assertTrue(facade.roleDescription("red").isEmpty());
        assertNull(facade.playerRoleDisplayName(u));
        assertNull(facade.operate(u, "energy", "current"));
        assertFalse(facade.isValidRole("red"));
        assertNull(facade.lastDamagerUuid(null), "击杀归属读口 null 受害者防御(读原版 getKiller,不经 RoleAPI)");
        assertNull(facade.lastDamager(null), "备用读口同 null 防御");
        assertEquals(1, lookup.calls(), "null 也缓存:缺席只解析一次,不打第二遍 ServicesManager");
    }

    // ── 组3 roleIcon:假 API 回 null ⇒ Facade 回 Material.AIR 而非 null(§8.2)──

    @Test
    void roleIcon缺失回AIR() {
        FakeRoleApi fake = new FakeRoleApi();
        fake.roleIcon = id -> null;
        RoleFacade facade = new RoleFacade(() -> fake);

        assertEquals(Material.AIR, facade.roleIcon("red"), "缺失回 AIR 而非 null");
        fake.roleIcon = id -> Material.DIAMOND;
        assertEquals(Material.DIAMOND, facade.roleIcon("red"), "在场原样透传");
    }

    // ── 组4 catalog():RoleInfo→RoleDescriptor 现转;反射断言 record 恰四字段无阵营(§8.2)──

    @Test
    void catalog转换_四字段无阵营() {
        FakeRoleApi fake = new FakeRoleApi();
        Component name = Component.text("红");
        List<Component> desc = List.of(Component.text("描述"));
        fake.roles = List.of(
                new RoleInfo("red", name, desc, Material.DIAMOND),
                new RoleInfo("blue", Component.text("蓝"), List.of(), null));    // icon 缺失
        RoleFacade facade = new RoleFacade(() -> fake);

        List<RoleDescriptor> catalog = facade.catalog();
        assertEquals(2, catalog.size());
        assertEquals("red", catalog.get(0).id());
        assertSame(name, catalog.get(0).displayName());
        assertSame(desc, catalog.get(0).description());
        assertEquals(Material.DIAMOND, catalog.get(0).icon());
        assertEquals(Material.AIR, catalog.get(1).icon(), "RoleInfo.icon 为 null 时落 Material.AIR");

        RecordComponent[] components = RoleDescriptor.class.getRecordComponents();
        assertEquals(4, components.length, "RoleDescriptor 恰四字段");
        assertEquals(Set.of("id", "displayName", "description", "icon"),
                Arrays.stream(components).map(RecordComponent::getName).collect(Collectors.toSet()),
                "四字段名且无 faction");
    }

    // ── 组5 阵营读方向:parse 映射口径 + 经 playerFaction/declaredFaction(§8.2)──

    @Test
    void 阵营读方向映射() {
        assertEquals(RoleFaction.SHADOW, RoleFaction.parse("SHADOW"));
        assertEquals(RoleFaction.HUNTER, RoleFaction.parse("HUNTER"));
        assertEquals(RoleFaction.OTHER, RoleFaction.parse("shadow"), "大小写敏感:小写认不出 ⇒ OTHER");
        assertEquals(RoleFaction.OTHER, RoleFaction.parse("XYZ"), "未来新阵营名 ⇒ OTHER");
        assertEquals(RoleFaction.UNKNOWN, RoleFaction.parse(null));
        assertEquals(RoleFaction.UNKNOWN, RoleFaction.parse(""));

        FakeRoleApi fake = new FakeRoleApi();
        fake.operateHandler = call -> "faction".equals(call.payload()) ? "SHADOW" : "HUNTER";
        RoleFacade facade = new RoleFacade(() -> fake);
        UUID u = UUID.randomUUID();
        assertEquals(RoleFaction.SHADOW, facade.playerFaction(u));
        assertEquals(RoleFaction.HUNTER, facade.declaredFaction(u));

        fake.operateHandler = call -> null;    // 无实例/读不到 ⇒ UNKNOWN
        assertEquals(RoleFaction.UNKNOWN, facade.playerFaction(u));
        assertEquals(RoleFaction.UNKNOWN, facade.declaredFaction(u));
    }

    // ── 组6 阵营写方向:OTHER 拒绝且零 operate;set 透传与组件拒绝;wireName 双向(§8.10(二))──

    @Test
    void 阵营写方向_OTHER拒绝_透传与拒绝回传() {
        FakeRoleApi fake = new FakeRoleApi();
        fake.operateHandler = call -> "HUNTER";    // 组件回「写后阵营名」
        RoleFacade facade = new RoleFacade(() -> fake);
        UUID u = UUID.randomUUID();

        assertFalse(facade.setPlayerFaction(u, RoleFaction.OTHER), "OTHER ⇒ false");
        assertTrue(fake.operateCalls.isEmpty(), "OTHER 拒绝时不得发起任何 operate");

        assertTrue(facade.setPlayerFaction(u, RoleFaction.HUNTER), "回非空 ⇒ true");
        assertEquals(new FakeRoleApi.OperateCall(u, "faction", "set HUNTER"), fake.operateCalls.get(0), "透传 set HUNTER");
        assertTrue(facade.setPlayerFaction(u, RoleFaction.SHADOW));
        assertEquals("set SHADOW", fake.operateCalls.get(1).payload());
        assertTrue(facade.setPlayerFaction(u, RoleFaction.UNKNOWN), "set UNKNOWN 也是合法写动词(组件三名单)");
        assertEquals("set UNKNOWN", fake.operateCalls.get(2).payload());

        fake.operateHandler = call -> null;        // 组件拒绝(如小写名)回 null ⇒ Facade 回 false(本测试只验透传)
        assertFalse(facade.setPlayerFaction(u, RoleFaction.HUNTER));
        assertEquals("set HUNTER", fake.operateCalls.get(3).payload(), "拒绝时调用仍原样发出");

        fake.operateHandler = call -> "SHADOW";    // reset 回复位后阵营名 ⇒ true
        assertTrue(facade.resetPlayerFaction(u));
        assertEquals("reset", fake.operateCalls.get(4).payload());

        // wireName 双向:OTHER 无名 ⇒ null(拒绝且不发起操作),其余回自身枚举名
        assertEquals("SHADOW", RoleFaction.SHADOW.wireName());
        assertEquals("HUNTER", RoleFaction.HUNTER.wireName());
        assertEquals("UNKNOWN", RoleFaction.UNKNOWN.wireName());
        assertNull(RoleFaction.OTHER.wireName());
    }

    // ── 组7 areHostile 四态:参数 null/回 null/回 "true"/回 "false"(§8.10(二))──

    @Test
    void areHostile四态与非对称发起方() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        FakeRoleApi fake = new FakeRoleApi();
        RoleFacade facade = new RoleFacade(() -> fake);

        assertFalse(facade.areHostile(null, b), "a 为 null ⇒ false");
        assertFalse(facade.areHostile(a, null), "b 为 null ⇒ false");
        assertTrue(fake.operateCalls.isEmpty(), "参数 null 时连 operate 都不发起");

        fake.operateHandler = call -> null;
        assertTrue(facade.areHostile(a, b), "发起方 a 无实例(回 null)⇒ true");
        fake.operateHandler = call -> "true";
        assertTrue(facade.areHostile(a, b), "回 \"true\" ⇒ true");
        fake.operateHandler = call -> "false";
        assertFalse(facade.areHostile(a, b), "回 \"false\" ⇒ false");
        fake.operateHandler = call -> "maybe";
        assertFalse(facade.areHostile(a, b), "意外串(协议漂移)⇒ false");

        // 非对称:动词 hostile <b> 在发起方 a 的实例上执行(操作 uuid=a)
        FakeRoleApi.OperateCall last = fake.operateCalls.get(fake.operateCalls.size() - 1);
        assertEquals(a, last.uuid(), "操作落在发起方 a 的实例上");
        assertEquals("faction", last.componentId());
        assertEquals("hostile " + b, last.payload());
    }

    // ── 组8 invalidate():失效后重新取 Supplier(为 3.3 动态刷新铺路,§8.8)──

    @Test
    void invalidate后重取Supplier() {
        FakeRoleApi first = new FakeRoleApi();
        SwitchableLookup lookup = new SwitchableLookup(first);
        RoleFacade facade = new RoleFacade(lookup);

        facade.knownRoleIds();
        assertEquals(1, lookup.calls());

        facade.invalidate();    // 失效:清缓存并立即重解析一次
        assertEquals(2, lookup.calls(), "invalidate 立即重解析一次");
        facade.knownRoleIds();
        assertEquals(2, lookup.calls(), "新缓存命中不再取");

        FakeRoleApi second = new FakeRoleApi();
        second.allRoleIds = Set.of("blue");
        lookup.switchTo(second);
        facade.invalidate();
        assertEquals(Set.of("blue"), facade.knownRoleIds(), "重取到新实例");
        assertEquals(3, lookup.calls());
    }

    // ── 组9 1:1 完整性:反射列公开方法对照 §8.10(一)镜像行+订阅面+阵营五动词+击杀归属两读口 ──

    @Test
    void 反射_公开面齐全且无自造方法() throws NoSuchMethodException {
        Class<RoleFacade> c = RoleFacade.class;

        // 12 个 1:1 映射(§8.10(一)逐行;参数类型逐字)
        assertNotNull(c.getMethod("knownRoleIds"));
        assertNotNull(c.getMethod("catalog"));
        assertNotNull(c.getMethod("isValidRole", String.class));
        assertNotNull(c.getMethod("roleDisplayName", String.class));
        assertNotNull(c.getMethod("roleDescription", String.class));
        assertNotNull(c.getMethod("roleIcon", String.class));
        assertNotNull(c.getMethod("roleIdOf", UUID.class));
        assertNotNull(c.getMethod("hasRole", UUID.class));
        assertNotNull(c.getMethod("playerRoleDisplayName", UUID.class));
        assertNotNull(c.getMethod("setRole", UUID.class, String.class));
        assertNotNull(c.getMethod("clearRole", UUID.class));
        assertNotNull(c.getMethod("operate", UUID.class, String.class, String.class));

        // 击杀归属两读口(本地桥,非 RoleAPI 镜像:角色插件已删 getLastDamager*,读原版 getKiller)
        assertNotNull(c.getMethod("lastDamagerUuid", Player.class), "参数必须 Player");
        assertNotNull(c.getMethod("lastDamager", Player.class), "备用读口");

        // 订阅面 + 阵营五动词(经 operate 的类型化面)
        assertNotNull(c.getMethod("subscribeRoleEvents", Consumer.class));
        assertNotNull(c.getMethod("playerFaction", UUID.class));
        assertNotNull(c.getMethod("declaredFaction", UUID.class));
        assertNotNull(c.getMethod("setPlayerFaction", UUID.class, RoleFaction.class));
        assertNotNull(c.getMethod("resetPlayerFaction", UUID.class));
        assertNotNull(c.getMethod("areHostile", UUID.class, UUID.class));

        // 公开面恰为 20 方法+桥接生命周期 available/invalidate(计数口径 3.1:103:生命周期方法不计入对照表)
        Set<String> declared = Arrays.stream(c.getDeclaredMethods())
                .filter(m -> Modifier.isPublic(m.getModifiers()))
                .map(Method::getName)
                .collect(Collectors.toSet());
        assertEquals(Set.of(
                "knownRoleIds", "catalog", "isValidRole", "roleDisplayName", "roleDescription",
                "roleIcon", "roleIdOf", "hasRole", "playerRoleDisplayName",
                "setRole", "clearRole", "lastDamagerUuid", "lastDamager", "operate",
                "subscribeRoleEvents",
                "playerFaction", "declaredFaction", "setPlayerFaction", "resetPlayerFaction", "areHostile",
                "available", "invalidate"), declared);
    }

    // ── 组10 透传真值:operate 对任意三元组原样透传(含 null payload),不加预处理不吞返回值(§14.14)──

    @Test
    void operate透传真值_含nullPayload() {
        FakeRoleApi fake = new FakeRoleApi();
        fake.operateHandler = call -> "42";
        RoleFacade facade = new RoleFacade(() -> fake);
        UUID u = UUID.randomUUID();

        assertEquals("42", facade.operate(u, "energy", "add 5"), "返回值原样透传");
        assertEquals(new FakeRoleApi.OperateCall(u, "energy", "add 5"), fake.operateCalls.get(0), "三元组原样入册");

        fake.operateHandler = call -> null;
        assertNull(facade.operate(u, "energy", null), "null 返回不吞");
        assertNull(fake.operateCalls.get(1).payload(), "null payload 原样透传");

        fake.operateHandler = call -> "";
        assertEquals("", facade.operate(u, "sante", "current"), "空串返回也原样透传");
    }

    // ── 补充:订阅面行为(施工计划3.1 步骤6 订阅面;角色插件 RoleAPI#subscribeEvents 合约)──

    @Test
    void 订阅面_翻译三已知种_未知静默跳过_close幂等() {
        FakeRoleApi fake = new FakeRoleApi();
        RoleFacade facade = new RoleFacade(() -> fake);
        List<RoleSignal> received = new ArrayList<>();

        RoleEventHandle handle = facade.subscribeRoleEvents(received::add);
        assertEquals(1, fake.subscribers.size(), "经 ensureApi() 立即订阅");

        UUID u = UUID.randomUUID();
        fake.publish(new RoleSetEvent(u, "red"));
        fake.publish(new RoleClearEvent(u, "red"));
        fake.publish(new RoleDeathEvent(u, "red"));
        assertEquals(3, received.size());
        assertEquals(RoleSignal.Kind.SET, received.get(0).kind());
        assertEquals(u, received.get(0).player());
        assertEquals("red", received.get(0).roleId());
        assertEquals(RoleSignal.Kind.CLEAR, received.get(1).kind());
        assertEquals(RoleSignal.Kind.DEATH, received.get(2).kind());

        fake.publish(new RoleEvent(u, "red") {
        });    // 未知事件种类(三已知种之外的子类)静默跳过
        assertEquals(3, received.size(), "未知事件种类不得构造 RoleSignal");

        handle.close();
        assertEquals(0, fake.subscribers.size(), "close 退订当前令牌");
        fake.publish(new RoleSetEvent(u, "red"));
        assertEquals(3, received.size(), "close 后不再收");
        handle.close();    // 幂等:重复 close 无害
        assertEquals(0, fake.subscribers.size());
    }

    @Test
    void 订阅面_缺席登记不弃_reload换代自动补订() {
        SwitchableLookup lookup = new SwitchableLookup(null);    // 先缺席
        RoleFacade facade = new RoleFacade(lookup);
        List<RoleSignal> received = new ArrayList<>();

        RoleEventHandle handle = facade.subscribeRoleEvents(received::add);    // 缺席期只登记不订阅,不抛

        FakeRoleApi fake = new FakeRoleApi();
        lookup.switchTo(fake);
        facade.invalidate();    // 服务注册驱动:重解析出新实例 ⇒ 在册 handler 全量补订
        assertEquals(1, fake.subscribers.size(), "恢复即自动补订");
        UUID u = UUID.randomUUID();
        fake.publish(new RoleSetEvent(u, "red"));
        assertEquals(1, received.size(), "缺席期登记的 handler 在恢复后收到事件");

        FakeRoleApi reloaded = new FakeRoleApi();    // 模拟角色插件 reload:换新实例,旧令牌作废
        lookup.switchTo(reloaded);
        facade.invalidate();
        assertEquals(0, fake.subscribers.size(), "旧令牌全部退订(退订作废令牌无害幂等)");
        assertEquals(1, reloaded.subscribers.size(), "新实例上全量重订");
        reloaded.publish(new RoleDeathEvent(u, "red"));
        assertEquals(RoleSignal.Kind.DEATH, received.get(1).kind(), "同一句柄无需重订,新实例事件照收");

        handle.close();
        assertEquals(0, reloaded.subscribers.size());
    }

    @Test
    void 订阅面_nullHandler不登记不抛() {
        FakeRoleApi fake = new FakeRoleApi();
        RoleFacade facade = new RoleFacade(() -> fake);

        RoleEventHandle handle = facade.subscribeRoleEvents(null);
        assertTrue(fake.subscribers.isEmpty(), "handler 为 null 时不登记");
        handle.close();    // 空句柄 close 幂等无害
        handle.close();
        assertTrue(fake.subscribers.isEmpty());
    }
}
