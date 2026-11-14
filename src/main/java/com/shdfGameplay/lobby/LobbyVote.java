package com.shdfGameplay.lobby;

import com.shdfGameplay.config.GameConfigManager;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 大厅『下一场玩什么』投票:候选集 mode@map、多数胜/平局 TieBreak.RANDOM、每人一票可改票、零票轮换兜底;纯宿主状态零框架类型。
 */
public final class LobbyVote {

    // TODO(2.5): vote-seconds 周期收票=BukkitScheduler runTaskTimer 自管节拍(非框架 tick);候选行 ClickEvent 聊天发送 —— 出自 04 §4.2 表、§14.10
    // TODO(2.5): 玩家名喂 MiniMessage 一律 Placeholder.unparsed;纯状态机离线可测(LobbyVoteTest) —— 出自 2.5 步骤3

    /** 扫描图配置生成候选行(格式 模式@图,如 bosswar@example)。 */
    public List<String> candidates(GameConfigManager games) {
        return games.all().stream().map(config -> config.modeId() + "@" + config.mapId()).toList();
    }

    /** 每人一票,可改票;choice 为候选行序号。 */
    public void castVote(UUID player, int choice) {
    }

    /** 计票:多数胜,平局 TieBreak.RANDOM;无票返回空。 */
    public Optional<String> tally() {
        return Optional.empty();
    }

    /** 零票时的轮换兜底顺序(mode@map 列表)。 */
    public List<String> fallbackOrder() {
        return List.of();
    }
}
