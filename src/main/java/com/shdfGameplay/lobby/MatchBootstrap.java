package com.shdfGameplay.lobby;

import com.hydrokarbs.api.HydrokarbsApi;
import com.hydrokarbs.api.SessionRecipe;
import com.hydrokarbs.engine.session.JoinResult;
import com.shdfGameplay.component.MatchConfigComponent;
import com.shdfGameplay.config.GameConfig;
import com.shdfGameplay.config.GameConfigManager;
import com.shdfGameplay.config.SpecificConfigParsers;
import com.shdfGameplay.lang.Lang;
import com.shdfGameplay.session.SessionRecipes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.logging.Logger;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

/**
 * 开局单闸:五门序判定(人数→票选→地图→模式→角色,04 §4.3 门表)全过后解析玩法专用配置,
 * 经 SessionRecipes 补挂口组装钉图配方(静态模板已带空场解散组件,严防双挂)再交框架建会话
 * (全工程唯一 createSession 调用点,SessionHandle 经 var 承接),后逐人句柄 join 托管;
 * join 被拒 rej.detail() 入 rejections 留大厅,join 成功立即经 ledgerRemove 回写移出大厅账本。
 * 开局失败节流归周期壳(本类不自旋);本插件无续局,对局结束即散场回大厅。
 * —— 出自 2.5 大厅逻辑(04-大厅模式 §4.3/§4.8);2.5 步骤5。
 */
public final class MatchBootstrap {

    /** 模式缺席门表口径(SEVERE=装配事故)的日志面。 */
    private static final Logger LOGGER = Logger.getLogger(MatchBootstrap.class.getName());

    /** 候选行分隔符(mode@map;id 约定不含 @,拆分取首个)。 */
    private static final char CANDIDATE_SEP = '@';

    /**
     * 开局结果:成功时 sessionId 非空;开局失败或部分成员被拒时 sessionId 为空,rejections 携带未过门原因、
     * 配置解析失败原因或 join 被拒 rej.detail() 明细。
     */
    public record BootstrapResult(String sessionId, List<String> rejections) {
    }

    private final HydrokarbsApi hk;
    private final LobbyVote vote;
    private final GameConfigManager games;
    private final SpecificConfigParsers parsers;
    private final SessionRecipes recipes;
    private final int minPlayers;
    private final BooleanSupplier rolePresence;
    private final Consumer<UUID> ledgerRemove;

    /**
     * @param hk           框架 API(maps()/mode() 门检与建会话托管;白名单类)
     * @param vote         大厅投票(票选门:计票或轮换兜底)
     * @param games        图配置管理器(过门后按 modeId+mapId 回查 GameConfig)
     * @param parsers      玩法专用配置解析器登记簿(开局前解析,带病拒收)
     * @param recipes      会话配方登记簿(pinned 补挂口:钉图+追加本局配置组件)
     * @param minPlayers   开局最少人数(人数门)
     * @param rolePresence 第五门『角色在场』经主类 onEnable(L0 装配点)构造的 BooleanSupplier 注入,
     *                     本类只持 JDK 类型,不 import bridge.role(★23)
     * @param ledgerRemove join 成功回写账本口(=lobby::leave,反向引用零循环依赖)
     */
    public MatchBootstrap(final HydrokarbsApi hk, final LobbyVote vote, final GameConfigManager games,
                          final SpecificConfigParsers parsers, final SessionRecipes recipes, final int minPlayers,
                          final BooleanSupplier rolePresence, final Consumer<UUID> ledgerRemove) {
        this.hk = hk;
        this.vote = vote;
        this.games = games;
        this.parsers = parsers;
        this.recipes = recipes;
        this.minPlayers = minPlayers;
        this.rolePresence = rolePresence;
        this.ledgerRemove = ledgerRemove;
    }

