package com.shdfGameplay.lobby;

import java.util.UUID;

/**
 * 大厅成员准备账本:全员 ready 即提前收票/立即开局信号(人数门槛仍要过)。
 */
public final class ReadyState {

    // TODO(2.5): /shdf ready 或 GUI 按钮驱动;与 MatchBootstrap 人数门并联判定 —— 出自 04 §4.2

    /** 记/清单个玩家的 ready 标记。 */
    public void setReady(UUID player, boolean ready) {
    }

    /** 全员 ready 判定(空账本视为未就绪)。 */
    public boolean allReady() {
        return false;
    }

    /** 玩家离开大厅时清标记。 */
    public void clear(UUID player) {
    }
}
