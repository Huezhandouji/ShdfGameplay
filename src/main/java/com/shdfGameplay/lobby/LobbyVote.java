package com.shdfGameplay.lobby;

import com.shdfGameplay.config.GameConfigManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.random.RandomGenerator;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * 大厅『下一场玩什么』投票状态机:起票抽样(禁投票抽 1 张直接用/开投票抽 mapAmount 张)、每人一票可改票、
 * 多数胜/平局 TieBreak.RANDOM(随机源注入可固定种子)、零票轮换兜底(fallbackOrder 稳定序 + consume 即推进指针);
 * 聊天候选行([n] mode@map + ClickEvent 执行 /shdf vote n)渲染归本类、发送归周期壳。
 * 纯宿主状态:零 org.bukkit 与零框架类型;候选行全 Component.text 链式构造不经 MiniMessage
 * (候选串来自服主配置,零玩家可控字符串注入面)。
 * —— 出自 2.5 大厅逻辑(04-大厅模式 §4.2、§14.10;2.5 步骤3)。
 */
public final class LobbyVote {

    /** 候选配置源(全量池:模式 id 排序、模式内 mapId 排序,见 GameConfigManager.all 稳定序)。 */
    private final GameConfigManager games;

    /** 起票抽样与平局裁决共用的随机源(注入:测试固定种子可复现,TieBreak.RANDOM 不配置化)。 */
    private final RandomGenerator random;

    /** 当前轮候选(起票抽样定格;未起票/清票后为空)。 */
    private List<String> roundCandidates = List.of();

    /** 票本:投票人 → 候选序号(1-based);后投覆盖先投=可改票,清票/重起票即清。 */
    private final Map<UUID, Integer> ballots = new HashMap<>();

    /** 轮换兜底指针:consume 即推进 +1 回绕;跨轮与清票持续(一 bootstrap 尝试一消费)。 */
    private int fallbackCursor;

    /**
     * @param games  候选配置源(起票抽样池与轮换兜底序同源)
     * @param random 随机源(起票抽样 + 平局 TieBreak.RANDOM)
     */
    public LobbyVote(final GameConfigManager games, final RandomGenerator random) {
        this.games = games;
        this.random = random;
    }

    /** 当前轮候选(未起票为空;不可变视图)。 */
    public List<String> candidates() {
        return this.roundCandidates;
    }

    /**
     * 起票:清票本并抽样定格本轮候选——voteEnabled=false 抽 1 张作唯一候选(零票即直用)、
     * true 抽 min(mapAmount, 池大小) 张去重(mapAmount≥1 由 LobbyConfig 装载层保证);空池起票=无候选。
     *
     * @param voteEnabled 是否开启投票(map_vote_setting.enabled)
     * @param mapAmount   抽样候选数(map_vote_setting.map_amount;池小于该数取全量)
     */
    public void beginRound(final boolean voteEnabled, final int mapAmount) {
        final List<String> pool = pool();
        final int amount = pool.isEmpty() ? 0 : voteEnabled ? Math.min(mapAmount, pool.size()) : 1;
        this.roundCandidates = sample(pool, amount);
        this.ballots.clear();
    }

    /**
     * 投票:本轮已起票且 1≤choice≤候选数才记,每人一票、后投覆盖先投(可改票);未起票/越界静默不记。
     *
     * @param player 投票人
     * @param choice 候选序号(1-based,对应候选行 [n])
     */
    public void castVote(final UUID player, final int choice) {
        if (choice < 1 || choice > this.roundCandidates.size()) {
            return;
        }
        this.ballots.put(player, choice);
    }

    /**
     * 计票:多数胜,平局按注入随机源在并列候选中裁决(TieBreak.RANDOM 固定,不配置化);
     * 零票时候选恰 1 张直返该候选(禁投票『直接用』口径),否则空(转轮换兜底)。非消费口,可重复调。
     *
     * @return 当选候选 mode@map;零票且非单候选为空
     */
    public Optional<String> tally() {
        if (this.ballots.isEmpty()) {
            return this.roundCandidates.size() == 1
                    ? Optional.of(this.roundCandidates.get(0))
                    : Optional.empty();
        }
        final Map<Integer, Integer> counts = new HashMap<>();
        for (final int choice : this.ballots.values()) {
            counts.merge(choice, 1, Integer::sum);
        }
        int best = 0;
        for (final int count : counts.values()) {
            best = Math.max(best, count);
        }
        final List<Integer> leaders = new ArrayList<>();
        for (final Map.Entry<Integer, Integer> entry : counts.entrySet()) {
            if (entry.getValue() == best) {
                leaders.add(entry.getKey());
            }
        }
        final int winner = leaders.size() == 1
                ? leaders.get(0)
                : leaders.get(this.random.nextInt(0, leaders.size()));
        return Optional.of(this.roundCandidates.get(winner - 1));
    }

