package com.shdfGameplay.bridge.role;

import com.shadowHunterRolesPlugin.api.RoleAPI;
import com.shadowHunterRolesPlugin.api.RoleEventSubscription;
import com.shadowHunterRolesPlugin.api.RoleInfo;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 共享测试桩:可配置的假 RoleAPI(实现冻结面全部方法,含 subscribeEvents 事件订阅面)。
 * 各方法返回值经公开字段/函数配置;executeComponentOperation 调用与事件订阅全程记录——
 * 供 3.1 RoleFacadeContractTest 与 3.2 RoleStateAccess 契约测试复用,离线可跑、不碰 Bukkit 静态门。
 * 出自 施工计划3.1 测试与验收/施工计划3.2(复用 3.1 假 RoleAPI 打桩)。
 */
public final class FakeRoleApi implements RoleAPI {

    /** 一次 executeComponentOperation 调用的原样三元组(uuid/componentId/payload,null payload 也原样入册)。 */
    public record OperateCall(UUID uuid, String componentId, String payload) {
    }

    // ── 可配置返回值(缺省即缺席/缺失语义:空集合、null、false)──────────

    /** getAllRoleIds 回值(默认空集)。 */
    public Set<String> allRoleIds = Set.of();

    /** getRoles 回值(默认空表)。 */
    public List<RoleInfo> roles = List.of();

    /** isValidRoleId 判定集合(默认全 false)。 */
    public final Set<String> validRoleIds = new HashSet<>();

    /** getRoleDisplayName 应答(默认 null)。 */
    public Function<String, Component> roleDisplayName = id -> null;

    /** getRoleDescription 应答(默认 null)。 */
    public Function<String, List<Component>> roleDescription = id -> null;

    /** getRoleIcon 应答(默认 null——icon 缺失场景)。 */
    public Function<String, Material> roleIcon = id -> null;

    /** getPlayerRoleId 回值(默认 null)。 */
    public String playerRoleId = null;

    /** hasRole 回值(默认 false)。 */
    public boolean hasRoleResult = false;

    /** getPlayerRoleDisplayName 应答(默认 null)。 */
    public Function<UUID, Component> playerRoleDisplayName = uuid -> null;

    /** setPlayerRole 回值(默认 false)。 */
    public boolean setRoleResult = false;

    /** clearPlayerRole 回值(默认 false)。 */
    public boolean clearRoleResult = false;

    /** getLastDamagerUuid 回值(默认 null)。 */
    public UUID lastDamagerUuidResult = null;

    /** getLastDamager 回值(默认 null)。 */
    public Player lastDamagerPlayerResult = null;

    // ── operate 面:可配置应答 + 调用记录 ──────────────────────────────

    /** operate 应答器:入参为调用三元组,回值即 executeComponentOperation 返回(默认 null=未识别/拒绝)。 */
    public Function<OperateCall, String> operateHandler = call -> null;

    /** operate 调用记录(按调用顺序原样三元组,含 null payload)。 */
    public final List<OperateCall> operateCalls = new ArrayList<>();

    @Override
    public String executeComponentOperation(UUID uuid, String componentId, String payload) {
        OperateCall call = new OperateCall(uuid, componentId, payload);
        operateCalls.add(call);
        return operateHandler.apply(call);
    }

    // ── 事件订阅面:订阅记录 + 模拟发布 ────────────────────────────────

    /** 当前在册事件订阅(按订阅顺序;退订即移除)。 */
    public final List<Consumer<Event>> subscribers = new ArrayList<>();

    @Override
    public RoleEventSubscription subscribeEvents(Consumer<Event> handler) {
        if (handler == null) {
            throw new IllegalArgumentException("handler 为 null(照 RoleAPI 合约如实抛)");
        }
        subscribers.add(handler);
        return () -> subscribers.remove(handler);    // 令牌幂等退订(重复退订无害)
    }

    /** 模拟角色插件发布:按订阅顺序同步分发给当前在册 handler(快照迭代,回调中退订不影响本次)。 */
    public void publish(Event event) {
        for (Consumer<Event> subscriber : List.copyOf(subscribers)) {
            subscriber.accept(event);
        }
    }

    // ── 查询/写面直通可配置返回值 ─────────────────────────────────────

    @Override
    public UUID getLastDamagerUuid(Player player) {
        return lastDamagerUuidResult;
    }

    @Override
    public Player getLastDamager(Player player) {
        return lastDamagerPlayerResult;
    }

    @Override
    public boolean isValidRoleId(String id) {
        return id != null && validRoleIds.contains(id);
    }

    @Override
    public boolean setPlayerRole(UUID uuid, String roleId) {
        return setRoleResult;
    }

    @Override
    public boolean clearPlayerRole(UUID uuid) {
        return clearRoleResult;
    }

    @Override
    public String getPlayerRoleId(UUID uuid) {
        return playerRoleId;
    }

    @Override
    public Component getPlayerRoleDisplayName(UUID uuid) {
        return playerRoleDisplayName.apply(uuid);
    }

    @Override
    public boolean hasRole(UUID uuid) {
        return hasRoleResult;
    }

    @Override
    public Component getRoleDisplayName(String roleId) {
        return roleDisplayName.apply(roleId);
    }

    @Override
    public List<Component> getRoleDescription(String roleId) {
        return roleDescription.apply(roleId);
    }

    @Override
    public Material getRoleIcon(String roleId) {
        return roleIcon.apply(roleId);
    }

    @Override
    public Set<String> getAllRoleIds() {
        return allRoleIds;
    }

    @Override
    public List<RoleInfo> getRoles() {
        return roles;
    }
}
