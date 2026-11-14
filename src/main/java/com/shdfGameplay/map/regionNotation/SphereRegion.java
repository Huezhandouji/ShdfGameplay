package com.shdfGameplay.map.regionNotation;

import org.bukkit.util.Vector;

/**
 * 球体区域:球心+半径定义,不可变值对象(getter 一律新建副本,相等按球心三分量+半径值判定)。
 * 半径允许 0(退化为点:contains 恰为球心本身、randomPoint 恒返回球心),禁止负数;
 * NaN/Infinity 一律拒绝。
 */
public final class SphereRegion extends Region {

    private final double centerX;
    private final double centerY;
    private final double centerZ;
    private final double radius;

    private SphereRegion(final double centerX, final double centerY, final double centerZ, final double radius) {
        if (Double.isNaN(radius) || Double.isInfinite(radius) || radius < 0) {
            throw new IllegalArgumentException("SphereRegion radius must be finite and non-negative: " + radius);
        }
        this.centerX = centerX;
        this.centerY = centerY;
        this.centerZ = centerZ;
        this.radius = radius;
    }

    /**
     * 由球心与半径创建。
     *
     * @param center 球心坐标
     * @param radius 半径,允许 0(退化为点)、禁止负数
     * @return 球体区域
     * @throws IllegalArgumentException radius 为负/NaN/Infinity,或球心任一分量为 NaN/Infinity
     */
    public static SphereRegion of(final Vector center, final double radius) {
        if (Double.isNaN(center.getX()) || Double.isInfinite(center.getX())
                || Double.isNaN(center.getY()) || Double.isInfinite(center.getY())
                || Double.isNaN(center.getZ()) || Double.isInfinite(center.getZ())) {
            throw new IllegalArgumentException("SphereRegion center components must be finite!");
        }
        return new SphereRegion(center.getX(), center.getY(), center.getZ(), radius);
    }

    /**
     * 球心坐标。
     *
     * @return 新建副本
     */
    public Vector getCenter() {
        return new Vector(centerX, centerY, centerZ);
    }

    /**
     * 半径。
     *
     * @return 半径值(退化点区域为 0)
     */
    public double getRadius() {
        return radius;
    }

    @Override
    protected boolean containsNonNull(final Vector point) {
        final double dx = point.getX() - centerX;
        final double dy = point.getY() - centerY;
        final double dz = point.getZ() - centerZ;
        return dx * dx + dy * dy + dz * dz <= radius * radius;
    }

    @Override
    public Vector randomPoint() {
        // 球体内均匀采样:球面均匀方向(球坐标)× 半径按体积分布(r ∝ cbrt(u));
        // radius==0 时 cbrt(0)==0,恒返回球心,无需特判
        final double theta = 2 * Math.PI * Math.random();
        final double phi = Math.acos(2 * Math.random() - 1);
        final double r = radius * Math.cbrt(Math.random());
        final double sinPhi = Math.sin(phi);
        return new Vector(
                centerX + r * sinPhi * Math.cos(theta),
                centerY + r * sinPhi * Math.sin(theta),
                centerZ + r * Math.cos(phi));
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof final SphereRegion other)) {
            return false;
        }
        return centerX == other.centerX && centerY == other.centerY
                && centerZ == other.centerZ && radius == other.radius;
    }

    @Override
    public int hashCode() {
        int result = Double.hashCode(centerX);
        result = 31 * result + Double.hashCode(centerY);
        result = 31 * result + Double.hashCode(centerZ);
        result = 31 * result + Double.hashCode(radius);
        return result;
    }

    @Override
    public String toString() {
        return "SphereRegion[center=(" + centerX + ", " + centerY + ", " + centerZ + "), radius=" + radius + "]";
    }
}
