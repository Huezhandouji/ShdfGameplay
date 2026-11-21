package com.shdfGameplay.lobby.ui;

import com.shdfGameplay.config.GameConfig;
import com.shdfGameplay.config.GameConfigManager;
import com.shdfGameplay.lobby.LobbyManager;
import com.shdfGameplay.lobby.LobbyVote;
import com.shdfGameplay.lang.Lang;
import com.zeoliteLib.inventoryMenu.ChestMenu;
import com.zeoliteLib.item.event.ZeoliteInventoryClickEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * 地图投票箱子菜单件(ZeoliteLib inventoryMenu):六行九列、灰玻璃板全边框、第二行 A-G 七候选槽
 * (本轮候选自 A 起依序占位,余槽留空;候选数&gt;7 时超出部分仅聊天候选行可投,菜单面截断展示)。
 * 每次打开新建 ChestMenu 实例(配置冻结纪律)、点击候选即 castVote 可改票;
 * 「有玩家投票即刷新所有已打开菜单」:refreshAll 对每个在场菜单重放候选物品
 * (lore=票数+投票人,ballotBoard 同源),候选缩水的余槽清空——刷新由 BukkitLobbyView 差集
 * (LobbyVote.revision)驱动,菜单点击//shdf vote 命令/门表自愈摘候选三路径共用一口。
 * —— 出自 本次任务(大厅交互层;布局按任务给样,样式参照弃用插件 WaitingPhase 菜单)。
 */
final class LobbyVoteMenu {

    /** 边框字符(灰染色玻璃板)。 */
    private static final char BORDER = '#';

    /** 候选槽字符 A-G(九列去左右边框恰七槽,候选数上限)。 */
    private static final char[] CANDIDATE_CHARS = {'A', 'B', 'C', 'D', 'E', 'F', 'G'};

    /** 候选首槽(第二行第 2 格,即 row=1/col=1)。 */
    private static final int FIRST_CANDIDATE_SLOT = 10;

    private final LobbyManager lobby;
    private final LobbyVote vote;
    private final GameConfigManager games;

    /** 已打开菜单的容器集(onClose 摘除;refreshAll/closeAll 遍历面)。 */
    private final Set<Inventory> open = new HashSet<>();

    /**
     * @param lobby 大厅账本(菜单点击的在场校验)
     * @param vote  投票状态机(候选/票面看板/记票)
     * @param games 图配置管理器(候选行 mode@map 回查展示三键:display_name/description/icon)
     */
    LobbyVoteMenu(final LobbyManager lobby, final LobbyVote vote, final GameConfigManager games) {
        this.lobby = lobby;
        this.vote = vote;
        this.games = games;
    }

    /** 打开投票菜单(调用方保证在场且本轮有候选):固定布局+A-G 绑定,物品按当前候选渲染。 */
    void open(final Player player) {
        final ChestMenu menu = new ChestMenu(Lang.render("lobby.menu.title"))
                .map("#########",
                     "#ABCDEFG#",
                     "#       #",
                     "#       #",
                     "#       #",
                     "#########")
                .set(BORDER, borderPane());
        for (int i = 0; i < CANDIDATE_CHARS.length; i++) {
            final int choice = i + 1;   // lambda 捕获需要(循环变量非有效终值)
            menu.set(CANDIDATE_CHARS[i], candidateItem(i), click -> cast(player, click, choice));
        }
        menu.onClose(this::onClose);
        this.open.add(menu.open(player));
    }

    /** 刷新所有已打开菜单:候选物品重放(lore 票数/投票人随 ballotBoard),候选缩水的余槽清空。 */
    void refreshAll() {
        for (final Inventory inventory : this.open) {
            for (int i = 0; i < CANDIDATE_CHARS.length; i++) {
                inventory.setItem(FIRST_CANDIDATE_SLOT + i, i < this.vote.candidates().size()
                        ? candidateItem(i)
                        : null);
            }
        }
    }

    /** 关闭全部已打开菜单(stop 收摊;closeInventory 触发 onClose 摘除,拷贝遍历防并发修改)。 */
    void closeAll() {
        for (final Inventory inventory : List.copyOf(this.open)) {
            for (final var viewer : List.copyOf(inventory.getViewers())) {
                viewer.closeInventory();
            }
        }
        this.open.clear();
    }

