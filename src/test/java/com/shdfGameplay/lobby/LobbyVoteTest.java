package com.shdfGameplay.lobby;

import com.shdfGameplay.config.GameConfig;
import com.shdfGameplay.config.GameConfigManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * LobbyVote 投票状态机离线测试(纯状态机零服务器):多数胜/平局固定种子 TieBreak.RANDOM 可复现/
 * 每人一票可改票/零票单候选直用与多候选 empty/起票抽样口径(禁投票恰 1 张、开投票去重抽样、池小取全量)/
 * 轮换兜底推进回绕与空序不开局/候选行 ClickEvent 执行 /shdf vote n/轮次重置。
 * 候选池经 GameConfig record 直构 + 反射注入 GameConfigManager 缓存字段(init() 离线不可走:
 * 其主配置 lobby 块需 Bukkit.getWorld 世界解析);注入序复刻 manager 稳定序(TreeMap + 模式内 mapId 排序)。
 * 出自 2.5 大厅逻辑(04-大厅模式 §4.2;2.5 步骤3 测试表 LobbyVoteTest)。
 */
class LobbyVoteTest {

    private static final UUID P1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID P2 = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID P3 = UUID.fromString("00000000-0000-0000-0000-000000000003");

    /** 三图候选池(跨两模式,覆盖兜底稳定序的模式间/模式内两级排序)。 */
    private static GameConfigManager threeMaps() {
        return gamesOf("tdm@map2", "bosswar@map3", "tdm@map1");
    }

    /** GameConfigManager 测试夹具:直构 GameConfig record 并反射注入缓存字段(零 init() 零服务器)。 */
    private static GameConfigManager gamesOf(final String... modeAtMap) {
        final GameConfigManager games = new GameConfigManager(Path.of("lobby-vote-test"));
        if (modeAtMap.length == 0) {
            return games;
        }
        final Map<String, List<GameConfig>> byMode = new TreeMap<>();
        for (final String entry : modeAtMap) {
            final String[] parts = entry.split("@", 2);
            byMode.computeIfAbsent(parts[0], mode -> new ArrayList<>())
                    .add(new GameConfig(parts[0], parts[1], Path.of("maps/" + parts[0] + "/" + parts[1] + ".mcstructure"),
                            parts[1], "", Material.MAP, Map.of()));
        }
        byMode.values().forEach(list -> list.sort(Comparator.comparing(GameConfig::mapId)));
        try {
            final Field cache = GameConfigManager.class.getDeclaredField("configsByModeId");
            cache.setAccessible(true);
            cache.set(games, byMode);
            return games;
        } catch (final ReflectiveOperationException e) {
            throw new IllegalStateException("测试夹具注入 GameConfigManager 缓存失败", e);
        }
    }

    // ── 起票与抽样口径 ──

    @Test
    void 未起票_候选空_投票不记() {
        final LobbyVote vote = new LobbyVote(threeMaps(), new Random(1L));

        assertTrue(vote.candidates().isEmpty(), "未起票:候选恒空");
        assertTrue(vote.candidateLines().isEmpty(), "未起票:候选行恒空");
        vote.castVote(P1, 1);
        assertEquals(Optional.empty(), vote.tally(), "未起票:投票不记,tally 恒空");
    }

    @Test
    void 起票_禁投票恰一张_零票直用() {
        final LobbyVote vote = new LobbyVote(threeMaps(), new Random(7L));

        vote.beginRound(false, 3);
        assertEquals(1, vote.candidates().size(), "禁投票:抽 1 张作唯一候选");
        final String only = vote.candidates().get(0);
        assertTrue(List.of("bosswar@map3", "tdm@map1", "tdm@map2").contains(only), "唯一候选来自全量池");
        assertEquals(Optional.of(only), vote.tally(), "零票且单候选:直返该候选(直接用口径)");
    }

