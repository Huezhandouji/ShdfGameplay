package com.shdfGameplay.mode.bomb;

/**
 * 炸弹平衡参数载体(v1.0 旧口径 modes/bomb_defusal.yml 的参数集;v2.0 起改读 games/bomb_defusal/game.yml)+ 声明式规则簿数据容器。
 * 来源:05-四模式矩阵 §5.4/§5.8;6.1-炸弹攻防 步骤 2;README 对齐注⑧(配置文件声明通道退场,宿主 games/&lt;玩法&gt;/ 自解析)。
 */
public record BombConfig(
        int bestOf,
        int halfSwapAfter,
        int plantSeconds,
        int fuseSeconds,
        int defuseSeconds,
        double defuseSecondsWithKit,
        double explodeRadius) {

    /**
     * 平衡参数骨架桩工厂(返回文档缺省值:best-of 13 / half-swap-after 6 / plant 3s / fuse 40s / defuse 7s / kit 3.5s / explode-radius 6.0;
     * 框架 MatchModeConfig 配置通道已全量删除,宿主参数改经 games/&lt;玩法&gt;/game.yml 自解析——出自 框架 v3.0.0(2026-11-16 重建)破坏性更新)。
     *
     * @return 平衡参数载体
     */
    public static BombConfig from() {
        // TODO(6.2): 解析 games/bomb_defusal/game.yml 参数(经 GameConfig 闭包注入);容错三铁律——缺键默认、非法值降级 WARN、带文件名报错 —— 出自 6.2-炸弹攻防;05-四模式矩阵 §5.4;2.5 步骤 7(GameConfigManager 装配管线)。
        return new BombConfig(13, 6, 3, 40, 7, 3.5, 6.0);
    }

    /**
     * 声明式规则簿数据容器:bomb.planted.confirm 等键,入框架 RuleBookComponent$Spec(List&lt;Rule&gt;) 挂载。
     * 来源:05-四模式矩阵 §5.2 规则示例;6.1-炸弹攻防 步骤 7。
     */
    public static final class BombRules {

        // TODO(6.1): 规则簿落位——bomb.planted.confirm / bomb.fuse.exploded / bomb.defused / bomb.round.decided / 换边占位,纯函数离线可测;经框架 RuleBookComponent$Spec(List&lt;Rule&gt;) 挂载(javap 已核 Rule(id,trigger,when,then));播报文案玩家名/站点名走 Placeholder.unparsed —— 出自 6.1-炸弹攻防 步骤 7;05-四模式矩阵 §5.2。
        private BombRules() {
        }
    }
}
