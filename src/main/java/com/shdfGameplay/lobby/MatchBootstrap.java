package com.shdfGameplay.lobby;

import com.hydrokarbs.api.MiniGameApi;
import com.shdfGameplay.flow.SessionFlowPolicy;
import com.shdfGameplay.map.GameConfigs;
import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * 开局单闸:五门判定后 SessionRecipe 钉图→api.createSession→逐人 joinSession 托管;全工程唯一 createSession 调用点。
 *
 * <p>来源:04 §4.3 代码块/§4.7 开局门表;README 对齐注⑧适用;2.5 步骤5(:39)/关键实现要点;★23=16-路线图与验收.md:65。</p>
 */
public final class MatchBootstrap {

    /**
     * 开局结果:成功时 sessionId 非空;开局失败或部分成员被拒时 sessionId 为空,rejections 携带未过门原因或 join 被拒 rej.detail() 明细。
     */
    public record BootstrapResult(String sessionId, List<String> rejections) {
    }

    private final MiniGameApi api;
    private final GameConfigs games;
    private final SessionFlowPolicy flow;
    private final BooleanSupplier rolePresence;

    /**
     * 第五门『角色在场』经主类 onEnable(L0 装配点)构造的 BooleanSupplier 注入,本类只持 JDK 类型,不 import bridge.role(★23)。
     */
    public MatchBootstrap(MiniGameApi api, GameConfigs games, SessionFlowPolicy flow, BooleanSupplier rolePresence) {
        this.api = api;
        this.games = games;
        this.flow = flow;
        this.rolePresence = rolePresence;
    }

    // TODO(2.5): 五门=人数≥min-players / 票选(winner 或 rotation 兜底)/ api.maps() 含图 / api.mode(id) 在场(缺席=SEVERE)/ 角色在场=注入的 BooleanSupplier
    //             ——主类 onEnable(L0) 构造 ()->RoleBridge.facade().available() 注入(lambda 体住 L0),lobby 包禁止 import bridge.role
    //             (★23=16-路线图与验收.md:65 LayeringTest#onlyWrapperComponentTouchesBridge 只放行 bridge/role 与 component/role);
    //             2.5 恒真 lambda 占位,3.5 换真实现;MatchBootstrapGateTest 文本扫描冻结单闸 —— 出自 04 §4.7、2.5 步骤5(:39)
    // TODO(2.5): 托管时序铁律:先 createSession 后逐人 join;join 被拒 rej.detail() 提示且留大厅;join 成功立即移出大厅账本
    //             ——过门后 new SessionRecipe(cfg.modeId(), Optional.of(cfg.mapId()), List.of(), flowExtras);SessionInfo 经 var 承接不显式 import —— 出自 04 §4.3
    // TODO(2.5): flowExtras 本阶段=List.of(new EmptyArenaComponent.Spec(Duration.ofSeconds(10)))(空场 ~10s 解散,DissolveReturnTest 依赖),经 SessionFlowPolicy.recipeExtras() 取得 —— 出自 2.5:39 原文
    /** 开局单闸:candidates 为候选行(mode@map);全工程唯一 createSession 调用点。 */
    public BootstrapResult bootstrap(List<String> candidates) {
        return new BootstrapResult(null, List.of());
    }
}
