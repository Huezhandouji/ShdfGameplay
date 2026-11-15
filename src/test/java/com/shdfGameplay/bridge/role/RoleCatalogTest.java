package com.shdfGameplay.bridge.role;

import com.shadowHunterRolesPlugin.api.RoleInfo;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * RoleCatalog 只读快照契约测试:照施工计划3.2「测试与验收」第 3 组写全——
 * fetch 后 roleIds/roles 与假 Facade 返回一致且为防御性拷贝(改原集合不影响快照)、
 * isExpired 的 TTL 边界(+29_999 不过期/+30_001 过期)、RoleDescriptor 内容透传无改写;
 * 外加缺席/入参 null 的空目录口径(缺席读空值,全类不抛)。
 * 复用 3.1 假 RoleAPI 打桩(FakeRoleApi),只用包私有 Supplier 构造缝,离线可跑、不碰 Bukkit 静态门。
 * 出自 施工计划3.2 测试与验收第 3 组;08-角色桥接 §8.5(只读快照且明确标注 TTL)。
 */
class RoleCatalogTest {

    // ── 组1 fetch 保真 + 防御性拷贝:改原集合不影响快照,快照自身不可改(§8.5)──

    @Test
    void fetch取目录_与假Facade一致_防御性拷贝() {
        FakeRoleApi fake = new FakeRoleApi();
        Set<String> ids = new HashSet<>(Set.of("red", "blue"));            // 可变原集合(桩按引用透传)
        List<RoleInfo> infos = new ArrayList<>(List.of(
                new RoleInfo("red", Component.text("红"), List.of(Component.text("描述")), Material.DIAMOND),
                new RoleInfo("sinThorn", Component.text("荆棘"), List.of(), null)));
        fake.allRoleIds = ids;
        fake.roles = infos;
        RoleFacade facade = new RoleFacade(() -> fake);

        long before = System.currentTimeMillis();
        RoleCatalog snapshot = RoleCatalog.fetch(facade);
        long after = System.currentTimeMillis();

        assertEquals(Set.of("red", "blue"), snapshot.roleIds(), "roleIds 与假 Facade 返回一致");
        assertEquals(2, snapshot.roles().size(), "roles 条数一致");
        assertTrue(snapshot.fetchedAtMillis() >= before && snapshot.fetchedAtMillis() <= after,
                "fetchedAtMillis 落在 fetch 调用窗口内");

        ids.remove("red");                                                 // 改 fetch 拷贝源的原集合
        infos.clear();
        assertEquals(Set.of("red", "blue"), snapshot.roleIds(), "改原 id 集合不影响快照");
        assertEquals(2, snapshot.roles().size(), "清空原 RoleInfo 表不影响快照");

        // 只读快照无写口:Set.copyOf/List.copyOf 产物不可改,add 即 UnsupportedOperationException
        assertThrows(UnsupportedOperationException.class, () -> snapshot.roleIds().add("green"),
                "快照 roleIds 不可改");
        assertThrows(UnsupportedOperationException.class, () -> snapshot.roles().add(null),
                "快照 roles 不可改");
    }

    // ── 组2 isExpired 纯函数 TTL 边界:显式 TTL 30s,严格「超过」才算过期(§8.5)──

    @Test
    void isExpired按TTL边界判定() {
        assertEquals(30_000L, RoleCatalog.DEFAULT_TTL_MILLIS, "默认 TTL 显式标注 30s(§8.5 原则:只读快照须明确标注 TTL)");
        RoleCatalog snapshot = RoleCatalog.fetch(new RoleFacade(() -> new FakeRoleApi()));
        long fetchedAt = snapshot.fetchedAtMillis();

        assertFalse(snapshot.isExpired(RoleCatalog.DEFAULT_TTL_MILLIS, fetchedAt + 29_999),
                "fetchedAt+29_999 不过期");
        assertFalse(snapshot.isExpired(RoleCatalog.DEFAULT_TTL_MILLIS, fetchedAt + 30_000),
                "恰等于 TTL 不算「超过」,不过期");
        assertTrue(snapshot.isExpired(RoleCatalog.DEFAULT_TTL_MILLIS, fetchedAt + 30_001),
                "fetchedAt+30_001 过期");
    }

    // ── 组3 RoleDescriptor 透传无改写:转换口径 3.1 已测,这里只测快照保真(§8.5)──

    @Test
    void RoleDescriptor透传无改写() {
        FakeRoleApi fake = new FakeRoleApi();
        Component name = Component.text("红");
        List<Component> desc = List.of(Component.text("描述"));
        fake.roles = List.of(new RoleInfo("red", name, desc, Material.DIAMOND));
        RoleFacade facade = new RoleFacade(() -> fake);

        RoleCatalog snapshot = RoleCatalog.fetch(facade);
        List<RoleDescriptor> fresh = facade.catalog();    // 同一来源的现转结果作对照

        assertEquals(fresh, snapshot.roles(), "快照内 Descriptor 与现转结果结构相等(透传无改写)");
        assertEquals(1, snapshot.roles().size());
        assertEquals("red", snapshot.roles().get(0).id());
        assertSame(name, snapshot.roles().get(0).displayName(), "displayName 引用透传");
        assertSame(desc, snapshot.roles().get(0).description(), "description 引用透传");
        assertEquals(Material.DIAMOND, snapshot.roles().get(0).icon());
    }

    // ── 补充:缺席/入参 null ⇒ 空目录快照,不抛(缺席读空值口径,§14.22 规避面)──

    @Test
    void 缺席或null入参_fetch回空目录快照_不抛() {
        RoleFacade absent = new RoleFacade(() -> null);    // 角色插件缺席(lookup 恒 null)
        RoleCatalog snapshot = RoleCatalog.fetch(absent);
        assertTrue(snapshot.roleIds().isEmpty(), "缺席 roleIds 空集");
        assertTrue(snapshot.roles().isEmpty(), "缺席 roles 空表");
        assertFalse(snapshot.isExpired(RoleCatalog.DEFAULT_TTL_MILLIS, snapshot.fetchedAtMillis()),
                "空快照同样受 TTL 语义约束(取后即查不过期)");

        RoleCatalog nullSnap = RoleCatalog.fetch(null);    // 入参 null 视同缺席
        assertTrue(nullSnap.roleIds().isEmpty(), "role 为 null 回空目录快照");
        assertTrue(nullSnap.roles().isEmpty());
    }
}
