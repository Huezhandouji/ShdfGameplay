package com.shdfGameplay.lobby;

import com.hydrokarbs.api.HydrokarbsApi;
import com.hydrokarbs.component.session.EmptyArenaComponent;
import com.shdfGameplay.component.MatchConfigComponent;
import com.shdfGameplay.config.GameConfigManager;
import com.shdfGameplay.config.SpecificConfigParsers;
import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * 开局单闸:五门判定后解析玩法专用配置→SessionRecipe 钉图+配置组件+空场解散组件→api.createSession(返 SessionHandle)
 * →逐人句柄 join 托管;全工程唯一 createSession 调用点。
 * 本插件无续局:单场会话,对局结束即散场回大厅(EndResult 无去向回落 limbo→宿主散场引导,见 MatchEndReturn)。
 */
public final class MatchBootstrap {

    /**
     * 开局结果:成功时 sessionId 非空;开局失败或部分成员被拒时 sessionId 为空,rejections 携带未过门原因、
     * 配置解析失败原因或 join 被拒 rej.detail() 明细。
     */
    public record BootstrapResult(String sessionId, List<String> rejections) {
    }

    private final HydrokarbsApi hk;
    private final GameConfigManager games;
    private final SpecificConfigParsers parsers;
    private final BooleanSupplier rolePresence;

    /**
     * 第五门『角色在场』经主类 onEnable(L0 装配点)构造的 BooleanSupplier 注入,本类只持 JDK 类型,不 import bridge.role(★23)。
     * SpecificConfigParsers 由 L0 构造并逐模式注册后注入(玩法专用配置的解析器登记簿,lobby 零 mode 包 import)。
     */
    public MatchBootstrap(HydrokarbsApi hk, GameConfigManager games, SpecificConfigParsers parsers,
                          BooleanSupplier rolePresence) {
        this.hk = hk;
        this.games = games;
        this.parsers = parsers;
        this.rolePresence = rolePresence;
    }

    // TODO(2.5): 五门=人数≥min-players / 票选(winner 或轮换兜底)/ api.maps() 含图 / api.mode(id) 在场(缺席=SEVERE)/ 角色在场=注入的 BooleanSupplier
    //             ——主类 onEnable(L0) 构造 ()->RoleBridge.facade().available() 注入(lambda 体住 L0),lobby 包禁止 import bridge.role
    //             (★23=16-路线图与验收.md:65 LayeringTest#onlyWrapperComponentTouchesBridge 只放行 bridge/role 与 component/role);
    //             2.5 恒真 lambda 占位,3.5 换真实现;MatchBootstrapGateTest 文本扫描冻结单闸 —— 出自 04 §4.7、2.5 步骤5(:39)
    // TODO(2.5): 托管时序铁律:先 createSession(★v1.15 返 SessionHandle)后逐人 handle.join(u);join 被拒 rej.detail() 提示且留大厅;join 成功立即移出大厅账本
    //             ——过门后 games.find(modeId,mapId) 取 GameConfig → parsers.find(modeId) 有则 parseRawGameConfig(cfg)
    //             (IllegalStateException 响亮捕作 rejection:带病配置开局前拒收不进会话,全员留大厅) —— 出自 04 §4.3;配置传递方案(会话组件,替代 MapDefinition.metadata 词料)
    // TODO(2.5): SessionRecipe 构造:new SessionRecipe(cfg.modeId(), Optional.of(cfg.mapId()), List.of(), extras)——
    //             extras=List.of(new MatchConfigComponent.Spec(cfg.modeId(), cfg.mapId(), 解析结果或 null)(本局玩法配置会话组件,
    //             跨模式切换存活;无解析器模式如 idle 为 null,组件仍恒在),
    //             new EmptyArenaComponent.Spec(Duration.ofSeconds(10))(空场 ~10s 解散,DissolveReturnTest 依赖));
    //             SessionHandle 经 var 承接不显式 import,sessionId 取 handle.sessionId() —— 出自 04 §4.3;2.5:39;SessionRecipe.extraComponents(jar 已核)
    /** 开局单闸:candidates 为候选行(mode@map);全工程唯一 createSession 调用点。 */
    public BootstrapResult bootstrap(List<String> candidates) {
        return new BootstrapResult(null, List.of());
    }
}
