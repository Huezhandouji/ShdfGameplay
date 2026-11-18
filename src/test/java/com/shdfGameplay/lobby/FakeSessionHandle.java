package com.shdfGameplay.lobby;

import com.hydrokarbs.api.SessionHandle;
import com.hydrokarbs.api.view.SessionInfo;
import com.hydrokarbs.engine.session.Handoff;
import com.hydrokarbs.engine.session.JoinResult;
import com.hydrokarbs.engine.session.LeaveReason;
import com.hydrokarbs.engine.session.MatchModeSelection;
import com.hydrokarbs.engine.session.SessionId;
import com.hydrokarbs.engine.session.SwitchResult;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * SessionHandle 测试假件:sessionId()+info() 按预置返回(info() 出 SessionInfo 公开构造,
 * members 即预置名册快照),info() 亦可预置抛错(容错用例);join 按预置映射返回
 * (未预置缺省返 Joined(自身 SessionId),2.5 开局单闸托管路径),leave/switchMode 不经,默认拒绝。
 * —— 出自 本次任务(shdf-launchstop-流程开关)设计文档 §9 测试面构造;
 * join 预置面增量出自 2.5 大厅逻辑(04-大厅模式 §4.3)测试面。
 */
final class FakeSessionHandle implements SessionHandle {

    private final String sessionId;
    private final List<UUID> members;
    private final RuntimeException infoFailure;
    private final Map<UUID, JoinResult> joinResults = new HashMap<>();

    FakeSessionHandle(final String sessionId, final List<UUID> members) {
        this(sessionId, members, null);
    }

    /** info() 抛错构造(单会话处理异常容错用例)。 */
    FakeSessionHandle(final String sessionId, final List<UUID> members, final RuntimeException infoFailure) {
        this.sessionId = sessionId;
        this.members = List.copyOf(members);
        this.infoFailure = infoFailure;
    }

    @Override
    public String sessionId() {
        return this.sessionId;
    }

    @Override
    public Optional<SessionInfo> info() {
        if (this.infoFailure != null) {
            throw this.infoFailure;
        }
        return Optional.of(new SessionInfo(this.sessionId, "bosswar", "example", this.members, "RUNNING"));
    }

    /** 预置某玩家的 join 结果(MatchBootstrap 托管路径:被拒/放行分叉断言);未预置者缺省放行。 */
    void putJoinResult(final UUID player, final JoinResult result) {
        this.joinResults.put(player, result);
    }

    @Override
    public JoinResult join(final UUID player) {
        final JoinResult preset = this.joinResults.get(player);
        return preset != null ? preset : new JoinResult.Joined(new SessionId(this.sessionId));
    }

    // —— 以下口 GameFlow 不经,默认拒绝 ——

    @Override
    public void leave(final UUID player, final LeaveReason reason) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletableFuture<SwitchResult> switchMode(final MatchModeSelection selection, final Handoff handoff) {
        throw new UnsupportedOperationException();
    }
}