    @Test
    void 起票_开投票_抽样张数与去重() {
        final LobbyVote vote = new LobbyVote(threeMaps(), new Random(7L));

        vote.beginRound(true, 2);
        assertEquals(2, vote.candidates().size(), "开投票:抽 mapAmount 张");
        assertEquals(2, vote.candidates().stream().distinct().count(), "候选去重(同图不重复入候选)");

        vote.beginRound(true, 5);
        assertEquals(3, vote.candidates().size(), "池小于 amount:取全量");
        assertEquals(3, vote.candidates().stream().distinct().count());
    }

    // ── 计票:多数胜/平局/改票/零票 ──

    @Test
    void 多数胜() {
        final LobbyVote vote = new LobbyVote(threeMaps(), new Random(3L));
        vote.beginRound(true, 3);

        vote.castVote(P1, 1);
        vote.castVote(P2, 1);
        vote.castVote(P3, 2);
        assertEquals(Optional.of(vote.candidates().get(0)), vote.tally(), "两票对一票:多数胜");
    }

    @Test
    void 改票覆盖旧票() {
        final LobbyVote vote = new LobbyVote(threeMaps(), new Random(3L));
        vote.beginRound(true, 3);

        vote.castVote(P1, 1);
        vote.castVote(P2, 1);
        vote.castVote(P3, 2);
        vote.castVote(P1, 2);
        assertEquals(Optional.of(vote.candidates().get(1)), vote.tally(), "P1 改投 2 号:后投覆盖先投,1:2 反转");
    }

    @Test
    void 投票越界不记() {
        final LobbyVote vote = new LobbyVote(threeMaps(), new Random(3L));
        vote.beginRound(true, 3);

        vote.castVote(P1, 0);
        vote.castVote(P2, 4);
        vote.castVote(P3, -1);
        assertEquals(Optional.empty(), vote.tally(), "零/负/越界序号均不记票");
    }

    @Test
    void 平局_固定种子裁决可复现() {
        final LobbyVote first = new LobbyVote(threeMaps(), new Random(42L));
        first.beginRound(true, 3);
        first.castVote(P1, 1);
        first.castVote(P2, 2);
        final LobbyVote second = new LobbyVote(threeMaps(), new Random(42L));
        second.beginRound(true, 3);
        second.castVote(P1, 1);
        second.castVote(P2, 2);

        assertEquals(first.tally(), second.tally(), "同种子同操作序列:平局裁决一致(可复现)");
        assertTrue(first.candidates().contains(first.tally().orElseThrow()), "平局胜者 ∈ 候选集");
    }

    @Test
    void 零票_多候选_tally空() {
        final LobbyVote vote = new LobbyVote(threeMaps(), new Random(3L));

        vote.beginRound(true, 3);
        assertEquals(Optional.empty(), vote.tally(), "零票且多候选:空(转轮换兜底)");
    }

    // ── 轮换兜底 ──

    @Test
    void 兜底序稳定_nextFallback推进回绕() {
        final LobbyVote vote = new LobbyVote(threeMaps(), new Random(1L));

        assertEquals(List.of("bosswar@map3", "tdm@map1", "tdm@map2"), vote.fallbackOrder(),
                "兜底序=games.all() 稳定序(模式 id 序、模式内 mapId 序)");
        assertEquals(Optional.of("bosswar@map3"), vote.nextFallback(), "指针从首项起");
        assertEquals(Optional.of("tdm@map1"), vote.nextFallback(), "consume 即推进");
        assertEquals(Optional.of("tdm@map2"), vote.nextFallback());
        assertEquals(Optional.of("bosswar@map3"), vote.nextFallback(), "越界回绕到首项");
    }

    @Test
    void 兜底空_不开局_起票无候选() {
        final LobbyVote vote = new LobbyVote(gamesOf(), new Random(1L));

        assertTrue(vote.fallbackOrder().isEmpty(), "零配置:兜底序空");
        assertEquals(Optional.empty(), vote.nextFallback(), "空序兜底:空(本轮不开局重新起票)");
        vote.beginRound(true, 3);
        assertTrue(vote.candidates().isEmpty(), "空池起票:无候选");
        assertEquals(Optional.empty(), vote.tally());
    }

    // ── 轮次重置与候选行渲染 ──

