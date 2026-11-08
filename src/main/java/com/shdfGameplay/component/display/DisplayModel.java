package com.shdfGameplay.component.display;

import net.kyori.adventure.text.Component;

import java.util.List;
import java.util.Optional;

/**
 * 表现层的中立数据模型:模式把语义翻译成行 + 进度 + 倒计时,组件只认本模型(宿主资产,框架 jar 零表现层)。
 * 来源:10-表现层组件 §10.1(自框架 06 §6.2 v1.3 迁入原文);05-四模式矩阵 §5.6(modes 摘要 DisplayModel/DisplayLine/Countdown/Progress 条)。
 */
public record DisplayModel(
        Component title,
        List<DisplayLine> bossBarLines,
        List<DisplayLine> sidebarLines,
        Optional<Countdown> countdown,
        Optional<Progress> progress) {

    // TODO(5.1): 倒计时从 PhaseComponent.countdown() 取、全局进度从 ObjectiveComponent.globalProgress() 取(经 svc(),非引擎口子;ObjectiveComponent 待框架线4.2);lobbyDisplay 为历史口径不落(README ⑩) —— 出自 10-表现层组件 §10.1 示例;05-四模式矩阵 §5.6;框架线施工计划 阶段4。
    /**
     * 单行显示数据:文本 + 权重 + 样式。
     */
    public record DisplayLine(Component text, int weight, LineStyle style) {
    }

    /**
     * 行样式:普通/高亮/暗淡/危险。
     */
    public enum LineStyle {
        NORMAL, HIGHLIGHT, DIM, DANGER
    }

    /**
     * 倒计时数据:剩余/总 tick 数 + 标签(经验条/标题倒计时)。
     */
    public record Countdown(int remainingTicks, int totalTicks, Component label) {
    }

    /**
     * 全局进度数据:当前值/目标值 + 标签(目标进度/分数/血条皆可)。
     */
    public record Progress(double current, double target, Component label) {
    }
}
