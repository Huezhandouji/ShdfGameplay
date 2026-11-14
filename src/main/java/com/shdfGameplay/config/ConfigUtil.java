package com.shdfGameplay.config;

import com.shdfGameplay.map.regionNotation.CubeRegion;
import com.shdfGameplay.map.regionNotation.Region;
import com.shdfGameplay.map.regionNotation.SphereRegion;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bukkit.util.Vector;

/**
 * 配置解析工具:metadata 自由嵌套映射({@link GameConfig} deepValues 产物)→ 玩法侧值对象。
 * 区域解析入口为 {@link #parseRegion}(按 type 分发),各具体表示法亦可单独调用。
 * 非法配置一律响亮抛 {@link IllegalStateException},与 GameConfig.from 的带病口径一致,跳过与否由上层决定。
 */
public final class ConfigUtil {

    /** 静态类，禁止实例化 */
    private ConfigUtil() {}

    /**
     * 解析任意类型的区域表示,按 type 键分发到具体表示法。
     *
     * @param rawConfigLines region 字典展开的条目(type 及该类型所需的其余键)
     * @return 对应的区域值对象(具体子类视 type 而定)
     * @throws IllegalStateException region 为空或 type 未知/缺键(具体键校验由分发目标抛出)
     */
    public static Region parseRegion(final Map<String, Object> rawConfigLines) {
        if (rawConfigLines == null || rawConfigLines.isEmpty()) {
            throw new IllegalStateException("region config is empty");
        }
        final Object type = rawConfigLines.get("type");
        if ("cube".equals(type)) {
            return parseCubeRegion(rawConfigLines);
        }
        if ("sphere".equals(type)) {
            return parseSphereRegion(rawConfigLines);
        }
        throw new IllegalStateException("region config type must be \"cube\" or \"sphere\": " + type);
    }

    /**
     * 从 region 字典条目解析长方体区域。
     *
     * <p>支持两种表示法(见 docs/玩法设计/bosswar.md):form=corners 为 corner1/corner2 两个 [x, y, z] 对角点键
     * (输入顺序与方向不限,交由 {@link CubeRegion#ofCorners} 按轴归一);form=origin_and_size 为
     * origin(最小角)与 size(各轴长度,允许 0、禁止负数)两个 [x, y, z]。</p>
     *
     * @param rawConfigLines region 字典展开的条目(type/form/corner1、corner2 或 origin/size;分量整数小数混排均收)
     * @return 解析出的长方体区域
     * @throws IllegalStateException region 为空、type 非 cube、form 未知、坐标分量缺失/非数字,
     *                               或数值被 CubeRegion 拒收(负长度/NaN/Infinity,统一转译为配置错误口径)
     */
    public static CubeRegion parseCubeRegion(final Map<String, Object> rawConfigLines) {
        if (rawConfigLines == null || rawConfigLines.isEmpty()) {
            throw new IllegalStateException("region config is empty");
        }
        if (!"cube".equals(rawConfigLines.get("type"))) {
            throw new IllegalStateException("region config type must be \"cube\": " + rawConfigLines.get("type"));
        }
        try {
            final Object form = rawConfigLines.get("form");
            if ("corners".equals(form)) {
                return CubeRegion.ofCorners(
                        parseVector(rawConfigLines.get("corner1"), "corner1"),
                        parseVector(rawConfigLines.get("corner2"), "corner2"));
            }
            if ("origin_and_size".equals(form)) {
                return CubeRegion.ofOriginAndSize(
                        parseVector(rawConfigLines.get("origin"), "origin"),
                        parseVector(rawConfigLines.get("size"), "size"));
            }
        } catch (final IllegalArgumentException e) {
            // CubeRegion 工厂拒收(负长度/NaN/Infinity 等)在此转译为配置错误口径,上层只捕一种类型
            throw new IllegalStateException("region config has invalid value: " + e.getMessage(), e);
        }
        throw new IllegalStateException("region config form must be \"corners\" or \"origin_and_size\": " + rawConfigLines.get("form"));
    }

