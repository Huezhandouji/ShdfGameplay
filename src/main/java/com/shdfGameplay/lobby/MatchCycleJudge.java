package com.shdfGameplay.lobby;

/**
 * 大厅开局判定核(离线纯状态机,壳每拍喂 present 人数与全员 ready 判定,输出动作):
 * WAIT=门未过(门=人数≥minPlayers ∧ (¬enableReady ∨ allReady));enableReady=true 门过当拍立即开局
 * (04 §4.2「全员 ready=立即开局,人数门槛仍要过」);enableReady=false 门过起倒计时(countdownTicks
 * 防抖窗口,满额开局)每拍-1、归零 LAUNCH,倒计时内门破坏 ABORT 回 WAIT 剩余清零(重起从头数);
 * LAUNCH 后自回 WAIT(single-fire,下一拍重新判门);开局失败冷却住本核(notifyLaunchRejected 进
 * max(countdownTicks,100L) 拍冷却,冷却期恒 WAIT 递减,兼护 countdown=0 配置)——开局节奏全住本核,
 * 壳只转发结果(F1 节流必须可测:壳无离线测试)。
 * —— 出自 2.5 大厅逻辑(04-大厅模式 §4.2/§4.3 门表;2.5 步骤4)。
 */
public final class MatchCycleJudge {

    /** 每拍判定动作:WAIT 静默 / COUNTDOWN 倒计时进行(首拍 remaining==countdownTicks) / ABORT 倒计时中止 / LAUNCH 开局。 */
    public enum Decision {WAIT, COUNTDOWN, ABORT, LAUNCH}

    /** 开局失败冷却下限(tick,5s:countdownTicks 更小时兜底,防零/小倒计时配置每拍重试风暴)。 */
    private static final long MIN_REJECT_COOLDOWN_TICKS = 100L;

    private final int minPlayers;
    private final boolean enableReady;
    private final long countdownTicks;

    /** 倒计时进行标志(WAIT=false;COUNTDOWN 期间门破坏即回 WAIT)。 */
    private boolean counting;

    /** 倒计时剩余 tick(WAIT 恒 0;COUNTDOWN 首拍=countdownTicks,此后每拍-1,归零 LAUNCH)。 */
    private long remainingTicks;

    /** 开局失败冷却剩余 tick(0=无冷却;冷却期 tick 恒 WAIT,每拍递减,冷却尽恢复常语义)。 */
    private long cooldownTicks;

    /**
     * @param minPlayers     开局最少人数(人数门)
     * @param enableReady    是否启用准备门(true=人数+全员 ready 当拍立即开局,不经倒计时)
     * @param countdownTicks 倒计时长度(tick;enableReady=false 门过起算;0=退化为门过即开局)
     */
    public MatchCycleJudge(final int minPlayers, final boolean enableReady, final long countdownTicks) {
        this.minPlayers = minPlayers;
        this.enableReady = enableReady;
        this.countdownTicks = countdownTicks;
    }

    /**
     * 每拍判定(壳喂大厅快照;主线程拍序,内部状态随调用演进)。
     *
     * @param presentCount 当前大厅在场人数
     * @param allReady     在场者是否全员已准备(ReadyState.allReady 口径:空集=false)
     * @return 本拍动作(见 {@link Decision};冷却期恒 WAIT)
     */
    public Decision tick(final int presentCount, final boolean allReady) {
        if (this.cooldownTicks > 0) {
            this.cooldownTicks--;
            return Decision.WAIT;   // 开局失败冷却:门不判,冷却尽恢复常语义(门重判、倒计时重走)
        }
        final boolean gate = presentCount >= this.minPlayers && (!this.enableReady || allReady);
        if (this.counting) {
            if (!gate) {
                this.counting = false;
                this.remainingTicks = 0;
                return Decision.ABORT;   // 倒计时内门破坏:中止回 WAIT,重起从头数
            }
            this.remainingTicks--;
            if (this.remainingTicks <= 0) {
                this.counting = false;
                this.remainingTicks = 0;
                return Decision.LAUNCH;  // 归零开局;自回 WAIT(single-fire,下一拍重新判门)
            }
            return Decision.COUNTDOWN;
        }
        if (!gate) {
            return Decision.WAIT;
        }
        if (this.enableReady) {
            return Decision.LAUNCH;      // 人数+全员 ready:当拍立即开局,不经倒计时(人数门槛仍要过)
        }
        this.counting = true;
        this.remainingTicks = this.countdownTicks;
        if (this.remainingTicks <= 0) {
            this.counting = false;
            return Decision.LAUNCH;      // countdown=0 退化为门过即开局
        }
        return Decision.COUNTDOWN;       // 首拍:remaining==countdownTicks(壳据此只广播一次倒计时)
    }

    /**
     * 开局失败通知(F1 节流):进入 max(countdownTicks,100L) 拍冷却并打断在途倒计时;
     * 冷却期 tick() 恒 WAIT,冷却尽恢复常语义——开局重试节奏全住本核,壳只转发。
     */
    public void notifyLaunchRejected() {
        this.cooldownTicks = Math.max(this.countdownTicks, MIN_REJECT_COOLDOWN_TICKS);
        this.counting = false;
        this.remainingTicks = 0;
    }

    /** 回 WAIT 清零(含冷却;周期 stop 用,幂等)。 */
    public void reset() {
        this.counting = false;
        this.remainingTicks = 0;
        this.cooldownTicks = 0;
    }

    /** 倒计时剩余 tick(WAIT 恒 0;壳判首拍广播与测试口)。 */
    public long remaining() {
        return this.remainingTicks;
    }
}
