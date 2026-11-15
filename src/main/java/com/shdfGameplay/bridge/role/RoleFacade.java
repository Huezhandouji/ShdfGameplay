package com.shdfGameplay.bridge.role;

import com.shadowHunterRolesPlugin.api.RoleAPI;
import com.shadowHunterRolesPlugin.api.RoleEventSubscription;
import com.shadowHunterRolesPlugin.api.RoleInfo;
import com.shadowHunterRolesPlugin.api.event.RoleClearEvent;
import com.shadowHunterRolesPlugin.api.event.RoleDeathEvent;
import com.shadowHunterRolesPlugin.api.event.RoleSetEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 角色系统防腐层唯一类:全工程唯一允许 import 角色插件之处,公开面照自有名 1:1 覆盖 RoleAPI 冻结面 15 方法。
 * 纪律:签名只用 JDK/Bukkit/Adventure 类型(升级只改本文件);全部方法只准主线程调用;
 * 懒解析 null 也缓存、失效只经 {@link #invalidate()};缺席读回空值/写回 false,全类不抛。
 */
public final class RoleFacade {

    /** 懒解析缓存(可为 null;null 也缓存——插件缺席时不该每次调用都打一遍 ServicesManager,08 §8.8)。 */
    private RoleAPI api;

    /** 懒解析完成标记:null 也是合法缓存态,须与「未解析」区分(失效只经 invalidate(),08 §8.8)。 */
    private boolean resolved;

    /** 事件订阅在册表(handler 持久在册直到句柄 close;缺席期登记不弃,令牌随角色插件 reload 换代)。 */
    private final List<Registration> registrations = new ArrayList<>();

    /** API 获取缝:生产默认经 Bukkit.getServicesManager().load(RoleAPI.class) 唯一出口(08 §8.1/§8.8),测试注入假 Supplier(3.1 步骤4 唯一测试缝)。 */
    private final Supplier<RoleAPI> lookup;

    /** 阵营组件 id(08 §8.10(二)表头:阵营无 API 成员,一律经组件操作面 componentId="faction")。 */
    private static final String FACTION_COMPONENT_ID = "faction";

    /** 阵营动词 token(08 §8.10(二);逐字来自角色系统 FactionComponent 动词表,enemy_in_range 不收)。 */
    private static final String VERB_FACTION_CURRENT = "faction";
    private static final String VERB_FACTION_DECLARED = "declared";
    private static final String VERB_FACTION_HOSTILE = "hostile";
    private static final String VERB_FACTION_SET = "set";
    private static final String VERB_FACTION_RESET = "reset";

    /** 在册条目:handler+当前订阅令牌(缺席期为 null;令牌类型不出本文件)。 */
    static final class Registration {

        final Consumer<RoleSignal> handler;
        RoleEventSubscription token;

        Registration(Consumer<RoleSignal> handler) {
            this.handler = handler;
        }
    }

    /** 生产构造:懒解析——构造期不取 API(角色插件缺席时构造不炸,08 §8.8;§14.22 规避)。 */
    public RoleFacade() {
        this(() -> Bukkit.getServicesManager().load(RoleAPI.class));
    }

    /** 唯一测试缝:注入假 API 查找(3.1 步骤4;生产路径零改变)。 */
    RoleFacade(Supplier<RoleAPI> lookup) {
        this.lookup = lookup;
    }

    /**
     * 懒解析统一入口:缓存命中即返(含 null 缓存态),未命中经 lookup 取并缓存;
     * 解析出新实例时对在册 handler 全量补订(订阅不跨角色插件 reload 存活)。
     */
    private RoleAPI ensureApi() {
        if (!resolved) {
            RoleAPI found = lookup.get();      // 获取只经 ServicesManager 唯一出口,不绕开直连角色系统内部类(08 §8.1/§8.8)
            api = found;                       // null 也缓存:缺席不打 ServicesManager 一遍遍(08 §8.8)
            resolved = true;
            if (found != null) {
                resubscribeAll();              // 新实例 ⇒ 在册 handler 全量补订 —— 出自 角色插件 RoleAPI#subscribeEvents 生命周期合约;08-角色桥接 §8.8
            }
        }
        return api;
    }

    /** 桥接生命周期方法(不计入 15 方法对照表,3.1:103):清空 api 缓存并恢复事件订阅;只由服务注册/注销事件驱动。 */
    public void invalidate() {
        for (Registration reg : registrations) {
            unsubscribeQuietly(reg.token);     // ①旧令牌随角色插件 disable 作废,退订无害幂等
            reg.token = null;
        }
        api = null;                            // ②清空 api 缓存(null 也缓存语义不变)
        resolved = false;
        ensureApi();                           // ③立即懒解析一次:取到新实例则全量补订,事件流只断在角色插件 disable→enable 窗口内
    }

    // ── 事件订阅面(第 15 方法:角色插件正式事件流,宿主侧死亡自探测的替代)──────────

    /**
     * → api.subscribeEvents(Consumer&lt;Event&gt;):订阅角色插件事件流并翻译为宿主词汇 {@link RoleSignal}——
     * 翻译消费(instanceof RoleSetEvent/RoleClearEvent/RoleDeathEvent 三已知种→按载荷构造 RoleSignal;未知事件种类
     * 静默跳过,待 Kind 扩枚举)只住本文件,角色插件事件类型不泄漏;★主线程同步送达(分发即角色插件发布点,
     * DEATH 在其清角之前)。缺席时登记在册不抛、恢复后自动补订;handler 为 null 时不登记(全类不抛)。
     */
    public RoleEventHandle subscribeRoleEvents(Consumer<RoleSignal> handler) {
        if (handler == null) {
            return new RoleEventHandle(this, null);    // 不登记不抛
        }
        Registration reg = new Registration(handler);
        registrations.add(reg);                        // 缺席期也先在册登记(缺席期只登记不订阅)
        ensureApi();                                   // 解析出新实例则内里已全量补订(含本条)
        subscribeIfAbsent(reg);                        // 缓存命中路径:本条尚无令牌则立即订阅
        return new RoleEventHandle(this, reg);
    }

    /** 桥接生命周期方法(不计入 15 方法对照表,3.1:103):角色系统在场探测(ensureApi() != null 口径,3.3 能力探测口)。 */
    public boolean available() {
        return ensureApi() != null;
    }

    // ── 查询:角色目录(只读,主线程)───────────────────────────────

    /** → api.getAllRoleIds();只读;3.2 起被 RoleCatalog 缓存。 */
    public Set<String> knownRoleIds() {
        RoleAPI a = ensureApi();
        return a == null ? Set.of() : a.getAllRoleIds();
    }

    /** → api.getRoles() 转 RoleDescriptor;目录不含阵营(08 §8.2)。 */
    public List<RoleDescriptor> catalog() {
        RoleAPI a = ensureApi();
        List<RoleInfo> infos = a == null ? null : a.getRoles();
        if (infos == null) {
            return List.of();
        }
        List<RoleDescriptor> out = new ArrayList<>(infos.size());
        for (RoleInfo info : infos) {
            // RoleInfo→RoleDescriptor 现转只住本方法(3.1 步骤5:转换只住本层;缓存归 3.2 RoleCatalog,08 §8.5)
            out.add(new RoleDescriptor(info.id(), info.displayName(), info.description(),
                    info.icon() == null ? Material.AIR : info.icon()));
        }
        return out;
    }

    /** → api.isValidRoleId(String);只读。 */
    public boolean isValidRole(String roleId) {
        RoleAPI a = ensureApi();
        return a != null && a.isValidRoleId(roleId);
    }

    /** → api.getRoleDisplayName(String);只读,GUI 渲染(判缺失要同时判 AIR 与空 Component,3.1:107)。 */
    public Component roleDisplayName(String roleId) {
        RoleAPI a = ensureApi();
        return a == null ? null : a.getRoleDisplayName(roleId);
    }

    /** → api.getRoleDescription(String);只读,GUI 渲染。 */
    public List<Component> roleDescription(String roleId) {
        RoleAPI a = ensureApi();
        return a == null ? List.of() : a.getRoleDescription(roleId);
    }

    /** → api.getRoleIcon(String);★ 缺失回 Material.AIR 而非 null(08 §8.2)。 */
    public Material roleIcon(String roleId) {
        RoleAPI a = ensureApi();
        if (a == null) {
            return Material.AIR;
        }
        Material icon = a.getRoleIcon(roleId);
        return icon == null ? Material.AIR : icon;
    }

    // ── 查询:玩家当前角色(不缓存,角色系统权威,08 §8.5 表第 1 行)──

    /** → api.getPlayerRoleId(UUID);无则 null 原样透传(Optional 化在上层,§14.14)。 */
    public String roleIdOf(UUID uuid) {
        RoleAPI a = ensureApi();
        return a == null ? null : a.getPlayerRoleId(uuid);
    }

    /** → api.hasRole(UUID);只读。 */
    public boolean hasRole(UUID uuid) {
        RoleAPI a = ensureApi();
        return a != null && a.hasRole(uuid);
    }

    /** → api.getPlayerRoleDisplayName(UUID);只读。 */
    public Component playerRoleDisplayName(UUID uuid) {
        RoleAPI a = ensureApi();
        return a == null ? null : a.getPlayerRoleDisplayName(uuid);
    }

    // ── 阵营五动词:角色系统无阵营 API,一律经组件操作面(componentId="faction",08 §8.10(二))──

    /** 该玩家自己那份实例的当前阵营(动词 faction);无实例/读不到 ⇒ RoleFaction.UNKNOWN。 */
    public RoleFaction playerFaction(UUID uuid) {
        return RoleFaction.parse(operate(uuid, FACTION_COMPONENT_ID, VERB_FACTION_CURRENT));
    }

    /** 该玩家当前所持角色声明的默认阵营(动词 declared);无实例 ⇒ UNKNOWN;★ 角色目录里读不到此值。 */
    public RoleFaction declaredFaction(UUID uuid) {
        return RoleFaction.parse(operate(uuid, FACTION_COMPONENT_ID, VERB_FACTION_DECLARED));
    }

    /** ★主线程。改写该玩家阵营(动词 set <名>,只影响该玩家);RoleFaction.OTHER ⇒ false 且不发起操作。 */
    public boolean setPlayerFaction(UUID uuid, RoleFaction faction) {
        if (faction == null) {
            return false;                      // 防御:无名可写即拒绝且不发起操作(全类不抛)
        }
        String wireName = faction.wireName();
        if (wireName == null) {
            return false;                      // OTHER 无对应阵营名 ⇒ false 且不发起 operate(08 §8.10(二) set 行)
        }
        return operate(uuid, FACTION_COMPONENT_ID, VERB_FACTION_SET + " " + wireName) != null;    // 回非空 ⇒ true,null ⇒ false
    }

    /** ★主线程。复位为声明值(动词 reset),幂等。 */
    public boolean resetPlayerFaction(UUID uuid) {
        return operate(uuid, FACTION_COMPONENT_ID, VERB_FACTION_RESET) != null;    // 回复位后阵营名(非空)⇒ true
    }

    /** 判敌(★非对称:动词 hostile <b> 在发起方 a 的实例上执行;发起方无实例 ⇒ true;任一参数 null ⇒ false)。 */
    public boolean areHostile(UUID a, UUID b) {
        if (a == null || b == null) {
            return false;                      // 任一参数 null ⇒ false(08 §8.10(二)语义要点行)
        }
        if (ensureApi() == null) {
            return true;                       // 角色插件缺席=发起方必无实例 ⇒「没有阵营 ⇒ 敌人」(08 §8.2/§8.10(二))
        }
        String raw = operate(a, FACTION_COMPONENT_ID, VERB_FACTION_HOSTILE + " " + b);
        if (raw == null) {
            return true;                       // 发起方 a 无实例 ⇒ true(08 §8.2;角色系统同口径 FactionComponent:35)
        }
        return "true".equals(raw);             // "true"⇒true;"false"/意外串 ⇒ false(意外串=协议漂移)
    }

    // ── 设置 / 清除(★主线程:内部重建热键栏物品并写背包)───────────

    /** → api.setPlayerRole(UUID,String);★主线程。 */
    public boolean setRole(UUID uuid, String roleId) {
        RoleAPI a = ensureApi();
        return a != null && a.setPlayerRole(uuid, roleId);
    }

    /** → api.clearPlayerRole(UUID);★主线程;幂等——清场单闸 RoleComponent.stop 全员清角依赖此点(08 §8.2)。 */
    public boolean clearRole(UUID uuid) {
        RoleAPI a = ensureApi();
        return a != null && a.clearPlayerRole(uuid);
    }

    // ── 击杀归属(§14.4)────────────────────────────────────────────

    /** → api.getLastDamagerUuid(Player);击杀归属唯一正解,禁 player.getKiller();参数必须是 Player(禁加 LivingEntity 重载)。 */
    public UUID lastDamagerUuid(Player victim) {
        RoleAPI a = ensureApi();
        return a == null ? null : a.getLastDamagerUuid(victim);
    }

    /** → api.getLastDamager(Player);备用(需要 Player 对象而非 UUID 时,08 §8.10(一)第 13 行)。 */
    public Player lastDamager(Player victim) {
        RoleAPI a = ensureApi();
        return a == null ? null : a.getLastDamager(victim);
    }

    // ── 角色数据操作原语(字符串协议出口)───────────────────────────

    /** → api.executeComponentOperation(UUID,String,String);★主线程;null 有歧义,本层原样透传不解释(Optional 化归 RoleStateAccess,§14.14)。 */
    public String operate(UUID uuid, String componentId, String payload) {
        RoleAPI a = ensureApi();
        return a == null ? null : a.executeComponentOperation(uuid, componentId, payload);
    }

    // ── 订阅簿记(全部私有:角色插件令牌与翻译消费不出本文件)─────────

    /** 句柄 close 落点:移除在册条目并退订其当前令牌(幂等;空句柄 reg=null 无事可做)。 */
    void removeRegistration(Registration reg) {
        if (reg == null || !registrations.remove(reg)) {
            return;                            // 已 close 过/空句柄:重复 close 无害
        }
        unsubscribeQuietly(reg.token);
        reg.token = null;
    }

    /** 单条补订:api 在场且尚无令牌时经它订阅(翻译消费住本文件)。 */
    private void subscribeIfAbsent(Registration reg) {
        if (api == null || reg.token != null) {
            return;
        }
        reg.token = api.subscribeEvents(event -> translate(reg.handler, event));
    }

    /** 全量补订:懒解析出新实例时对在册 handler 逐条订阅。 */
    private void resubscribeAll() {
        for (Registration reg : registrations) {
            subscribeIfAbsent(reg);
        }
    }

    /** 事件翻译:三已知种按载荷(playerUuid/roleId)构造 RoleSignal,未知事件种类静默跳过(待 Kind 扩枚举)。 */
    private static void translate(Consumer<RoleSignal> handler, Event event) {
        if (event instanceof RoleSetEvent set) {
            handler.accept(new RoleSignal(RoleSignal.Kind.SET, set.getPlayerUuid(), set.getRoleId()));
        } else if (event instanceof RoleClearEvent clear) {
            handler.accept(new RoleSignal(RoleSignal.Kind.CLEAR, clear.getPlayerUuid(), clear.getRoleId()));
        } else if (event instanceof RoleDeathEvent death) {
            handler.accept(new RoleSignal(RoleSignal.Kind.DEATH, death.getPlayerUuid(), death.getRoleId()));
        }
    }

    /** 退订不外抛:作废令牌无害幂等,角色插件侧异常也吞掉(全类不抛,离线测试安全)。 */
    private static void unsubscribeQuietly(RoleEventSubscription token) {
        if (token == null) {
            return;
        }
        try {
            token.unsubscribe();
        } catch (RuntimeException tolerated) {
            // 退订作废令牌无害幂等 —— 出自 角色插件 RoleAPI#subscribeEvents javadoc
        }
    }
}
