package com.shdfGameplay.lobby.ui;

import com.shdfGameplay.config.LobbyConfig;
import com.shdfGameplay.lobby.ReadyState;
import com.shdfGameplay.lang.Lang;
import com.zeoliteLib.item.ItemInteractionDefinition;
import com.zeoliteLib.item.ItemInteractionHandle;
import com.zeoliteLib.item.ItemInteractionType;
import com.zeoliteLib.item.event.ZeoliteItemInteractionEvent;
import java.util.List;
import java.util.function.Consumer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * 大厅快捷栏物品件(ZeoliteLib 物品支持库):地图投票物品(0 槽 PAPER,右键开投票菜单)与
 * 准备物品(1 槽 木剑⇄钻石剑,右键翻准备账本;enableReady=false 不发放)两件交互物品的
 * 定义注册/PDC 标记/发放收回/准备态即时换装。默认不可丢弃不可移动(build() 自动挂 DropBlocker/
 * InventoryMoveBlocker 族),右键回调限定主手(ZeoliteItemInteractionEvent 按手别分发,副手不响应);
 * 样式参照弃用插件 SHDFGamePlugin WaitingPhase.createReadyItem/createMapVoteItem。
 * 生命周期随 LobbyView:start 注册定义、stop 注销(再 start 重注册),register 前零交互面。
 * —— 出自 本次任务(大厅交互层)。
 */
final class LobbyViewItems {

    /** 地图投票物品定义 id(ZeoliteLib ItemInteractionManager 登记键,shdf 前缀防撞)。 */
    private static final String VOTE_ITEM_ID = "shdf_lobby_map_vote";

    /** 准备物品定义 id。 */
    private static final String READY_ITEM_ID = "shdf_lobby_ready_toggle";

    /** 地图投票物品槽位(快捷栏第 0 格;参照弃用插件 0/1/2 三件位,本层无选队件故前移)。 */
    static final int VOTE_SLOT = 0;

    /** 准备物品槽位(快捷栏第 1 格)。 */
    static final int READY_SLOT = 1;

    private final ReadyState ready;
    private final LobbyConfig config;

    /** 交互定义句柄(null=未注册;start 注册/stop 注销)。 */
    private ItemInteractionHandle voteHandle;
    private ItemInteractionHandle readyHandle;

    /**
     * @param ready  准备账本(准备物品换装读取)
     * @param config 大厅配置(enableReady=false 不发放准备物品)
     */
    LobbyViewItems(final ReadyState ready, final LobbyConfig config) {
        this.ready = ready;
        this.config = config;
    }

    /**
     * 注册两件交互定义(ZeoliteLib 物品模块随之懒初始化,主线程);右键回调限定主手转发给注入入口。
     *
     * @param onVoteClick 投票物品右键入口(开投票菜单)
     * @param onReadyClick 准备物品右键入口(翻准备账本)
     */
    void register(final Consumer<Player> onVoteClick, final Consumer<Player> onReadyClick) {
        this.voteHandle = registerItem(VOTE_ITEM_ID, onVoteClick);
        this.readyHandle = registerItem(READY_ITEM_ID, onReadyClick);
    }

    /** 注销交互定义(幂等;stop 收摊用,此后旧 PDC 物品交互不再分发)。 */
    void unregister() {
        if (this.voteHandle != null) {
            this.voteHandle.unregister();
            this.voteHandle = null;
        }
        if (this.readyHandle != null) {
            this.readyHandle.unregister();
            this.readyHandle = null;
        }
    }

    /** 发放大厅快捷栏物品(present 差集新入场者;enableReady=false 准备槽置空)。 */
    void give(final Player player) {
        player.getInventory().setItem(VOTE_SLOT, voteItem());
        player.getInventory().setItem(READY_SLOT, this.config.enableReady()
                ? readyItem(this.ready.isReady(player.getUniqueId()))
                : null);
    }

    /** 收回大厅快捷栏物品(离场者;两槽置空,enableReady=false 时准备槽本就为空)。 */
    void strip(final Player player) {
        player.getInventory().setItem(VOTE_SLOT, null);
        player.getInventory().setItem(READY_SLOT, null);
    }

    /** 即时换装准备物品(翻账后调用,不等差集拍;未启用准备为无操作)。 */
    void refreshReady(final Player player) {
        if (!this.config.enableReady()) {
            return;
        }
        player.getInventory().setItem(READY_SLOT, readyItem(this.ready.isReady(player.getUniqueId())));
    }

    /** 注册单件「右键转发」交互定义:canDrop/canMove 缺省 false(build 自动挂丢弃/移动拦截族)。 */
    private static ItemInteractionHandle registerItem(final String id, final Consumer<Player> onClick) {
        final ItemInteractionDefinition definition = ItemInteractionDefinition.createBuilder(id)
                .addItemInteractionListener(event -> forwardRightClick(event, onClick))
                .build();
        final ItemInteractionHandle handle = ItemInteractionHandle.create(definition);
        handle.register();
        return handle;
    }

    /** 右键转发过滤器:仅主手 RIGHT_CLICK 转发(副手不响应防双触发,ZeoliteLib 已去重同 tick 补发包)。 */
    private static void forwardRightClick(final ZeoliteItemInteractionEvent event, final Consumer<Player> onClick) {
        final Player player = event.getRawEvent().getPlayer();
        if (event.getType() != ItemInteractionType.RIGHT_CLICK
                || event.getHand() != EquipmentSlot.HAND
                || player == null) {
            return;
        }
        onClick.accept(player);
    }

    /** 地图投票物品:PAPER「票选地图」蓝粗体 + 用法双行 lore + PDC 标记。 */
    private ItemStack voteItem() {
        final ItemStack item = new ItemStack(Material.PAPER);
        final ItemMeta meta = item.getItemMeta();
        meta.displayName(Lang.render("lobby.items.vote_name"));
        meta.lore(List.of(
                Lang.render("lobby.items.vote_lore_1"),
                Lang.render("lobby.items.vote_lore_2")));
        this.voteHandle.writePDC(meta);
        item.setItemMeta(meta);
        return item;
    }

    /** 准备物品:已准备=钻石剑绿粗体 / 未准备=木剑红粗体(参照弃用插件对偶形态),lore 说明门槛 + PDC 标记。 */
    private ItemStack readyItem(final boolean isReady) {
        final ItemStack item = new ItemStack(isReady ? Material.DIAMOND_SWORD : Material.WOODEN_SWORD);
        final ItemMeta meta = item.getItemMeta();
        meta.displayName(isReady
                ? Lang.render("lobby.items.ready_on_name")
                : Lang.render("lobby.items.ready_off_name"));
        meta.lore(List.of(
                Lang.render("lobby.items.ready_lore_1"),
                Lang.render("lobby.items.ready_lore_2")));
        this.readyHandle.writePDC(meta);
        item.setItemMeta(meta);
        return item;
    }
}