    /**
     * 从 region 字典条目解析球体区域。
     *
     * <p>表示法(见 docs/玩法设计/bosswar.md play_region):type=sphere,centre 为球心 [x, y, z],
     * radius 为数字标量(允许 0 退化为点、禁止负数)。</p>
     *
     * @param rawConfigLines region 字典展开的条目(type/centre/radius;分量整数小数混排均收)
     * @return 解析出的球体区域
     * @throws IllegalStateException region 为空、type 非 sphere、centre 非三元素数字列表、
     *                               radius 缺键/非数字,或数值被 SphereRegion 拒收(负半径/NaN/Infinity,
     *                               统一转译为配置错误口径)
     */
    public static SphereRegion parseSphereRegion(final Map<String, Object> rawConfigLines) {
        if (rawConfigLines == null || rawConfigLines.isEmpty()) {
            throw new IllegalStateException("region config is empty");
        }
        if (!"sphere".equals(rawConfigLines.get("type"))) {
            throw new IllegalStateException("region config type must be \"sphere\": " + rawConfigLines.get("type"));
        }
        final Object radiusRaw = rawConfigLines.get("radius");
        if (!(radiusRaw instanceof final Number radius)) {
            throw new IllegalStateException("region config \"radius\" is not a number: " + radiusRaw);
        }
        try {
            return SphereRegion.of(parseVector(rawConfigLines.get("centre"), "centre"), radius.doubleValue());
        } catch (final IllegalArgumentException e) {
            // SphereRegion 工厂拒收(负半径/NaN/Infinity 等)在此转译为配置错误口径,上层只捕一种类型
            throw new IllegalStateException("region config has invalid value: " + e.getMessage(), e);
        }
    }

    /** 下钻取子字典:deepValues 产物嵌套层均为 Map&lt;String, Object&gt;,强转安全(数字键等异形由消息暴露)。 */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> section(final Map<String, Object> rawConfigLines, final String key) {
        final Object value = rawConfigLines.get(key);
        if (!(value instanceof final Map<?, ?> sub)) {
            throw new IllegalStateException("config \"" + key + "\" must be a section: " + value);
        }
        return (Map<String, Object>) sub;
    }

    /**
     * 读必填整数标量。
     *
     * @throws IllegalStateException 键缺席或非数字(小数截断取整)
     */
    public static int getInt(final Map<String, Object> rawConfigLines, final String key) {
        final Object value = rawConfigLines.get(key);
        if (!(value instanceof final Number number)) {
            throw new IllegalStateException("config \"" + key + "\" is not a number: " + value);
        }
        return number.intValue();
    }

    /**
     * 读必填长整数标量(tick 计时等;YAML 整数 Integer/Long 均收)。
     *
     * @throws IllegalStateException 键缺席或非数字
     */
    public static long getLong(final Map<String, Object> rawConfigLines, final String key) {
        final Object value = rawConfigLines.get(key);
        if (!(value instanceof final Number number)) {
            throw new IllegalStateException("config \"" + key + "\" is not a number: " + value);
        }
        return number.longValue();
    }

    /**
     * 读必填布尔标量。
     *
     * @throws IllegalStateException 键缺席或非布尔(YAML 的 "true"/"false" 字符串不收)
     */
    public static boolean getBoolean(final Map<String, Object> rawConfigLines, final String key) {
        final Object value = rawConfigLines.get(key);
        if (!(value instanceof final Boolean bool)) {
            throw new IllegalStateException("config \"" + key + "\" is not a boolean: " + value);
        }
        return bool;
    }

    /**
     * 读必填字符串列表(元素逐一验型)。
     *
     * @return 只读副本
     * @throws IllegalStateException 键缺席、非列表或任一元素非字符串
     */
    public static List<String> getStringList(final Map<String, Object> rawConfigLines, final String key) {
        final Object value = rawConfigLines.get(key);
        if (!(value instanceof final List<?> list)) {
            throw new IllegalStateException("config \"" + key + "\" must be a list: " + value);
        }
        final List<String> out = new ArrayList<>(list.size());
        for (int i = 0; i < list.size(); i++) {
            if (!(list.get(i) instanceof final String element)) {
                throw new IllegalStateException("config \"" + key + "\" element " + (i + 1) + " is not a string: " + list.get(i));
            }
            out.add(element);
        }
        return List.copyOf(out);
    }

    /** [x, y, z] 列表 → Vector:恰好 3 个数字分量(定位消息用 what 标注所在键)。 */
    private static Vector parseVector(final Object raw, final String what) {
        if (!(raw instanceof final List<?> coords) || coords.size() != 3) {
            throw new IllegalStateException("region config \"" + what + "\" must be a 3-element [x, y, z] list: " + raw);
        }
        final double[] xyz = new double[3];
        for (int i = 0; i < xyz.length; i++) {
            if (!(coords.get(i) instanceof final Number number)) {
                throw new IllegalStateException("region config \"" + what + "\" component " + (i + 1) + " is not a number: " + coords.get(i));
            }
            xyz[i] = number.doubleValue();
        }
        return new Vector(xyz[0], xyz[1], xyz[2]);
    }

}
