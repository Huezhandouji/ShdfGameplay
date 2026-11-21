package com.shdfGameplay.lobby;

import org.bukkit.entity.Player;

/**
 * 大厅交互层缝(生产 Bukkit 实现住 lobby/ui,测试假件各一):快捷栏物品(地图投票/准备)、
 * 地图投票箱子菜单与侧边计分板的发放/刷新/收摊,随开局周期起停、每拍由周期壳驱动。
 * 契约:全部方法仅主线程调用(周期壳节拍与 ZeoliteLib 交互事件均主线程);实现自持差集状态
 * (present 观察快照/票本观察序号),tick 差集驱动「有玩家投票即刷新所有已打开菜单」——
 * 单一刷新点覆盖菜单点击、/shdf vote 命令与门表自愈摘候选全部票本变更路径。
 * —— 出自 本次任务(大厅交互层,参照弃用插件 SHDFGamePlugin WaitingPhase 物品/侧边栏样式)。
 */
public interface LobbyView {

    /**
     * 启动交互层:注册交互物品定义(ZeoliteLib 物品模块懒初始化)并复位差集状态;
     * 物品发放/侧边栏挂载由此后每拍 tick 差集覆盖(launch 时账本恒空,无即时发放面)。
     */
    void start();

    /** 停止交互层:关全部已打开投票菜单、收回物品、撤侧边栏、注销物品定义(幂等,流程 stop 与 disable 共用)。 */
    void stop();

    /**
     * 周期壳每拍驱动口:present 差集发放/收回快捷栏物品与挂/撤侧边栏、票本观察序号变更即刷新
     * 所有已打开投票菜单(lore 票数/投票人)、侧边栏按需重建(≥1Hz 或 present/全员 ready 翻转)。
     *
     * @param countdownRemaining 判定核倒计时剩余 tick(0=未在倒计时;侧边栏倒计时行渲染源)
     */
    void tick(long countdownRemaining);

    /** 打开地图投票菜单(投票物品右键入口;不在大厅/本轮无候选给回执不开)。 */
    void openVoteMenu(Player player);

    /**
     * 翻准备账本(准备物品右键入口,与 /shdf ready 同口径:在场校验红字/未启用黄字/翻账绿字),
     * 翻账后即时刷新该玩家准备物品(木剑⇄钻石剑)。
     */
    void toggleReady(Player player);
}
