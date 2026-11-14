# ai-generated — 类头 Javadoc 精简归档

本目录归档 2026-11-10 的一次全工程整理:各类头部 javadoc 从冗长篇幅精简至 **5 行以内**,
只保留「一句话定位 + 关键契约」;被移除的详细设计说明、历史沿革与章节来源追溯收录于本目录,
供后续施工与对齐时查阅。原注释内容未丢失,仅从源码迁出。

## 分文件索引

| 文件 | 覆盖包 | 类数 |
|---|---|---|
| [javadoc-bridge-role.md](javadoc-bridge-role.md) | `bridge/role` — 角色系统防腐层 | 9 |
| [javadoc-map.md](javadoc-map.md) | `map` — 图配置/地图装配/词料键 | 4 |
| [javadoc-map-region.md](javadoc-map-region.md) | `map/region` — 区域几何体 | 3 |
| [javadoc-lobby.md](javadoc-lobby.md) | `lobby` — 大厅三件与散场 | 5 |
| [javadoc-listener.md](javadoc-listener.md) | `listener` — 事件路由层 | 2 |
| [javadoc-component.md](javadoc-component.md) | `component` / `component/role` | 3 |
| [javadoc-core.md](javadoc-core.md) | 主类 / `command` / `flow` | 3 |

## 体例

每个类一节,包含:

- **源文件**路径;
- **保留**在源码 javadoc 里的内容摘要(定位+契约);
- **归档**:被移除的原始段落全文——含详细设计理由、旧实现沿革(如旧插件 SHDFGamePlugin 的改造对照)、
  以及「来源」行(设计文档章节号、施工计划步骤号、README 对齐注),这些引用是施工对齐与验收的依据。

## 注意

- 源码类头中仍保留工程自有的 `TODO(施工阶段)` 行内注释,未在本轮处理范围内。
- 引用缩写:`04`=04-大厅模式、`08`=08-角色桥接、`09`=09-角色组件层、`2.5`/`3.x`=施工计划步骤、
  `★23`=16-路线图与验收.md:65 的分层豁免裁决、`§14.x`=14-陷阱清单。
