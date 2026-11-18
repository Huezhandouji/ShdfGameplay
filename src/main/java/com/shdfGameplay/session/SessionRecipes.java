package com.shdfGameplay.session;

import com.hydrokarbs.api.SessionRecipe;
import com.shdfGameplay.component.MatchConfigComponent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 会话配方注册处:modeId(mode_config/ 目录名)→ {@link SessionRecipe} 的进程级登记簿。
 * 只管登记与回查,配方构造住调用方(主类 L0 装配点):登记簿存配方静态部分(空场解散组件等模式级固定件);
 * 开局期才知的钉图(initialMapId)与 {@code MatchConfigComponent.Spec} 经 {@link #pinned} 补挂成一次性开局配方,
 * 不回写登记簿。注册/注销由主类 L0 装配点显式完成(无 SPI);注册集与 L0 registerMode 模式集一致,
 * tdm/bomb 配方随 5.1/6.1 模式上线补登,与 SpecificConfigParsers(bosswar 随 6.3)同口径。
 * —— 补挂口出自 2.5 大厅逻辑(04-大厅模式 §4.3 钉图要点)。
 */
public final class SessionRecipes {

    private final Map<String, SessionRecipe> recipes = new ConcurrentHashMap<>();

    /**
     * 登记某模式的会话配方(重复 id 整体替换)。
     *
     * @param modeId 模式 id(注册表唯一键,与 mode_config/ 目录名一致)
     * @param recipe 该模式的会话配方(静态模板,由调用方构造)
     */
    public void register(final String modeId, final SessionRecipe recipe) {
        Objects.requireNonNull(modeId, "modeId");
        Objects.requireNonNull(recipe, "recipe");
        this.recipes.put(modeId, recipe);
    }

    /**
     * 按模式 id 取会话配方。
     *
     * @param modeId 模式 id(mode_config/ 目录名)
     * @return 该模式的配方;未注册为空(L0 未 registerMode 的模式不应查到)
     */
    public Optional<SessionRecipe> find(final String modeId) {
        return Optional.ofNullable(this.recipes.get(modeId));
    }

    /**
     * 开局补挂口:取登记静态配方,重钉 initialMapId=本局图、extraComponents=静态模板在前
     * (空场解散组件等固定件)+本局配置组件追加在后(框架按组件 id 查重,empty-arena 与 match-config
     * 异 id,双保险防双挂),组装成一次性开局配方(不回写登记簿,每局重新补挂)。
     * 组件类型经 var/diamond 传递,本类不新增框架 import。
     *
     * @param modeId      模式 id(mode_config/ 目录名,与登记键一致)
     * @param mapId       本局钉图 id(票选结果拆解)
     * @param matchConfig 本局玩法配置组件(开局前已解析;组件序=静态模板在前、本组件在后)
     * @return 钉图开局配方(交 MatchBootstrap 建会话)
     * @throws IllegalStateException 模式未登记会话配方(装配期事故:登记集与 registerMode 模式集
     *                               一致性被破坏,必须响亮炸——与玩法配置解析的数据面软失败有意区分)
     */
    public SessionRecipe pinned(final String modeId, final String mapId, final MatchConfigComponent.Spec matchConfig) {
        final SessionRecipe template = find(modeId).orElseThrow(() ->
                new IllegalStateException("模式未登记会话配方: " + modeId));
        final var extras = new ArrayList<>(template.extraComponents());
        extras.add(matchConfig);
        return new SessionRecipe(modeId, Optional.of(mapId), template.mapPool(), List.copyOf(extras));
    }

    /** 清空登记(onDisable 反注册口径)。 */
    public void clear() {
        this.recipes.clear();
    }
}
