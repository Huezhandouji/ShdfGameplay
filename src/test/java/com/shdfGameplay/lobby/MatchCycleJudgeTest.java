package com.shdfGameplay.lobby;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 开局判定核离线测试(纯状态机,零 Bukkit/框架依赖):门过/门未过两态、enableReady 立即开局
 * (人数门槛仍要过)、enableReady=false 门过起倒计时(首拍 remaining=countdownTicks)、倒计时内
 * 门破坏 ABORT 回 WAIT 重起从头数、countdown=0 退化为门过即 LAUNCH、倒计时归零 LAUNCH、
 * LAUNCH 后自回 WAIT(下一拍重新判门不 latch)、开局失败冷却(F1 节流:冷却期门过+全员 ready
 * 恒 WAIT、冷却下限 100 拍兼护零倒计时配置、冷却尽恢复常语义)、reset 清冷却与剩余。
 * —— 出自 2.5 大厅逻辑(04-大厅模式 §4.2/§4.3 门表;2.5 步骤4)。
 */
class MatchCycleJudgeTest {

    /** 人数门未过恒 WAIT(enableReady 两态;allReady 传 true 也过不了人数门)。 */
    @Test
    void 人数不足_恒WAIT_准备开关两态() {
        final MatchCycleJudge readyOn = new MatchCycleJudge(2, true, 600L);
        assertEquals(MatchCycleJudge.Decision.WAIT, readyOn.tick(1, true), "启用准备:人数不足仍 WAIT");
        final MatchCycleJudge readyOff = new MatchCycleJudge(2, false, 600L);
        assertEquals(MatchCycleJudge.Decision.WAIT, readyOff.tick(1, false), "未启用准备:人数不足 WAIT");
        assertEquals(MatchCycleJudge.Decision.WAIT, readyOff.tick(1, true), "未启用准备:人数不足即便全员 ready 也 WAIT");
    }

    /** enableReady=true 且未全员 ready:门未过 WAIT(WAIT 态 remaining 恒 0)。 */
    @Test
    void 启用准备_未全员ready_WAIT() {
        final MatchCycleJudge judge = new MatchCycleJudge(2, true, 600L);

        assertEquals(MatchCycleJudge.Decision.WAIT, judge.tick(3, false));
        assertEquals(0L, judge.remaining(), "WAIT 态剩余恒 0");
    }

    /** enableReady=true 人数+全员 ready:当拍立即 LAUNCH(人数门槛仍要过;不经倒计时)。 */
    @Test
    void 启用准备_人数过且全员ready_当拍立即开局() {
        final MatchCycleJudge judge = new MatchCycleJudge(2, true, 600L);

        assertEquals(MatchCycleJudge.Decision.LAUNCH, judge.tick(2, true), "恰过人数门即当拍 LAUNCH");
        assertEquals(MatchCycleJudge.Decision.LAUNCH, judge.tick(5, true), "超员照常当拍 LAUNCH");
        assertEquals(MatchCycleJudge.Decision.WAIT, new MatchCycleJudge(2, true, 600L).tick(1, true),
                "人数不足即使全员 ready 不开局");
    }

    /** enableReady=false 人数门过:起倒计时 COUNTDOWN,首拍 remaining=countdownTicks,此后每拍-1。 */
    @Test
    void 未启用准备_人数门过_起倒计时首拍剩余满额() {
        final MatchCycleJudge judge = new MatchCycleJudge(2, false, 600L);

        assertEquals(MatchCycleJudge.Decision.COUNTDOWN, judge.tick(2, false), "门过起倒计时");
        assertEquals(600L, judge.remaining(), "首拍剩余=配置倒计时(壳据此只广播一次)");
        assertEquals(MatchCycleJudge.Decision.COUNTDOWN, judge.tick(2, false));
        assertEquals(599L, judge.remaining(), "此后每拍-1");
    }

    /** 倒计时内人数跌破:ABORT 回 WAIT 剩余清零;再过门从头数(remaining 回满)。 */
    @Test
    void 倒计时内人数跌破_中止回WAIT_再过门从头数() {
        final MatchCycleJudge judge = new MatchCycleJudge(2, false, 600L);
        judge.tick(2, false);
        judge.tick(2, false);   // 剩余 599

        assertEquals(MatchCycleJudge.Decision.ABORT, judge.tick(1, false), "门破坏当拍 ABORT");
        assertEquals(0L, judge.remaining(), "中止即清零");
        assertEquals(MatchCycleJudge.Decision.WAIT, judge.tick(1, false), "回 WAIT(不自动重起)");
        assertEquals(MatchCycleJudge.Decision.COUNTDOWN, judge.tick(2, false), "再过门重起倒计时");
        assertEquals(600L, judge.remaining(), "重起从头数(不续旧剩余)");
    }