    /**
     * 开局单闸:五门任一不过即 rejection 全员留大厅(不建会话);全过后 games.find 回查图配置→
     * 解析器在场则 parseRawGameConfig(IllegalStateException 响亮捕作 rejection,带病配置开局前拒收,
     * 04 §4.3「人读可恢复」)→ SessionRecipes.pinned 组装钉图配方→框架建会话→逐人 handle.join。
     *
     * @param participants 本局参与者(周期壳从 lobby.present() 采集)
     * @return 开局结果(见 {@link BootstrapResult})
     */
    public BootstrapResult bootstrap(final Collection<UUID> participants) {
        // 门一·人数:不足继续等(04 §4.3 门表「继续等/重新起票」口径)
        if (participants.size() < this.minPlayers) {
            return new BootstrapResult(null,
                    List.of(Lang.plain("lobby.bootstrap.min_players",
                            Placeholder.unparsed("present", String.valueOf(participants.size())),
                            Placeholder.unparsed("required", String.valueOf(this.minPlayers)))));
        }
        // 门二·票选:计票多数胜,零票走轮换兜底;两者皆空=本轮不开局重新起票
        final Optional<String> chosen = this.vote.tally().or(this.vote::nextFallback);
        if (chosen.isEmpty()) {
            return new BootstrapResult(null, List.of(Lang.plain("lobby.bootstrap.no_candidate")));
        }
        final String candidate = chosen.get();
        final int sep = candidate.indexOf(CANDIDATE_SEP);
        final String modeId = candidate.substring(0, sep);
        final String mapId = candidate.substring(sep + 1);
        // 门三·地图:目标图未注册=摘除候选重收票
        boolean mapPresent = false;
        for (final var info : this.hk.maps()) {
            if (info.id().equals(mapId)) {
                mapPresent = true;
                break;
            }
        }
        if (!mapPresent) {
            // 门表自愈落地:摘除该失败候选并清票本重收(04 §4.3 门表「摘除候选重收票」——恢复动作随拒收真实执行,非仅文案)
            this.vote.withdraw(candidate);
            return new BootstrapResult(null,
                    List.of(Lang.plain("lobby.bootstrap.map_missing", Placeholder.unparsed("candidate", candidate))));
        }
        // 门四·模式:缺席属装配事故,SEVERE 记日志并不开局(不建会话)
        if (this.hk.mode(modeId).isEmpty()) {
            LOGGER.severe(Lang.plain("log.lobby.mode_missing_severe", Placeholder.unparsed("mode", modeId)));
            return new BootstrapResult(null,
                    List.of(Lang.plain("lobby.bootstrap.mode_missing", Placeholder.unparsed("mode", modeId))));
        }
        // 门五·角色在场(按玩法):缺席摘除该玩法候选,大厅照常(2.5 注入恒真占位,按玩法判定的真实现随 3.5 角色玩法
        // 换入——04 §4.7 不陪葬:角色插件缺席不得陪葬无角色需求的模式开局)
        if (!this.rolePresence.getAsBoolean()) {
            // 门表自愈落地:摘除该玩法全部候选并清票本(04 §4.3 门表「摘除该玩法候选,大厅照常」)
            this.vote.withdrawMode(modeId);
            return new BootstrapResult(null,
                    List.of(Lang.plain("lobby.bootstrap.role_missing", Placeholder.unparsed("mode", modeId))));
        }
        // 过门回查图配置:票选候选应恒在标记点名录内,缺席即不开局(防御口径)
        final Optional<GameConfig> config = this.games.find(modeId, mapId);
        if (config.isEmpty()) {
            return new BootstrapResult(null,
                    List.of(Lang.plain("lobby.bootstrap.config_missing", Placeholder.unparsed("candidate", candidate))));
        }
        // 玩法专用配置解析:带病响亮捕作 rejection,全员留大厅不开局(数据面软失败,人读可恢复)
        Record parsed = null;
        final var parser = this.parsers.find(modeId);
        if (parser.isPresent()) {
            try {
                parsed = parser.get().parseRawGameConfig(config.get());
            } catch (final IllegalStateException e) {
                return new BootstrapResult(null,
                        List.of(Lang.plain("lobby.bootstrap.parse_failed",
                                Placeholder.unparsed("reason", String.valueOf(e.getMessage())))));
            }
        }
        // 组装钉图配方:静态模板(空场解散组件在前)+追加本局配置组件(在后),严防组件双挂
        final SessionRecipe recipe = this.recipes.pinned(modeId, mapId,
                new MatchConfigComponent.Spec(modeId, mapId, parsed));
        // 全工程唯一建会话调用点;未登记模式/坏组件由 pinned/createSession 响亮抛(装配期问题,启动就该炸)
        final var handle = this.hk.createSession(recipe);
        // 逐人托管:被拒者 rej.detail() 入 rejections 留大厅;join 成功立即移出大厅账本(此后生灭归框架名册)
        final List<String> rejections = new ArrayList<>();
        for (final UUID participant : participants) {
            if (handle.join(participant) instanceof JoinResult.Rejected rej) {
                rejections.add(Lang.plain("lobby.bootstrap.join_rejected",
                        Placeholder.unparsed("player", participant.toString()),
                        Placeholder.unparsed("detail", String.valueOf(rej.detail()))));
            } else {
                this.ledgerRemove.accept(participant);
            }
        }
        return new BootstrapResult(handle.sessionId(), List.copyOf(rejections));
    }
}
