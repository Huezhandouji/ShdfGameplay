package com.shdfGameplay.config;

/**
 * 玩法专用配置解析器:把 {@link GameConfig} 的 metadata 自由映射解析为某模式的专用配置 record
 * (如 BosswarSpecificConfig);实现必须纯函数、非法配置响亮抛 {@link IllegalStateException}
 * (带病配置在 MatchBootstrap 开局前拒收,不进会话)。解析结果开局前随 MatchConfigComponent
 * 会话组件入局,不再写入 MapDefinition.metadata(词料通道已弃用)。
 *
 * @param <T> 玩法专用配置 record 类型
 */
public interface SpecificConfigParser<T extends Record> {

    /**
     * 解析单图原始配置。
     *
     * @param rawConfig 单图原始配置(GameConfigManager 缓存件)
     * @return 玩法专用配置 record
     * @throws IllegalStateException 任一必需键缺席或类型不符(响亮)
     */
    T parseRawGameConfig(GameConfig rawConfig);

}
