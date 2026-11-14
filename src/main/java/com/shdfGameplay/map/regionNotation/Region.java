package com.shdfGameplay.map.regionNotation;

import org.bukkit.util.Vector;

/**
 * 区域抽象:不可变几何体值对象——返回 {@link Vector} 一律新建副本、相等按量值判定可作 Map 键;
 * 允许退化(平面/线段/点),退化轴上包含判定为坐标精确相等、随机采样恒落退化集合内。
 * {@link #contains} 为模板方法收口"null 不包含"契约,子类只在 {@link #containsNonNull} 实现几何判定。
 */
public abstract class Region {

    /** 基类无状态,子类以原始 double 自持几何量。 */
    protected Region() {
    }

    /**
     * 几何中心。
     *
     * @return 中心点(新建副本,修改不影响本区域)
     */
    public abstract Vector getCenter();

    /**
     * 点包含判定;退化区域在退化轴上退化为坐标精确相等(见类头)。
     *
     * @param point 待判定点,null 一律不包含
     * @return 点在区域内返回 true
     */
    public final boolean contains(final Vector point) {
        return point != null && containsNonNull(point);
    }

    /**
     * 非空点的几何包含判定——模板方法 {@link #contains} 已收口 null 分支,子类无需再记。
     *
     * @param point 待判定点,调用方保证非 null
     * @return 点在区域内返回 true
     */
    protected abstract boolean containsNonNull(Vector point);

    /**
     * 区域内均匀随机采样一点;退化区域恒返回退化集合上的点。
     *
     * @return 采样点(新建副本,永不为 null)
     */
    public abstract Vector randomPoint();
}