    @Test
    void clear_票本轮次全清_兜底指针跨clear持续() {
        final LobbyVote vote = new LobbyVote(threeMaps(), new Random(3L));
        vote.beginRound(true, 3);
        vote.castVote(P1, 1);
        vote.nextFallback(); // 指针推进到第 2 项

        vote.clear();
        assertTrue(vote.candidates().isEmpty(), "清后回未起票态");
        assertEquals(Optional.empty(), vote.tally(), "票本已清");
        vote.castVote(P1, 1);
        assertEquals(Optional.empty(), vote.tally(), "清后无候选:投票不再记");
        assertEquals(Optional.of("tdm@map1"), vote.nextFallback(), "轮换指针不清:从上次消费处续走");
    }

    // ── 门表自愈摘除(04 §4.3 门表「摘除候选重收票/摘除该玩法候选」口径) ──

    @Test
    void 摘除候选_候选重排_票本清空重收() {
        final LobbyVote vote = new LobbyVote(threeMaps(), new Random(3L));
        vote.beginRound(true, 3);
        final String dead = vote.candidates().get(0);
        final List<String> expectedRest = List.copyOf(vote.candidates().subList(1, 3));
        vote.castVote(P1, 1);
        vote.castVote(P2, 1);

        vote.withdraw(dead);
        assertEquals(expectedRest, vote.candidates(), "摘除后候选重排(被摘候选离板,其余保序)");
        assertEquals(Optional.empty(), vote.tally(), "票本随摘除清空:零票多候选为空(重收票,旧票随旧序失效)");
        vote.castVote(P1, 2);
        assertEquals(Optional.of(expectedRest.get(1)), vote.tally(), "重收后按新序正常计票");
    }

    @Test
    void 摘除候选_不在本轮_无操作() {
        final LobbyVote vote = new LobbyVote(threeMaps(), new Random(3L));
        vote.beginRound(false, 1); // 单候选:池中另两张不在本轮
        final List<String> before = vote.candidates();
        vote.castVote(P1, 1);

        vote.withdraw(before.get(0).equals("bosswar@map3") ? "tdm@map1" : "bosswar@map3");
        assertEquals(before, vote.candidates(), "不在本轮的候选:摘除无操作");
        assertEquals(Optional.of(before.get(0)), vote.tally(), "票本未被动:单候选直返口径不变");
    }

    @Test
    void 摘除玩法候选_该模式全摘_票本清空() {
        final LobbyVote vote = new LobbyVote(threeMaps(), new Random(3L));
        vote.beginRound(true, 3); // 候选含 bosswar@map3 与两张 tdm
        vote.castVote(P1, 1);
        vote.castVote(P2, 2);

        vote.withdrawMode("tdm");
        assertEquals(List.of("bosswar@map3"), vote.candidates(), "摘除该玩法全部候选,异玩法候选保留");
        assertEquals(Optional.of("bosswar@map3"), vote.tally(), "票本清空后单候选直返(大厅照常服务异玩法)");
    }

    @Test
    void 摘除玩法候选_玩法不在本轮_无操作() {
        final LobbyVote vote = new LobbyVote(threeMaps(), new Random(3L));
        vote.beginRound(false, 1);
        final List<String> before = vote.candidates();
        vote.castVote(P1, 1);

        vote.withdrawMode(before.get(0).startsWith("tdm") ? "bosswar" : "tdm");
        assertEquals(before, vote.candidates(), "该玩法不在本轮候选:无操作");
        assertEquals(Optional.of(before.get(0)), vote.tally(), "票本未被动");
    }

