package com.shdfGameplay.lang;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * i18n 静态门面:全插件文案唯一出口——玩家可见文本走 {@link #render}(MiniMessage 模板→Component),
 * 日志/异常消息走 {@link #plain}(render 后经纯文本序列化,样式标签剥除)。
 * 模板住 jar 内 lang/&lt;语言&gt;.yml(顶层点分键,样式标签写在模板里),init 以数据目录同名文件逐键覆盖在上
 * (老安装缺新键自动回退 jar 默认);未 init 时首次调用懒加载 jar 内默认语言(线程安全单飞,单测零装配可直接用)。
 *
 * <p>调用口径:键名字面量与调用同行(如 {@code render("a.b.c", ...)},保证 grep 可审计);
 * 运行期值(玩家名/地图名/路径等)一律经 {@code Placeholder.unparsed("占位名", 值)} 注入、严禁字符串拼进模板
 * (防 MiniMessage 注入;本工程 MiniMessage 4.26.1 的 {@code unparsed} 只收 (String, String),数值先
 * {@code String.valueOf});模板内命名占位符写作 {@code <占位名>} 标签,字面 {@code "<"} 写作 {@code "\<"};
 * 无占位静态文案直接 {@code render("键名")}。</p>
 *
 * <p>占位形参类型勘误:规格原文写作 {@code net.kyori.adventure.text.minimessage.Tag},该 FQN 在本工程
 * 依赖(adventure-text-minimessage 4.26.1,经 paper-api 1.21.11 传递)中不存在——javap 已核:4.10 起
 * {@code Tag} 移至 tag 子包且为语法树节点类型,占位注入类型为 {@link TagResolver}
 * ({@code MiniMessage#deserialize(String, TagResolver...)} 为唯一变参口径,{@code Placeholder.unparsed}
 * 返回 {@code TagResolver$Single})。故本类形参取 {@link TagResolver},调用侧
 * {@code render("键", Placeholder.unparsed(...))} 传参不受影响。</p>
 *
 * <p>自举例外:本类自身数条 warning(语言装载失败/缺键回退诊断)为硬编码中文、不经本门面——
 * 它们恰在语言模板不可用时触发,走模板会退化成裸键名,诊断价值尽失。</p>
 */
public final class Lang {

    /** 类级日志口(JUL→console;缺键警告走此,不依赖插件实例在场)。 */
    private static final Logger LOGGER = Logger.getLogger(Lang.class.getName());

    /** 全工程唯一 MiniMessage 实例(模板反序列化只经此一处)。 */
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    /** 纯文本序列化器(plain 口专用,render 结果→String)。 */
    private static final PlainTextComponentSerializer PLAIN_SERIALIZER = PlainTextComponentSerializer.plainText();

    /** 缺省语言(config.yml language 键缺省值,亦是配置语言文件缺失时的回退语言)。 */
    private static final String DEFAULT_LANGUAGE = "zh_CN";

    /** 模板缓存(点分键→MiniMessage 模板;null=未装填,render/plain 首调懒加载 jar 默认)。 */
    private static volatile Map<String, String> templates;

    /** 装填互斥锁(init 与懒加载单飞共用;私有锁对象,不锁本类 class 防外部干扰)。 */
    private static final Object LOAD_LOCK = new Object();

    /** 缺键警告账本(每键只警告一次,进程生命周期内不重置)。 */
    private static final Set<String> WARNED_KEYS = ConcurrentHashMap.newKeySet();

    private Lang() {
    }

    /**
     * 装填语言模板(装配序首行调用,先于任何文案消费):
     * language 键取数据目录 config.yml(缺省 {@value #DEFAULT_LANGUAGE},数据文件缺席回退 jar 内默认 config.yml);
     * 以 jar 内 lang/&lt;语言&gt;.yml 为底、数据目录 lang/&lt;语言&gt;.yml 逐键覆盖在上;
     * 配置语言的 jar 内语言文件缺失时回退 {@value #DEFAULT_LANGUAGE} 并打一条 warning。
     * 幂等:重复调用重读并整体换缓存,同输入同结果(插件重载取新配置走同一入口)。
     *
     * @param plugin 宿主插件(数据目录与 jar 资源口)
     */
    public static void init(final JavaPlugin plugin) {
        final String language = configuredLanguage(plugin);
        final Map<String, String> loaded = loadForLanguage(plugin, language);
        synchronized (LOAD_LOCK) {
            templates = loaded;
        }
    }

    /**
     * 玩家可见文本:模板经全工程唯一 MiniMessage 实例反序列化为 Component。
     * 缺键回退红字键名 {@code Component.text(key, RED)} 并对每个键只警告一次(console)。
     *
     * @param key          语言键(顶层点分键;字面量与调用同行,保证 grep 可审计)
     * @param placeholders 命名占位符(经 {@code Placeholder.unparsed("占位名", 值)} 注入,模板内写作 {@code <占位名>})
     * @return 渲染结果(样式标签已在模板内,调用侧不再构造颜色/装饰)
     */
    public static Component render(final String key, final TagResolver... placeholders) {
        final String template = templateOf(key);
        if (template == null) {
            if (WARNED_KEYS.add(key)) {
                LOGGER.warning("语言键缺失,回退红字键名:" + key);
            }
            return Component.text(key, NamedTextColor.RED);
        }
        return MINI_MESSAGE.deserialize(template, placeholders);
    }

    /**
     * 日志/异常消息:render 后经 PlainTextComponentSerializer 序列化为纯文本 String(样式标签剥除)。
     *
     * @param key          语言键(同 render 口径)
     * @param placeholders 命名占位符(同 render 口径)
     * @return 纯文本(无样式;拼进日志与异常消息安全)
     */
    public static String plain(final String key, final TagResolver... placeholders) {
        return PLAIN_SERIALIZER.serialize(render(key, placeholders));
    }

    /** 取模板;未装填时先懒加载 jar 内默认语言(单飞)。 */
    private static String templateOf(final String key) {
        Map<String, String> loaded = templates;
        if (loaded == null) {
            ensureLoaded();
            loaded = templates;
        }
        return loaded.get(key);
    }

    /** 懒加载 jar 内默认语言(未 init 时首调触发;双重检查单飞,线程安全只载一次)。 */
    private static void ensureLoaded() {
        if (templates != null) {
            return;
        }
        synchronized (LOAD_LOCK) {
            if (templates != null) {
                return;
            }
            templates = loadDefaultLanguage();
        }
    }

    /**
     * language 键解析:数据目录 config.yml → jar 内默认 config.yml → 缺省值
     * (文件缺席/键缺失/空值同缺省;带病 YAML 由 loadConfiguration 静默容错为空配置,同样落缺省)。
     */
    private static String configuredLanguage(final JavaPlugin plugin) {
        final Path dataConfig = plugin.getDataFolder().toPath().resolve("config.yml");
        YamlConfiguration yaml = null;
        if (Files.exists(dataConfig)) {
            yaml = YamlConfiguration.loadConfiguration(dataConfig.toFile());
        } else {
            try (InputStream in = plugin.getResource("config.yml")) {
                if (in != null) {
                    yaml = YamlConfiguration.loadConfiguration(utf8Reader(in));
                }
            } catch (final IOException e) {
                LOGGER.warning("jar 内默认 config.yml 读取失败,language 取缺省 " + DEFAULT_LANGUAGE + ":" + e.getMessage());
            }
        }
        final String configured = yaml == null ? null : yaml.getString("language");
        return configured == null || configured.isBlank() ? DEFAULT_LANGUAGE : configured.trim();
    }

    /**
     * 某语言的完整装填:jar 内语言文件为底,数据目录同名文件逐键覆盖在上
     * (覆盖语言随有效底走——配置语言缺席回退默认语言后,覆盖亦取默认语言的同名文件)。
     */
    private static Map<String, String> loadForLanguage(final JavaPlugin plugin, final String language) {
        Map<String, String> base = loadJarLanguage(plugin, language);
        if (base == null) {
            LOGGER.warning("语言文件 lang/" + language + ".yml 不在插件 jar 内,回退默认语言 " + DEFAULT_LANGUAGE);
            base = loadJarLanguage(plugin, DEFAULT_LANGUAGE);
            if (base == null) {
                LOGGER.warning("默认语言文件 lang/" + DEFAULT_LANGUAGE + ".yml 亦不在插件 jar 内,语言模板为空(全部文案将走缺键回退)");
                return Map.of();
            }
            return withOverlay(plugin, DEFAULT_LANGUAGE, base);
        }
        return withOverlay(plugin, language, base);
    }

    /** 数据目录 lang/&lt;语言&gt;.yml 逐键覆盖在 jar 底之上(文件缺席=纯 jar 底;返回不可变快照)。 */
    private static Map<String, String> withOverlay(final JavaPlugin plugin, final String language,
                                                   final Map<String, String> base) {
        final Path overlayFile = plugin.getDataFolder().toPath().resolve("lang").resolve(language + ".yml");
        if (Files.notExists(overlayFile)) {
            return Map.copyOf(base);
        }
        final Map<String, String> merged = new LinkedHashMap<>(base);
        merged.putAll(flatEntries(YamlConfiguration.loadConfiguration(overlayFile.toFile())));
        return Map.copyOf(merged);
    }

    /** jar 内某语言文件装填(init 口径,经 JavaPlugin#getResource 取本插件 jar 资源;文件缺席返 null)。 */
    private static Map<String, String> loadJarLanguage(final JavaPlugin plugin, final String language) {
        try (InputStream in = plugin.getResource("lang/" + language + ".yml")) {
            return in == null ? null : flatEntries(YamlConfiguration.loadConfiguration(utf8Reader(in)));
        } catch (final IOException e) {
            LOGGER.warning("jar 内语言文件 lang/" + language + ".yml 读取失败:" + e.getMessage());
            return null;
        }
    }

    /** classpath 默认语言文件装填(懒加载口径,无插件实例;缺席/失败=空集并 warning,不炸调用方)。 */
    private static Map<String, String> loadDefaultLanguage() {
        try (InputStream in = Lang.class.getResourceAsStream("/lang/" + DEFAULT_LANGUAGE + ".yml")) {
            if (in == null) {
                LOGGER.warning("classpath 缺 /lang/" + DEFAULT_LANGUAGE + ".yml,懒加载得到空模板集(全部文案将走缺键回退)");
                return Map.of();
            }
            return Map.copyOf(flatEntries(YamlConfiguration.loadConfiguration(utf8Reader(in))));
        } catch (final IOException e) {
            LOGGER.warning("classpath 语言文件 /lang/" + DEFAULT_LANGUAGE + ".yml 懒加载失败:" + e.getMessage());
            return Map.of();
        }
    }

    /** UTF-8 字符流(语言文件含中文,YAML 读取统一走 Reader 口,不做平台默认编码赌注)。 */
    private static InputStreamReader utf8Reader(final InputStream in) {
        return new InputStreamReader(in, StandardCharsets.UTF_8);
    }

    /**
     * 语言文件摊平为点分键→模板串:语言文件不嵌套(顶层点分键),但 Bukkit 装载把点分键按路径分节,
     * 故取深值集(getValues(true))再摊平回点分键;非字符串值(节节点/列表等)跳过。
     */
    private static Map<String, String> flatEntries(final YamlConfiguration yaml) {
        final Map<String, String> entries = new LinkedHashMap<>();
        for (final Map.Entry<String, Object> entry : yaml.getValues(true).entrySet()) {
            if (entry.getValue() instanceof final String template) {
                entries.put(entry.getKey(), template);
            }
        }
        return entries;
    }
}
