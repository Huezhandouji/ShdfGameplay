package com.shdfGameplay.lobby;

import com.hydrokarbs.api.SessionEventView;
import java.util.function.Consumer;

/**
 * 散场单闸:全工程唯一 subscribeSessionEvents 订阅点——Dissolved 接引收尾、MemberLeft 单人传送回大厅、ModeSwitchCompleted(to=limbo) 记待散信号。
 */
public final class LobbyReturnListener implements Consumer<SessionEventView> {

    // TODO(2.5): onDisable close 订阅句柄幂等;账本与框架名册同步点仅 join 成功/MemberLeft/Dissolved 三处 —— 出自 04 §4.8
    // TODO(2.5): 框架解散先传送后发 Dissolved——宿主以散场引导名单+MemberLeft 兜底,两段传送(主世界出生点→大厅 spawn)无缝衔接;REUSE_DIRTY 副本随解散销毁宿主零感知 —— 出自 04 §4.6

    /** 唯一订阅点回调:只按事件子类路由/记账,不做业务之外的动作。 */
    @Override
    public void accept(SessionEventView event) {
    }
}
