package com.shdfGameplay.mode.idle;

import com.hydrokarbs.engine.AttributeBag;
import com.hydrokarbs.engine.mode.MatchMode;
import com.hydrokarbs.engine.mode.MatchModeMetadata;
import com.hydrokarbs.engine.mode.MatchModeProvider;
import com.hydrokarbs.engine.session.MatchModeConfig;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;

import java.util.Set;

/**
 * 空转测试模式双工厂:idlepad/idlepark 两个最小声明的测试模式,互为 100 次切换往返对象,服务 2.5 出口闸门。
 * 来源:施工计划/2.5-大厅模式与最小闭环.md 步骤 8;README 对齐注⑧(SPI 通道退场,宿主 jar 禁写 META-INF/services)。
 */
public final class IdleModes {

    // TODO(2.5): IdlePadRoundTripTest——idlepad↔idlepark 100 次往返,成员恒等/句柄无累积/同入口/事件各 100 次;IdleModeDeclarationTest 冻结声明(组件表空、meta 词料键(false,1,200)、mapTags [idle]) —— 出自 2.5-大厅模式与最小闭环 测试与验收表。
    private IdleModes() {
    }

    /**
     * 空转模式 A(压力板):Builder 最小声明,组件表空(不挂 EndGate = 无结束判定)。
     */
    public static final class IdlePadFactory implements MatchModeProvider {

        @Override
        public String id() {
            return "idlepad";
        }

        @Override
        public MatchMode build(final MatchModeConfig cfg) {
            // TODO(2.5): 主类 onEnable 显式 registerMode × 2(宿主 jar 禁写 META-INF/services);build(MatchModeConfig) 形参忽略(框架 modes/<id>.yml 对宿主模式缺省空 config) —— 出自 2.5-大厅模式与最小闭环 步骤 1/步骤 8;README 对齐注⑧。
            return new MatchMode.Builder(id())
                    .displayName(Component.text("空转测试 · idlepad"))
                    .icon(Material.LIGHT_WEIGHTED_PRESSURE_PLATE)
                    .mapTags(Set.of("idle"))
                    .metadata(MatchModeMetadata.addTo(new AttributeBag.Builder(), false, 1, 200).build())
                    .build();
        }
    }

    /**
     * 空转模式 B(公园):与 idlepad 同构的最小声明,互为切换往返对象。
     */
    public static final class IdleParkFactory implements MatchModeProvider {

        @Override
        public String id() {
            return "idlepark";
        }

        @Override
        public MatchMode build(final MatchModeConfig cfg) {
            return new MatchMode.Builder(id())
                    .displayName(Component.text("空转测试 · idlepark"))
                    .icon(Material.OAK_SAPLING)
                    .mapTags(Set.of("idle"))
                    .metadata(MatchModeMetadata.addTo(new AttributeBag.Builder(), false, 1, 200).build())
                    .build();
        }
    }
}
