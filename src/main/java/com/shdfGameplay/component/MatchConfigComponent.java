package com.shdfGameplay.component;

import com.hydrokarbs.engine.component.SessionComponent;
import com.hydrokarbs.engine.port.SessionServices;

/**
 * 本局玩法配置宿主会话组件(开局前由 MatchBootstrap 解析装入、随 SessionRecipe.extraComponents 贡献):
 * 携带 mode_config/&lt;模式&gt;/&lt;图&gt;.yaml 的玩法专用配置 record,跨模式切换存活(bosswar 选队/选角/倒计时等
 * 复用准备大厅图的 mode 同样可读);模式组件经 svc().self().session().component(MatchConfigComponent.class)
 * 只读取用(SessionView 软接入)。替代原 MapDefinition.metadata 词料传递通道(已弃用)。
 */
public final class MatchConfigComponent extends SessionComponent {

    /** 本局配置来源模式 id(mode_config/ 目录名,如 bosswar)。 */
    private final String modeId;

    /** 本局配置来源图 id(yaml 文件名去扩展名)。 */
    private final String mapId;

    /** 玩法专用配置 record(BosswarSpecificConfig 等);无专用配置的模式(如 idle)为 null。 */
    private final Record config;

    private MatchConfigComponent(final String id, final SessionServices svc,
                                 final String modeId, final String mapId, final Record config) {
        super(id, svc);
        this.modeId = modeId;
        this.mapId = mapId;
        this.config = config;
    }

    /** 本局配置来源模式 id(mode_config/ 目录名)。 */
    public String modeId() {
        return modeId;
    }

    /** 本局配置来源图 id(yaml 文件名去扩展名)。 */
    public String mapId() {
        return mapId;
    }

    /** 本局是否携带玩法专用配置(未注册解析器的模式为 false)。 */
    public boolean hasConfig() {
        return config != null;
    }

    /**
     * 类型化读口:按模式自己的专用配置类型取值。
     *
     * @param type 玩法专用配置 record 类型(如 BosswarSpecificConfig.class)
     * @return 本局配置
     * @throws IllegalStateException 本局无专用配置(该模式未注册解析器),或类型与本局模式不符(编程/装配错误,响亮)
     */
    public <T extends Record> T config(final Class<T> type) {
        if (config == null) {
            throw new IllegalStateException("本局未携带玩法专用配置(modeId=" + modeId + ";该模式未注册 SpecificConfigParser?)");
        }
        if (!type.isInstance(config)) {
            throw new IllegalStateException("玩法专用配置类型不符:期望 " + type.getSimpleName()
                    + ",实为 " + config.getClass().getSimpleName() + "(modeId=" + modeId + ")");
        }
        return type.cast(config);
    }

    /**
     * 声明面:携带开局前(MatchBootstrap 内)解析完成的玩法专用配置;config 为 null 表示该模式无专用配置。
     */
    public static final class Spec extends SessionComponent.Specification<MatchConfigComponent> {

        private final String modeId;
        private final String mapId;
        private final Record config;

        public Spec(final String modeId, final String mapId, final Record config) {
            super("match-config");
            this.modeId = modeId;
            this.mapId = mapId;
            this.config = config;
        }

        @Override
        public MatchConfigComponent create(final String id, final SessionServices svc) {
            return new MatchConfigComponent(id, svc, modeId, mapId, config);
        }
    }
}