    @Test
    void 候选行_ClickEvent执行投票命令() {
        final LobbyVote vote = new LobbyVote(threeMaps(), new Random(9L));
        vote.beginRound(true, 3);

        final List<Component> lines = vote.candidateLines();
        assertEquals(vote.candidates().size(), lines.size(), "一行一候选");
        for (int i = 0; i < lines.size(); i++) {
            final Component line = lines.get(i);
            final ClickEvent click = line.clickEvent();
            assertNotNull(click, "候选行必带 ClickEvent");
            assertEquals(ClickEvent.Action.RUN_COMMAND, click.action(), "点击即执行命令");
            assertEquals("/shdf vote " + (i + 1), ((ClickEvent.Payload.Text) click.payload()).value(),
                    "命令串指向对应序号(payload 取值,value() 已过时不踩)");
            assertEquals("[" + (i + 1) + "] ", ((TextComponent) line).content(), "行首序号段(序号连续)");
            assertEquals(1, line.children().size(), "候选段为唯一子组件");
            assertEquals(vote.candidates().get(i), ((TextComponent) line.children().get(0)).content(),
                    "候选段=mode@map(整行即 [n] mode@map)");
        }
    }

    // ── 票面看板与观察序号(交互层渲染/刷新口,本次任务·大厅交互层) ──

    @Test
    void 票面看板_候选序全覆盖_投票人按投票先后() {
        final LobbyVote vote = new LobbyVote(threeMaps(), new Random(3L));

        assertTrue(vote.ballotBoard().isEmpty(), "未起票:看板恒空");

        vote.beginRound(true, 3);
        assertEquals(Set.of(1, 2, 3), vote.ballotBoard().keySet(), "起票后:候选序 1..N 全覆盖(零票候选含空列表)");
        vote.ballotBoard().values().forEach(voters -> assertTrue(voters.isEmpty(), "零票候选映射空列表"));

        vote.castVote(P1, 1);
        vote.castVote(P2, 2);
        vote.castVote(P3, 1);
        assertEquals(List.of(P1, P3), vote.ballotBoard().get(1), "1 号候选投票人按投票先后(P1 先 P3 后)");
        assertEquals(List.of(P2), vote.ballotBoard().get(2));
        assertTrue(vote.ballotBoard().get(3).isEmpty(), "零票候选空列表");
        assertEquals(2, vote.ballotBoard().get(1).size(), "票数=列表长度(菜单 lore 票数口径)");

        vote.castVote(P1, 2); // P1 改票:1 号失一票、2 号得票且 P1 排 P2 后(改票时刻为新投票时刻)
        assertEquals(List.of(P3), vote.ballotBoard().get(1), "改票后旧候选失票");
        assertEquals(List.of(P2, P1), vote.ballotBoard().get(2), "改票者按改票时刻排到该候选队尾");
    }

    @Test
    void 摘除候选_票面看板随新序重排() {
        final LobbyVote vote = new LobbyVote(threeMaps(), new Random(3L));
        vote.beginRound(true, 3);
        final String dead = vote.candidates().get(0);
        vote.castVote(P1, 1);

        vote.withdraw(dead);
        assertEquals(Set.of(1, 2), vote.ballotBoard().keySet(), "摘除后看板键随新候选序收紧");
        vote.ballotBoard().values().forEach(voters -> assertTrue(voters.isEmpty(), "票本随摘除清空:全候选零票"));
    }

    @Test
    void 观察序号_有效变更即推进_无效操作不动() {
        final LobbyVote vote = new LobbyVote(threeMaps(), new Random(3L));
        assertEquals(0L, vote.revision(), "初始为 0");

        vote.castVote(P1, 1);
        assertEquals(0L, vote.revision(), "未起票投票不记:序号不动");

        vote.beginRound(true, 3);
        final long afterBegin = vote.revision();
        assertEquals(1L, afterBegin, "起票(候选定格+清票)有效变更:+1");

        vote.castVote(P1, 1);
        vote.castVote(P1, 2);
        assertEquals(afterBegin + 2, vote.revision(), "两次有效记票(含改票):再 +2");

        vote.castVote(P1, 99);
        assertEquals(afterBegin + 2, vote.revision(), "越界记票无效:序号不动");

        vote.clear();
        assertEquals(afterBegin + 3, vote.revision(), "清票:+1");

        vote.withdraw("tdm@map9");
        vote.withdrawMode("conquest");
        assertEquals(afterBegin + 3, vote.revision(), "不在本轮的摘除为无操作:序号不动");
    }
}
