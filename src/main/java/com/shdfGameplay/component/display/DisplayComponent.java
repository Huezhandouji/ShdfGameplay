package com.shdfGameplay.component.display;

import com.hydrokarbs.engine.component.MatchModeComponent;
import com.hydrokarbs.engine.instance.MatchModeInstanceView;
import com.hydrokarbs.engine.port.MatchModeInstanceServices;

import java.util.function.Function;

/**
 * 表现层宿主组件(自框架 v1.3 迁入):数据驱动渲染——上一帧 diff、参与者显示登记、stop 全员清除;Spec 收构建函数方法引用。
 * 来源:10-表现层组件 §10.2/归属口径;05-四模式矩阵 §5.6;README 对齐注头部 R2(表现层出框架迁宿主)与①(宿主包根 com.shdf.*)。
 */
public final class DisplayComponent extends MatchModeComponent {

    // TODO(5.1): 显示登记回收走框架组件基类登记资源口(stop 后统一回收)——机制在框架、表现内容在宿主;渲染器族(render/**:BossBar/Sidebar/Title/Countdown)施工时按 10 篇文档落,届时回本方案补文件条目 —— 出自 10-表现层组件 §10.2;框架 02 §2.5。
    private final Function<MatchModeInstanceView, DisplayModel> builder;

    DisplayComponent(final String id, final MatchModeInstanceServices svc,
            final Function<MatchModeInstanceView, DisplayModel> builder) {
        super(id, svc);
        this.builder = builder;
    }

    /**
     * 声明件:收构建函数(方法引用形态,如 BombDisplays::bombDisplay)。
     */
    public static final class Spec extends MatchModeComponent.Specification<DisplayComponent> {

        // TODO(5.1): 挂法=new DisplayComponent.Spec(构建函数方法引用,如 BombDisplays::bombDisplay);TDM 构建函数随 5.1 落 TeamDeathmatchModes 内嵌容器 —— 出自 05-四模式矩阵 §5.8 组件表 display 行;5.1-TDM模式工厂 步骤 7;10-表现层组件 §10.2。
        private final Function<MatchModeInstanceView, DisplayModel> builder;

        public Spec(final Function<MatchModeInstanceView, DisplayModel> builder) {
            super("Display");
            this.builder = builder;
        }

        @Override
        public DisplayComponent create(final String id, final MatchModeInstanceServices svc) {
            return new DisplayComponent(id, svc, builder);
        }
    }
}
