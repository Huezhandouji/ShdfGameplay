package com.shdfGameplay.lobby;

import com.hydrokarbs.api.EventSubscription;
import com.hydrokarbs.api.HydrokarbsApi;
import com.hydrokarbs.api.SessionEventView;
import com.hydrokarbs.api.SessionHandle;
import com.hydrokarbs.api.SessionRecipe;
import com.hydrokarbs.api.view.MapInfo;
import com.hydrokarbs.api.view.MatchModeInfo;
import com.hydrokarbs.engine.component.SessionComponent;
import com.hydrokarbs.engine.mode.MatchModeProvider;
import com.hydrokarbs.map.MapDefinition;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import org.bukkit.plugin.Plugin;

/**
 * HydrokarbsApi 测试桩基类:sessions()/leaveSession()/sessionOf() 直通(GameFlow/LobbyManager 用面,
 * leaveSession 记入共享调用序日志供「先离开后传送」次序断言,sessionOf 按预置映射返回);
 * createSession/maps()/mode() 为预置驱动面(2.5 开局单闸测试:setCreateResult/setMaps/putMode 预置,
 * maps()/mode() 未预置保持抛);其余口抛 UnsupportedOperationException。
 * 测试代码不受 lobby 主源集框架 import 白名单约束。
 * —— 出自 本次任务(shdf-launchstop-流程开关)设计文档 §9 测试面构造;
 * 预置面增量出自 2.5 大厅逻辑(04-大厅模式 §4.3)测试面。
 */
final class StubHydrokarbsApi implements HydrokarbsApi {

    private final List<String> callLog;
    private List<SessionHandle> sessions = List.of();
    private final Map<UUID, SessionHandle> sessionByPlayer = new HashMap<>();
    private SessionHandle createResult;
    private SessionRecipe lastCreateRecipe;
    private List<MapInfo> maps;
    private final Map<String, MatchModeInfo> modesById = new HashMap<>();
    private boolean modesPreset;

    /** @param callLog 与 RecordingTeleport 共享的调用序日志(null=不记录) */
    StubHydrokarbsApi(final List<String> callLog) {
        this.callLog = callLog;
    }

    /** 预置/切换会话集(用例间切换假会话,如残留名单用例的旧/新周期)。 */
    void setSessions(final List<SessionHandle> sessions) {
        this.sessions = List.copyOf(sessions);
    }

    /** 预置玩家→所在会话映射(LobbyManager join 自查:在会话者不接回;未预置=不在任何会话)。 */
    void putSessionOf(final UUID player, final SessionHandle handle) {
        this.sessionByPlayer.put(player, handle);
    }

    /** 预置 createSession 返回句柄(MatchBootstrap 托管路径用;未预置即被调则保持抛)。 */
    void setCreateResult(final SessionHandle handle) {
        this.createResult = handle;
    }

    /** 最近一次 createSession 收到的配方(null=从未被调,零调用断言用);断言钉图与组件追加序。 */
    SessionRecipe lastCreateRecipe() {
        return this.lastCreateRecipe;
    }

    /** 预置 maps() 返回集(地图门判定;预置空集=无已注册图)。 */
    void setMaps(final List<MapInfo> maps) {
        this.maps = List.copyOf(maps);
    }

    /** 预置已注册模式(mode 门判定;首次预置激活 mode() 面,未预置 id 查询返空=模式缺席)。 */
    void putMode(final String modeId, final MatchModeInfo info) {
        this.modesPreset = true;
        this.modesById.put(modeId, info);
    }

    @Override
    public List<SessionHandle> sessions() {
        return this.sessions;
    }

    @Override
    public boolean leaveSession(final UUID player) {
        if (this.callLog != null) {
            this.callLog.add("leave:" + player);
        }
        return true;
    }

    @Override
    public Optional<SessionHandle> sessionOf(final UUID player) {
        return Optional.ofNullable(this.sessionByPlayer.get(player));
    }

    // —— 预置驱动面(2.5 开局单闸测试:createSession 记配方返预置;maps()/mode() 预置后正常应答) ——

    @Override
    public SessionHandle createSession(final SessionRecipe recipe) {
        this.lastCreateRecipe = recipe;
        if (this.createResult == null) {
            throw new UnsupportedOperationException("createSession 未预置返回值(setCreateResult)");
        }
        return this.createResult;
    }

    @Override
    public List<MapInfo> maps() {
        if (this.maps == null) {
            throw new UnsupportedOperationException("maps() 未预置(setMaps)");
        }
        return this.maps;
    }

    @Override
    public Optional<MatchModeInfo> mode(final String modeId) {
        if (!this.modesPreset) {
            throw new UnsupportedOperationException("mode() 未预置(putMode)");
        }
        return Optional.ofNullable(this.modesById.get(modeId));
    }

    // —— 以下口本组测试不经,默认拒绝 ——

    @Override
    public Optional<SessionHandle> session(final String sessionId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public EventSubscription subscribeSessionEvents(final Consumer<SessionEventView> listener) {
        throw new UnsupportedOperationException();
    }

    @Override
    public MapInfo registerMap(final Plugin plugin, final MapDefinition definition) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void unregisterMap(final Plugin plugin, final String mapId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public MatchModeInfo registerMode(final Plugin plugin, final MatchModeProvider provider) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void unregisterMode(final Plugin plugin, final String modeId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void registerSessionComponents(final Plugin plugin, final List<SessionComponent.Specification<?>> specs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void unregisterSessionComponents(final Plugin plugin) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<MatchModeInfo> modes() {
        throw new UnsupportedOperationException();
    }
}
