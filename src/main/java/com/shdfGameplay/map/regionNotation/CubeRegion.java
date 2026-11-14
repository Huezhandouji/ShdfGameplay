package com.shdfGameplay.map.regionNotation;

import org.bukkit.util.Vector;

/**
 * 长方体区域:min/max 两对角点定义(各轴 min&le;max),不可变值对象,相等按六分量值判定。
 * 允许退化:一轴 0 为平面、两轴 0 为线段、三轴 0 为点,退化轴上 contains 精确相等。
 * 负长度不允许(输入无序走 {@link #ofCorners} 自动归一);NaN/Infinity 一律拒绝。
 */
public final class CubeRegion extends Region {

    private final double minX;
    private final double minY;
    private final double minZ;
    private final double maxX;
    private final double maxY;
    private final double maxZ;

    private CubeRegion(final double minX, final double minY, final double minZ,
                       final double maxX, final double maxY, final double maxZ) {
        // 六分量有限 + min<=max 的不变式收口在构造器:NaN/Inf 经 Math.min/max 会原样传播
        // 且与比较运算恒为 false,工厂层拦不住,必须在此统一兜底
        if (!isFinite(minX, minY, minZ, maxX, maxY, maxZ)) {
            throw new IllegalArgumentException("CubeRegion components must all be finite!");
        }
        if (minX > maxX || minY > maxY || minZ > maxZ) {
            throw new IllegalArgumentException("CubeRegion requires min <= max on every axis!");
        }
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
    }

    /**
     * 由任意两对角点创建:内部按轴归一(min/max),输入顺序与方向不限。
     *
     * @param a 对角点一(与 b 重合即得点区域)
     * @param b 对角点二
     * @return 归一后的长方体区域
     * @throws IllegalArgumentException 任一分量为 NaN/Infinity
     */
    public static CubeRegion ofCorners(final Vector a, final Vector b) {
        return new CubeRegion(
                Math.min(a.getX(), b.getX()), Math.min(a.getY(), b.getY()), Math.min(a.getZ(), b.getZ()),
                Math.max(a.getX(), b.getX()), Math.max(a.getY(), b.getY()), Math.max(a.getZ(), b.getZ()));
    }

    /**
     * 由原点(最小角)与各轴长度创建。
     *
     * @param origin 最小角坐标
     * @param size   各轴长度,分量允许 0(退化)、禁止负数
     * @return 长方体区域
     * @throws IllegalArgumentException size 任一分量为负/NaN/Infinity
     */
    public static CubeRegion ofOriginAndSize(final Vector origin, final Vector size) {
        final double sx = requireLength(size.getX(), "size.x");
        final double sy = requireLength(size.getY(), "size.y");
        final double sz = requireLength(size.getZ(), "size.z");
        return new CubeRegion(
                origin.getX(), origin.getY(), origin.getZ(),
                origin.getX() + sx, origin.getY() + sy, origin.getZ() + sz);
    }

    /** 长度分量校验:有限且非负(0 即退化,负数为调用方错误)。 */
    private static double requireLength(final double value, final String name) {
        if (Double.isNaN(value) || Double.isInfinite(value) || value < 0) {
            throw new IllegalArgumentException("CubeRegion " + name + " must be finite and non-negative: " + value);
        }
        return value;
    }

    private static boolean isFinite(final double... values) {
        for (final double value : values) {
            if (Double.isNaN(value) || Double.isInfinite(value)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 最小角坐标。
     *
     * @return 新建副本
     */
    public Vector getMin() {
        return new Vector(minX, minY, minZ);
    }

    /**
     * 最大角坐标。
     *
     * @return 新建副本
     */
    public Vector getMax() {
        return new Vector(maxX, maxY, maxZ);
    }

    @Override
    public Vector getCenter() {
        return new Vector((minX + maxX) / 2d, (minY + maxY) / 2d, (minZ + maxZ) / 2d);
    }

    @Override
    protected boolean containsNonNull(final Vector point) {
        return point.getX() >= minX && point.getX() <= maxX
                && point.getY() >= minY && point.getY() <= maxY
                && point.getZ() >= minZ && point.getZ() <= maxZ;
    }

    @Override
    public Vector randomPoint() {
        // 长度为 0 的轴 Math.random()*0==0,采样恒落在退化集合上,无需特判
        return new Vector(
                minX + Math.random() * (maxX - minX),
                minY + Math.random() * (maxY - minY),
                minZ + Math.random() * (maxZ - minZ));
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof final CubeRegion other)) {
            return false;
        }
        return minX == other.minX && minY == other.minY && minZ == other.minZ
                && maxX == other.maxX && maxY == other.maxY && maxZ == other.maxZ;
    }

    @Override
    public int hashCode() {
        int result = Double.hashCode(minX);
        result = 31 * result + Double.hashCode(minY);
        result = 31 * result + Double.hashCode(minZ);
        result = 31 * result + Double.hashCode(maxX);
        result = 31 * result + Double.hashCode(maxY);
        result = 31 * result + Double.hashCode(maxZ);
        return result;
    }

    @Override
    public String toString() {
        return "CubeRegion[min=(" + minX + ", " + minY + ", " + minZ + "), max=(" + maxX + ", " + maxY + ", " + maxZ + ")]";
    }
}
