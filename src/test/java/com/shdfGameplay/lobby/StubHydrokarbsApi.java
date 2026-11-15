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
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import org.bukkit.plugin.Plugin;

/**
 * HydrokarbsApi 测试桩基类:除 sessions()/leaveSession() 外全部口抛 UnsupportedOperationException
 * (GameFlow 只经这两个面);leaveSession 记入共享调用序日志(与 RecordingTeleport 同一日志,
 * 供「先离开后传送」次序断言)。测试代码不受 lobby 主源集框架 import 白名单约束。
 * —— 出自 本次任务(shdf-launchstop-流程开关)设计文档 §9 测试面构造。
 */
final class StubHydrokarbsApi implements HydrokarbsApi {

    private final List<String> callLog;
    private List<SessionHandle> sessions = List.of();

    /** @param callLog 与 RecordingTeleport 共享的调用序日志(null=不记录) */
    StubHydrokarbsApi(final List<String> callLog) {
        this.callLog = callLog;
    }

    /** 预置/切换会话集(用例间切换假会话,如残留名单用例的旧/新周期)。 */
    void setSessions(final List<SessionHandle> sessions) {
        this.sessions = List.copyOf(sessions);
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

    // —— 以下口本组测试不经,默认拒绝 ——

    @Override
    public Optional<SessionHandle> sessionOf(final UUID player) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<SessionHandle> session(final String sessionId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public SessionHandle createSession(final SessionRecipe recipe) {
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

    @Override
    public List<MapInfo> maps() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<MatchModeInfo> mode(final String modeId) {
        throw new UnsupportedOperationException();
    }
}
