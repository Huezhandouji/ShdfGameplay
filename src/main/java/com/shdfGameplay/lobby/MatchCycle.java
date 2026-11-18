package com.shdfGameplay.lobby;

/**
 * 大厅开局周期缝:GameFlow 随流程开关单调调用的起停口(生产 {@link BukkitMatchCycle} BukkitScheduler
 * 节拍壳/测试 no-op 假件各一,对齐 LobbyTeleport 缝惯例)。start 幂等(false=已在运行),
 * stop 取消周期任务并清投票票本/准备账本/判定核(幂等)——壳内语义全住生产实现,本缝只定形。
 * —— 出自 2.5 大厅逻辑(04-大厅模式 §4.2/§4.3;2.5 步骤4)。
 */
public interface MatchCycle {

    /**
     * 启动周期:起票+候选行广播+注册节拍任务。
     *
     * @return true=本次完成启动;false=已在运行(幂等无副作用)
     */
    boolean start();

    /** 停止周期:取消任务、清票本/准备账本/判定核(幂等,disable 与流程 stop 共用)。 */
    void stop();
}
