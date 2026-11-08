package com.shdfGameplay.listener;

import java.util.UUID;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * MONITOR 伤害记账监听器:onDamage 双侧记账 getFinalDamage()(victim 与 damager 各记一笔真扣血);内嵌包私有 DamageLedger 过渡 seam(伤害记账与角色无关,拔掉角色插件照常工作)。
 * 来源:施工计划/阶段3 3.5-监听器与击杀归属 步骤3(seam 定义,:56);12-代码骨架 §12.2 StatsSignalListener 块;15-PaperMC适配 §15.1 优先级表;README 对齐注①⑤。
 */
public final class StatsSignalListener implements Listener {

    private final DamageLedger ledger;

    public StatsSignalListener(DamageLedger ledger) {
        this.ledger = ledger;
    }

    /**
     * 最终伤害记账:MONITOR 只记账不改状态(不得 setCancelled、不得改实体);ignoreCancelled 在这里有实义(伤害事件可取消,角色系统可能因免疫/格挡取消伤害,只记真的扣了血的)。
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        // TODO(3.5): 只记账不做重活:victim 与 damager 双侧各记一笔 getFinalDamage()(以实例为界,不在会话里的伤害自然落空);构造注入只进数据不进行为、不做服务定位 —— 出自 3.5-监听器与击杀归属 步骤3(:54-57);12-代码骨架 §12.2;15-PaperMC适配 §15.1 MONITOR 行。
    }

    // TODO(框架线4.4): StatsComponent 就位后把注入换成 i.component(StatsComponent.class) 直查并删除 DamageLedger seam(框架阶段4 4.4 填实而非新建) —— 出自 3.5-监听器与击杀归属 步骤3 与待裁决问题;框架线阶段4 README §3 并行约束(:69)。

    /**
     * 伤害账本过渡 seam(包私有窄接口,默认 no-op 实现):阶段4 StatsComponent 交付前的路由目标,v3.1 拆分动机=伤害记账与角色无关。
     */
    interface DamageLedger {

        /** 记一笔承受伤害(受害者侧)。 */
        void addDamageTaken(UUID player, long amount);

        /** 记一笔造成伤害(发起者侧)。 */
        void addDamageDealt(UUID player, long amount);
    }
}