    /** 零票轮换兜底顺序:games.all() 稳定序 mode@map(本工程无 rotation.yml,全量池即兜底序;实时读缓存,重载后随动)。 */
    public List<String> fallbackOrder() {
        return pool();
    }

    /**
     * 轮换兜底取下一项:consume 即推进(+1 回绕,每调一次),空序为空。
     *
     * @return 兜底序当前指针项;兜底序空(零配置)为空 → 本轮不开局重新起票
     */
    public Optional<String> nextFallback() {
        final List<String> order = fallbackOrder();
        if (order.isEmpty()) {
            return Optional.empty();
        }
        final String next = order.get(this.fallbackCursor);
        this.fallbackCursor = (this.fallbackCursor + 1) % order.size();
        return Optional.of(next);
    }

    /** 清票本:候选与投票归零回未起票态(周期重起/停止用);轮换指针不清(跨轮持续)。 */
    public void clear() {
        this.roundCandidates = List.of();
        this.ballots.clear();
    }

    /**
     * 摘除当前轮单张候选(04 §4.3 门表「摘除候选重收票」口径,地图门拒收自愈):该候选在本轮候选内才生效——
     * 摘除后候选序重排、票本清空(重收票,旧票随旧序一并失效);不在本轮为无操作(不动票本)。
     * 轮换指针不清(兜底序跨轮持续口径不变)。
     *
     * @param candidate 被摘候选 mode@map
     */
    public void withdraw(final String candidate) {
        if (!this.roundCandidates.contains(candidate)) {
            return;
        }
        final List<String> remaining = new ArrayList<>(this.roundCandidates);
        remaining.remove(candidate);
        this.roundCandidates = List.copyOf(remaining);
        this.ballots.clear();
    }

    /**
     * 摘除当前轮该玩法全部候选(04 §4.3 门表「摘除该玩法候选,大厅照常」口径,角色门拒收自愈):
     * 有摘除才清空票本(重收票);该玩法不在本轮候选内为无操作。
     *
     * @param modeId 被摘玩法 id(摘除其在本轮的全部 mode@map 候选)
     */
    public void withdrawMode(final String modeId) {
        final List<String> remaining = new ArrayList<>();
        for (final String candidate : this.roundCandidates) {
            if (!candidate.startsWith(modeId + "@")) {
                remaining.add(candidate);
            }
        }
        if (remaining.size() == this.roundCandidates.size()) {
            return;
        }
        this.roundCandidates = List.copyOf(remaining);
        this.ballots.clear();
    }

    /**
     * 聊天候选行渲染(发送归周期壳:起票全员广播 + 新入场者差集补发,唯一口不在本类):
     * 每行「[n] mode@map」,ClickEvent 执行 /shdf vote n;Component.text 链式构造不经 MiniMessage
     * (候选串来自服主配置文件,零玩家可控字符串,无注入面)。
     */
    public List<Component> candidateLines() {
        final List<Component> lines = new ArrayList<>();
        for (int i = 0; i < this.roundCandidates.size(); i++) {
            final int number = i + 1;
            lines.add(Component.text("[" + number + "] ", NamedTextColor.GRAY)
                    .append(Component.text(this.roundCandidates.get(i), NamedTextColor.AQUA))
                    .clickEvent(ClickEvent.runCommand("/shdf vote " + number)));
        }
        return lines;
    }

    /** 全量候选池:games.all() 稳定序展平为 mode@map(起票抽样池与轮换兜底序同源)。 */
    private List<String> pool() {
        return this.games.all().stream()
                .map(config -> config.modeId() + "@" + config.mapId())
                .toList();
    }

    /** Fisher-Yates 前缀抽样:池索引洗牌取前 amount 张(索引唯一保证去重;amount≤0 返空)。 */
    private List<String> sample(final List<String> pool, final int amount) {
        if (amount <= 0) {
            return List.of();
        }
        final List<Integer> indexes = new ArrayList<>();
        for (int i = 0; i < pool.size(); i++) {
            indexes.add(i);
        }
        for (int i = indexes.size() - 1; i > 0; i--) {
            final int j = this.random.nextInt(0, i + 1);
            final int swap = indexes.get(i);
            indexes.set(i, indexes.get(j));
            indexes.set(j, swap);
        }
        final List<String> picked = new ArrayList<>(amount);
        for (int i = 0; i < amount; i++) {
            picked.add(pool.get(indexes.get(i)));
        }
        return List.copyOf(picked);
    }
}