    /** countdown=0 配置:enableReady=false 门过即 LAUNCH(不产 COUNTDOWN)。 */
    @Test
    void 倒计时为零_门过即开局() {
        final MatchCycleJudge judge = new MatchCycleJudge(1, false, 0L);

        assertEquals(MatchCycleJudge.Decision.LAUNCH, judge.tick(1, false), "零倒计时退化为门过即开局");
    }

    /** 倒计时归零 LAUNCH:countdownTicks 拍倒数后开局(countdown=3 → 3 拍 COUNTDOWN 后第 4 拍 LAUNCH)。 */
    @Test
    void 倒计时归零_开局() {
        final MatchCycleJudge judge = new MatchCycleJudge(1, false, 3L);

        assertEquals(MatchCycleJudge.Decision.COUNTDOWN, judge.tick(1, false));
        assertEquals(MatchCycleJudge.Decision.COUNTDOWN, judge.tick(1, false));
        assertEquals(MatchCycleJudge.Decision.COUNTDOWN, judge.tick(1, false));
        assertEquals(MatchCycleJudge.Decision.LAUNCH, judge.tick(1, false), "倒数尽当拍 LAUNCH");
    }

    /** LAUNCH 后自回 WAIT(single-fire):enableReady 两态下一拍均重新判门、不 latch。 */
    @Test
    void 开局后自回WAIT_下一拍重新判门() {
        final MatchCycleJudge countdown = new MatchCycleJudge(1, false, 2L);
        countdown.tick(1, false);
        countdown.tick(1, false);
        assertEquals(MatchCycleJudge.Decision.LAUNCH, countdown.tick(1, false));
        assertEquals(MatchCycleJudge.Decision.COUNTDOWN, countdown.tick(1, false), "同输入下一拍重新起倒计时");
        assertEquals(2L, countdown.remaining(), "重起剩余回满");

        final MatchCycleJudge ready = new MatchCycleJudge(1, true, 600L);
        assertEquals(MatchCycleJudge.Decision.LAUNCH, ready.tick(1, true));
        assertEquals(MatchCycleJudge.Decision.WAIT, ready.tick(1, false), "ready 撤回即 WAIT(证明未 latch 在 LAUNCH)");
    }

    /** F1 节流:notifyLaunchRejected 后冷却期(max(countdown,100) 拍)门过+全员 ready 仍恒 WAIT,冷却尽恢复常语义。 */
    @Test
    void 开局失败通知_冷却期内门过仍WAIT_冷却尽恢复() {
        final MatchCycleJudge judge = new MatchCycleJudge(1, false, 100L);
        judge.notifyLaunchRejected();   // 冷却=max(100,100)=100 拍

        for (int i = 0; i < 100; i++) {
            assertEquals(MatchCycleJudge.Decision.WAIT, judge.tick(5, true),
                    "冷却第 " + (i + 1) + " 拍门过+全员 ready 仍恒 WAIT");
        }
        assertEquals(MatchCycleJudge.Decision.COUNTDOWN, judge.tick(2, false), "冷却尽恢复常语义:门重判起倒计时");
        assertEquals(100L, judge.remaining(), "冷却尽倒计时从头走");
    }

    /** F1 节流下限:countdown<100 时冷却仍 100 拍(兼护 countdown=0 配置的每拍重试风暴)。 */
    @Test
    void 开局失败通知_冷却下限一百拍_兼护零倒计时() {
        final MatchCycleJudge judge = new MatchCycleJudge(1, true, 0L);
        assertEquals(MatchCycleJudge.Decision.LAUNCH, judge.tick(1, true));
        judge.notifyLaunchRejected();   // 冷却=max(0,100)=100 拍

        for (int i = 0; i < 100; i++) {
            assertEquals(MatchCycleJudge.Decision.WAIT, judge.tick(1, true), "零倒计时配置同样 100 拍节流");
        }
        assertEquals(MatchCycleJudge.Decision.LAUNCH, judge.tick(1, true), "冷却尽恢复 enableReady 立即开局语义");
    }

    /** 失败通知打断在途倒计时(剩余清零);reset 清冷却与剩余(周期 stop 口径)。 */
    @Test
    void reset_清冷却与倒计时剩余() {
        final MatchCycleJudge judge = new MatchCycleJudge(1, false, 50L);
        judge.tick(1, false);           // COUNTDOWN,剩余 50
        judge.notifyLaunchRejected();   // 冷却 100 拍且打断倒计时
        judge.reset();

        assertEquals(0L, judge.remaining(), "reset 清剩余");
        assertEquals(MatchCycleJudge.Decision.COUNTDOWN, judge.tick(1, false), "reset 清冷却:下一拍按常语义判门(非 WAIT)");
    }
}