    /** 菜单关闭摘容器(玩家手关/切换/服务器关闭全路径由 ZeoliteLib 路由到此)。 */
    private void onClose(final InventoryCloseEvent event) {
        this.open.remove(event.getInventory());
    }

    /**
     * 候选点击记票:空槽静默(固定 A-G 绑定盖过布局,候选不足七张时余槽无物品)、
     * 不在场红字(开着菜单走出大厅)、序号越界红字(菜单定格旧轮而票本已重排)、
     * 记票绿字回执与 /shdf vote 同文案;lore 刷新归周期壳 revision 差集(≤1 拍)。
     */
    private void cast(final Player player, final ZeoliteInventoryClickEvent click, final int choice) {
        if (click.getCurrentItem() == null || click.getCurrentItem().isEmpty()) {
            return;
        }
        final UUID uuid = player.getUniqueId();
        if (!this.lobby.present().contains(uuid)) {
            player.sendMessage(Lang.render("lobby.vote.not_in_lobby"));
            return;
        }
        final List<String> candidates = this.vote.candidates();
        if (choice > candidates.size()) {
            player.sendMessage(Lang.render("lobby.vote.out_of_range",
                    Placeholder.unparsed("count", String.valueOf(candidates.size()))));
            return;
        }
        this.vote.castVote(uuid, choice);
        player.sendMessage(Lang.render("lobby.vote.cast",
                Placeholder.unparsed("choice", String.valueOf(choice)),
                Placeholder.unparsed("candidate", candidates.get(choice - 1))));
    }

    /** 候选物品:图配置 icon 为底、显示名 [n] display_name、lore=mode@map 行/描述行/票数/投票人/操作提示。 */
    private ItemStack candidateItem(final int index) {
        final List<String> candidates = this.vote.candidates();
        if (index >= candidates.size()) {
            return null;
        }
        final String candidate = candidates.get(index);
        final GameConfig config = findConfig(candidate).orElse(null);
        final ItemStack item = new ItemStack(config == null ? Material.MAP : config.icon());
        final ItemMeta meta = item.getItemMeta();
        meta.displayName(Lang.render("lobby.menu.candidate_name",
                Placeholder.unparsed("index", String.valueOf(index + 1)),
                Placeholder.unparsed("name", config == null ? candidate : config.displayName())));
        meta.lore(candidateLore(index, candidate, config));
        item.setItemMeta(meta);
        return item;
    }

    /** 候选 lore:mode@map 与描述(展示键缺席兜底略)、票数与投票人(ballotBoard 同源)、点击提示。 */
    private List<Component> candidateLore(final int index, final String candidate, final GameConfig config) {
        final List<Component> lore = new ArrayList<>();
        lore.add(Lang.render("lobby.menu.candidate_id", Placeholder.unparsed("candidate", candidate)));
        if (config != null && !config.description().isBlank()) {
            lore.add(Lang.render("lobby.menu.candidate_desc", Placeholder.unparsed("description", config.description())));
        }
        final List<UUID> voters = this.vote.ballotBoard().getOrDefault(index + 1, List.of());
        lore.add(Lang.render("lobby.menu.candidate_votes", Placeholder.unparsed("count", String.valueOf(voters.size()))));
        if (voters.isEmpty()) {
            lore.add(Lang.render("lobby.menu.candidate_no_votes"));
        } else {
            for (final UUID voter : voters) {
                final Player online = Bukkit.getPlayer(voter);
                lore.add(Lang.render("lobby.menu.candidate_voter",
                        Placeholder.unparsed("voter", online == null ? voter.toString() : online.getName())));
            }
        }
        lore.add(Lang.render("lobby.menu.candidate_hint"));
        return lore;
    }

    /** 候选行 mode@map 回查图配置(重载后候选行可能已无对应配置,缺席由调用方兜底)。 */
    private Optional<GameConfig> findConfig(final String candidate) {
        final int at = candidate.indexOf('@');
        if (at <= 0 || at >= candidate.length() - 1) {
            return Optional.empty();
        }
        return this.games.find(candidate.substring(0, at), candidate.substring(at + 1));
    }

    /** 边框物品:灰色染色玻璃板无名牌(占位装饰)。 */
    private static ItemStack borderPane() {
        final ItemStack pane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        final ItemMeta meta = pane.getItemMeta();
        meta.displayName(Component.empty());
        pane.setItemMeta(meta);
        return pane;
    }
}
