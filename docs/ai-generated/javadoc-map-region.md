# map/region — 区域几何体 Javadoc 归档

> 源码包:`regionNotation`
> 源头:三类均参照旧插件 SHDFGamePlugin infrastructure/regionNotation/ 同名类改造。

---

## Region

**源文件**:`regionNotation`
**保留**:不可变值对象、退化允许、模板方法收口 null 契约。

**归档(原 javadoc 详细段落)**:

> 值对象细节:构造后状态永不改变,凡返回 Vector 处一律新建副本,外部对返回值的修改不回流区域本身;
> 相等按几何量值判定,可安全用作 Map 键。
> 退化细节:CubeRegion 可退化为平面/线段/点,SphereRegion 可退化为点;
> 退化轴上的包含判定为坐标精确相等,随机采样恒落在退化集合内(平面采到面上、点采到点本身)。
>
> 抽象类形态的分工:基类无几何状态(子类各自以原始 double 存储最适合自身的量,
> 球存球心+半径、长方体存两角点,中心一方派生一方存储,不强行上提),仅收口"null 不包含"契约。
>
> 来源:旧插件 SHDFGamePlugin infrastructure/regionNotation/Region(参照其方法形态;
> 因不可变化移除 copy()——副本无意义,原样共享即安全;抽象类+null 契约模板方法为本轮改造)。
> 词料消费方为 map 元数据 play_region/spawns/hotpoints 的 typed 定形(ShdfMapKeys TODO,见 SKELETON ④配置模块行)。

---

## CubeRegion

**源文件**:`regionNotation`
**保留**:min/max 角点、退化规则、校验口径。

**归档(原 javadoc 详细段落)**:

> 退化细节:如 `ofCorners(p, p)` 即点区域;退化轴上 contains 为精确相等、randomPoint 恒取该轴定值。
> 不可变:角点以六个原始 double 存储,getter 一律新建 Vector 副本。
>
> 来源:旧插件 SHDFGamePlugin infrastructure/regionNotation/CubeRegion(原 origin+size 存储、
> size 分量须严格为正;本实现改 min/max 角点存储、放开 0 分量以支持退化,
> 创建工厂与校验口径承接原 createFromCorners/createFromOriginAndSize)。

---

## SphereRegion

**源文件**:`regionNotation`
**保留**:球心+半径、半径 0 退化、校验口径。

**归档(原 javadoc 详细段落)**:

> 半径 0 退化语义:contains 为到球心距离平方 ≤0 即恰为球心本身,randomPoint 恒返回球心。
> 不可变:球心以三个原始 double 存储,getter 一律新建 Vector 副本。
>
> 来源:旧插件 SHDFGamePlugin infrastructure/regionNotation/SphereRegion(参照写法;
> 半径校验由"严格为正"放开为"非负"以支持退化为点,原 create 工厂更名 of)。
