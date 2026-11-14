# map — 图配置/地图装配 Javadoc 归档

> 源码包:`src/main/java/com/shdfGameplay/map/`

---

## GameConfig

**源文件**:`src/main/java/com/shdfGameplay/map/GameConfig.java`
**保留**:单图载体定位、一文件一图、展示三键归一。

**归档(原 javadoc 详细段落与来源)**:

> metadata 为自由嵌套映射(bosswar: spawns/boss_life_number/play_region;
> conquest: hotpoints/spawns/play_region),原样透传给 ShdfMapKeys 的玩法命名空间键,
> 词料定形随各模式工厂落地。
>
> mapPath 为 map_path 字符串的 Path 形态(相对宿主数据目录,如 maps/example_world)。
> 展示三键(display_name/description/icon)供大厅投票 GUI 消费,解析时归一默认值,三者永不为 null:
> display_name 缺省回退 mapId、description 缺省空串、icon 缺省/未知材质名回退 MAP。
>
> 来源:资源目录 mode_config/ 现行结构(README 与 example.yaml);
> 04-大厅模式 §4.5 分工表(引框架 §15.2 铁律1);README 对齐注⑧⑩适用项。

---

## GameConfigManager

**源文件**:`src/main/java/com/shdfGameplay/map/GameConfigManager.java`
**保留**:init 扫描缓存、容错口径、三读口。

**归档(原 javadoc 详细段落与来源)**:

> 一模式可挂多图:byMode 取该模式全部图,all 展平给投票候选集,find 按 modeId+mapId 定位单图;
> MatchBootstrap(开局判定,胜出候选行解析回图配置)与 LobbyVote(投票候选集及 GUI 展示键)的数据源。
>
> 容错细节:单文件解析失败(空/语法错误/缺 map_path 等,GameConfig.from 响亮)跳过该文件并记一条
> warning 日志,不阻整体;重复 init() 整体换缓存(重载口径)。
>
> 来源:资源目录 mode_config/ 现行结构;04-大厅模式 §4.3 字段;README 对齐注⑧⑩适用项。

---

## ShdfMapKeys

**源文件**:`src/main/java/com/shdfGameplay/map/ShdfMapKeys.java`
**保留**:一模式一键、同袋并存、TAGS 唯一框架解释键。

**归档(原 javadoc 详细段落与来源)**:

> 值为该模式 yaml 已解析的 metadata 自由映射
> (bosswar: spawns/boss_life_number/play_region;conquest: hotpoints/spawns/play_region),
> 存入 MapDefinition.metadata 的玩法命名空间。
> 框架唯一解释键 = MapDefinition.TAGS,其余键全部宿主自定(README 对齐注⑨,v1.10/11)。
>
> 来源:资源目录 mode_config/ 现行结构;04-大厅模式 §4.5;README 对齐注⑨适用项。

---

## ShdfMapAssembler

**源文件**:`src/main/java/com/shdfGameplay/map/ShdfMapAssembler.java`
**保留**:装配管线四步、maps/ 解析口径。

**归档(原 javadoc 详细段落与来源)**:

> 构造 MapDefinition 三字段(worldDirectoryPath/resetStrategy/metadata)。
> 世界存档存放在数据目录 maps/ 下,yaml 的 map_path 相对数据目录解析(如 maps/example_world)。
>
> 来源:资源目录 maps/ 与 mode_config/ 现行结构;04-大厅模式 §4.5;README 对齐注⑦⑧⑨适用项。
