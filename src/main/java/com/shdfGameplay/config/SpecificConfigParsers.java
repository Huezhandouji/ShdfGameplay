package com.shdfGameplay.config;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 玩法专用配置解析器注册处:modeId(mode_config/ 目录名)→ {@link SpecificConfigParser} 的进程级登记簿。
 * MatchBootstrap 开局前经 {@link #find} 取解析器,把 {@link GameConfig} 的 metadata 解析为玩法专用
 * record(带病配置在开局前响亮拒收),随 {@code MatchConfigComponent} 会话组件入局——玩法配置传递
 * 不再走 MapDefinition.metadata 词料通道(已弃用)。
 * 注册/注销由主类 L0 装配点显式完成(无 SPI);bosswar 解析器随 6.3 落地注册,idle 两模式无解析器。
 */
public final class SpecificConfigParsers {

    private final Map<String, SpecificConfigParser<?>> parsers = new ConcurrentHashMap<>();

    /**
     * 登记某模式的专用配置解析器(重复 id 整体替换)。
     *
     * @param modeId 模式 id(mode_config/ 目录名)
     * @param parser 解析器(如 BosswarSpecificConfig.Parser)
     */
    public void register(final String modeId, final SpecificConfigParser<?> parser) {
        Objects.requireNonNull(modeId, "modeId");
        Objects.requireNonNull(parser, "parser");
        this.parsers.put(modeId, parser);
    }

    /**
     * 按模式 id 取解析器。
     *
     * @param modeId 模式 id(mode_config/ 目录名)
     * @return 该模式的解析器;未注册(如 idle)为空
     */
    public Optional<SpecificConfigParser<?>> find(final String modeId) {
        return Optional.ofNullable(this.parsers.get(modeId));
    }

    /** 清空登记(onDisable 反注册口径)。 */
    public void clear() {
        this.parsers.clear();
    }
}
