package com.shdfGameplay.lobby;

import com.hydrokarbs.api.SessionRecipe;
import com.hydrokarbs.api.view.MapInfo;
import com.hydrokarbs.api.view.MatchModeInfo;
import com.hydrokarbs.component.session.EmptyArenaComponent;
import com.hydrokarbs.engine.component.SessionComponent;
import com.hydrokarbs.engine.session.JoinResult;
import com.shdfGameplay.component.MatchConfigComponent;
import com.shdfGameplay.config.GameConfigManager;
import com.shdfGameplay.config.SpecificConfigParsers;
import com.shdfGameplay.session.SessionRecipes;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import java.util.stream.Stream;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.World;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 开局单闸测试:①文本扫描冻结——全工程 src/main(注释与字符串字面量剥离)建会话调用恰一处
 * 且在 MatchBootstrap、订阅会话事件恰一处且在 Assembly、lobby 包框架 import 恰白名单四类
 * 且零组件 import(04 §4.8 单闸/零依赖验收命题);②五门行为——任一不过不开局(零建会话)、
 * 配置解析失败拒收全员留大厅、join 被拒者留大厅不移账、join 成功立即移账、成功路径配方钉图与
 * 组件追加序(防双挂)。GameConfigManager 离线装填:其 init() 必经 LobbyConfig 世界在场检查
 * (Bukkit.getWorld→Bukkit.server,离线为 null 已实证 NPE),测试内经反射临时挂仅答 getWorld 的
 * 假 Server、装填毕即还原原值,不外溢其他用例(反射仅测试面,主源集零 Bukkit 静态接触)。
 * —— 出自 2.5 大厅逻辑(04-大厅模式 §4.3/§4.8);2.5 步骤5。
 */
class MatchBootstrapGateTest {

    private static final UUID U1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID U2 = UUID.fromString("00000000-0000-0000-0000-000000000002");

    private static final String MODE_ID = "bosswar";
    private static final String MAP_ID = "example";
    private static final String CANDIDATE = MODE_ID + "@" + MAP_ID;

    /** 扫描根(测试工作目录=工程根)。 */
    private static final Path MAIN_SOURCE_ROOT = Path.of("src", "main", "java");

    /** lobby 包框架 import 白名单(方案冻结四类)。 */
    private static final Set<String> FRAMEWORK_IMPORT_WHITELIST = Set.of(
            "com.hydrokarbs.api.HydrokarbsApi",
            "com.hydrokarbs.api.SessionEventView",
            "com.hydrokarbs.api.SessionRecipe",
            "com.hydrokarbs.engine.session.JoinResult");

    /** 登记用静态配方(与 L0 装配口径一致:不钉图、空图池、仅空场解散组件)。 */
    private static final SessionRecipe STATIC_RECIPE = new SessionRecipe(MODE_ID, Optional.empty(), List.of(),
            List.of(new EmptyArenaComponent.Spec(Duration.ofSeconds(10))));

    /** 最小主配置(lobby 块几何必需键齐全,世界名由假 Server 应答)。 */
    private static final String CONFIG_YML = """
            map_mode_config: "mode_config/map_modes.yaml"
            lobby:
              bounds:
                world: world
                type: "cube"
                form: "corners"
                corner1: [0, 64, 0]
                corner2: [100, 128, 100]
              spawn:
                world: world
                x: 0.5
                y: 64.0
                z: 0.5
              countdown: 600
              enable_ready: false
              map_vote_setting:
                enabled: false
                map_amount: 1
              min-players: 2
            """;

    /** 落盘数据目录(JUnit 每用例自管清理)。 */
    @TempDir
    private Path dataDir;

    /** 测试装配:桩 API+真实 LobbyVote+真实解析器/配方登记簿+移账收集器(minPlayers=2,角色门默认放行)。 */
    private static final class Harness {

        final StubHydrokarbsApi stub = new StubHydrokarbsApi(null);
        final LobbyVote vote;
        final SpecificConfigParsers parsers = new SpecificConfigParsers();
        final List<UUID> removed = new ArrayList<>();
        final MatchBootstrap bootstrap;

        Harness(final GameConfigManager games) {
            this(games, () -> true);
        }

        Harness(final GameConfigManager games, final BooleanSupplier rolePresence) {
            final SessionRecipes recipes = new SessionRecipes();
            recipes.register(MODE_ID, STATIC_RECIPE);
            this.vote = new LobbyVote(games, new Random(20251118L));
            this.bootstrap = new MatchBootstrap(this.stub, this.vote, games, this.parsers, recipes, 2,
                    rolePresence, this.removed::add);
        }
    }

    /** 门三/四/五与托管路径的运行面预置:目标图在场、目标模式在场、建会话返预置句柄。 */
    private static void presetRuntimeFaces(final StubHydrokarbsApi stub, final FakeSessionHandle handle) {
        stub.setMaps(List.of(new MapInfo(MAP_ID, Set.of("shdf.bosswar"), "REUSE_DIRTY")));
        stub.putMode(MODE_ID, modeInfo());
        stub.setCreateResult(handle);
    }

    private static MatchModeInfo modeInfo() {
        return new MatchModeInfo(MODE_ID, "首领战", List.of("门测模式"), Material.MAP, Set.of(), false, 1, 100);
    }

    // ── 文本扫描冻结(注释与字符串字面量剥离后计数) ──

    @Test
    void 扫描_建会话调用_全工程恰一处且在开局单闸() throws IOException {
        assertTrue(Files.isDirectory(MAIN_SOURCE_ROOT), "扫描根须在场(测试工作目录=工程根)");
        final Map<String, Integer> counts = tokenCounts("createSession(");
        final int total = counts.values().stream().mapToInt(Integer::intValue).sum();
        assertEquals(1, total, "src/main 建会话调用恰一处(开局单闸,实际分布:" + counts + ")");
        assertEquals(1, counts.getOrDefault("com/shdfGameplay/lobby/MatchBootstrap.java", 0),
                "唯一调用点须在 lobby/MatchBootstrap");
    }

    @Test
    void 扫描_订阅会话事件_全工程恰一处且在装配器() throws IOException {
        assertTrue(Files.isDirectory(MAIN_SOURCE_ROOT), "扫描根须在场(测试工作目录=工程根)");
        final Map<String, Integer> counts = tokenCounts("subscribeSessionEvents(");
        final int total = counts.values().stream().mapToInt(Integer::intValue).sum();
        assertEquals(1, total, "src/main 订阅会话事件恰一处(散场单闸接线,实际分布:" + counts + ")");
        assertEquals(1, counts.getOrDefault("com/shdfGameplay/Assembly.java", 0), "唯一订阅接线须在 Assembly");
    }

    @Test
    void 扫描_lobby包框架import_恰白名单四类且零组件import() throws IOException {
        final Path lobbyDir = MAIN_SOURCE_ROOT.resolve("com/shdfGameplay/lobby");
        assertTrue(Files.isDirectory(lobbyDir), "lobby 包目录须在场");
        final Set<String> seen = new HashSet<>();
        try (final Stream<Path> files = Files.list(lobbyDir)) {
            for (final Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                for (final String line : stripped(Files.readString(file)).split("\\R")) {
                    final String trimmed = line.strip();
                    if (!trimmed.startsWith("import ") || !trimmed.contains("com.hydrokarbs")) {
                        continue;
                    }
                    assertFalse(trimmed.startsWith("import static "), file.getFileName() + " 禁框架 static import");
                    final String fqn = trimmed.substring("import ".length()).replace(";", "").strip();
                    assertTrue(FRAMEWORK_IMPORT_WHITELIST.contains(fqn),
                            file.getFileName() + " 框架 import 越白名单:" + fqn);
                    assertFalse(fqn.startsWith("com.hydrokarbs.component."),
                            file.getFileName() + " lobby 包零框架组件 import:" + fqn);
                    seen.add(fqn);
                }
            }
        }
        assertEquals(FRAMEWORK_IMPORT_WHITELIST, seen, "lobby 包框架 import 恰为冻结白名单集合(不多不少)");
    }

    // ── 五门行为:任一不过不开局(零建会话) ──

    @Test
    void 人数门_不足_不开局零建会话() throws IOException {
        final Harness harness = new Harness(loadedGames(true));
        final MatchBootstrap.BootstrapResult result = harness.bootstrap.bootstrap(List.of(U1));

        assertNull(result.sessionId(), "人数不足不开局");
        assertEquals(1, result.rejections().size(), "恰一条拒收原因");
        assertTrue(result.rejections().get(0).contains("人数不足"), "拒收原因携带人数口径(继续等)");
        assertTrue(harness.removed.isEmpty(), "零移账");
        assertNull(harness.stub.lastCreateRecipe(), "零建会话调用");
    }

    @Test
    void 票选门_零票且无轮换兜底_不开局零建会话() throws IOException {
        // 无标记文件=空图集:未起票计票为空、轮换兜底序为空,票选门无从开局
        final Harness harness = new Harness(loadedGames(false));
        final MatchBootstrap.BootstrapResult result = harness.bootstrap.bootstrap(List.of(U1, U2));

        assertNull(result.sessionId(), "零票且无轮换兜底不开局");
        assertEquals(1, result.rejections().size(), "恰一条拒收原因");
        assertTrue(result.rejections().get(0).contains("零票且无轮换兜底"), "拒收原因携带重新起票口径");
        assertTrue(harness.removed.isEmpty(), "全员留大厅零移账");
        assertNull(harness.stub.lastCreateRecipe(), "零建会话调用");
    }

    @Test
    void 地图门_目标图不在maps_不开局零建会话() throws IOException {
        final Harness harness = new Harness(loadedGames(true));
        harness.vote.beginRound(false, 1); // 单候选直用口径:零票且候选恰一张,计票直返
        harness.stub.setMaps(List.of()); // 地图注册面已激活但目标图缺席
        harness.stub.putMode(MODE_ID, modeInfo());
        final MatchBootstrap.BootstrapResult result = harness.bootstrap.bootstrap(List.of(U1, U2));

        assertNull(result.sessionId(), "目标图未注册不开局");
        assertTrue(result.rejections().get(0).contains("目标地图未注册"), "拒收原因携带摘候选口径");
        assertTrue(result.rejections().get(0).contains(CANDIDATE), "拒收原因携带候选行 mode@map");
        assertTrue(harness.vote.candidates().isEmpty(), "门表自愈落地:拒收即摘除失败候选并清票本(04 §4.3「摘除候选重收票」非仅文案)");
        assertNull(harness.stub.lastCreateRecipe(), "零建会话调用");
    }

    @Test
    void 模式门_缺席_不开局零建会话() throws IOException {
        final Harness harness = new Harness(loadedGames(true));
        harness.vote.beginRound(false, 1);
        harness.stub.setMaps(List.of(new MapInfo(MAP_ID, Set.of(), "REUSE_DIRTY")));
        harness.stub.putMode("other", modeInfo()); // 激活查询面但目标模式缺席(SEVERE 日志面另不在此断言)
        final MatchBootstrap.BootstrapResult result = harness.bootstrap.bootstrap(List.of(U1, U2));

        assertNull(result.sessionId(), "模式缺席不开局(装配事故)");
        assertTrue(result.rejections().get(0).contains("开局模式未注册"), "拒收原因携带装配事故口径");
        assertNull(harness.stub.lastCreateRecipe(), "零建会话调用");
    }

    @Test
    void 角色门_不在场_不开局零建会话() throws IOException {
        final Harness harness = new Harness(loadedGames(true), () -> false);
        harness.vote.beginRound(false, 1);
        harness.stub.setMaps(List.of(new MapInfo(MAP_ID, Set.of(), "REUSE_DIRTY")));
        harness.stub.putMode(MODE_ID, modeInfo());
        final MatchBootstrap.BootstrapResult result = harness.bootstrap.bootstrap(List.of(U1, U2));

        assertNull(result.sessionId(), "角色系统不在场不开局");
        assertTrue(result.rejections().get(0).contains("角色系统不在场"), "拒收原因携带摘玩法候选口径");
        assertTrue(harness.vote.candidates().isEmpty(), "门表自愈落地:拒收即摘除该玩法候选并清票本(04 §4.3「摘除该玩法候选,大厅照常」非仅文案)");
        assertNull(harness.stub.lastCreateRecipe(), "零建会话调用");
    }

    // ── 过门后托管时序 ──

    @Test
    void 配置解析失败_解析器抛IllegalStateException_拒收全员留大厅() throws IOException {
        final Harness harness = new Harness(loadedGames(true));
        harness.vote.beginRound(false, 1);
        presetRuntimeFaces(harness.stub, new FakeSessionHandle("s-boot", List.of()));
        harness.parsers.register(MODE_ID, raw -> {
            throw new IllegalStateException("boss-count 缺失");
        });
        final MatchBootstrap.BootstrapResult result = harness.bootstrap.bootstrap(List.of(U1, U2));

        assertNull(result.sessionId(), "带病配置开局前拒收:不建会话");
        assertTrue(result.rejections().get(0).contains("玩法配置解析失败"), "拒收原因携带解析失败口径");
        assertTrue(result.rejections().get(0).contains("boss-count 缺失"), "拒收原因透出解析器消息(人读可恢复)");
        assertTrue(harness.removed.isEmpty(), "全员留大厅零移账");
        assertNull(harness.stub.lastCreateRecipe(), "零建会话调用");
    }

    @Test
    void 配置解析成功_随本局配置组件入局开局() throws IOException {
        final Harness harness = new Harness(loadedGames(true));
        final AtomicBoolean parserHit = new AtomicBoolean();
        harness.parsers.register(MODE_ID, raw -> {
            parserHit.set(true);
            return new FakeModeConfig(7);
        });
        presetRuntimeFaces(harness.stub, new FakeSessionHandle("s-boot", List.of()));
        // 不起票直达轮换兜底(票选门另一分支):games.all() 稳定序首项即唯一候选
        final MatchBootstrap.BootstrapResult result = harness.bootstrap.bootstrap(List.of(U1, U2));

        assertEquals("s-boot", result.sessionId(), "解析成功照常开局");
        assertTrue(parserHit.get(), "已注册解析器被调用");
        assertTrue(result.rejections().isEmpty(), "无拒收");
    }

    @Test
    void join被拒_Rejected_该员留大厅不移账() throws IOException {
        final Harness harness = new Harness(loadedGames(true));
        harness.vote.beginRound(false, 1);
        final FakeSessionHandle handle = new FakeSessionHandle("s-boot", List.of());
        handle.putJoinResult(U2, new JoinResult.Rejected("FULL", "会话已满"));
        presetRuntimeFaces(harness.stub, handle);
        final MatchBootstrap.BootstrapResult result = harness.bootstrap.bootstrap(List.of(U1, U2));

        assertEquals("s-boot", result.sessionId(), "会话已建(任一 join 被拒不影响会话与其余成员)");
        assertEquals(1, result.rejections().size(), "恰一条 join 拒收明细");
        assertTrue(result.rejections().get(0).contains("会话已满"), "拒收明细透出 rej.detail()");
        assertTrue(result.rejections().get(0).contains(U2.toString()), "拒收明细标识被拒玩家");
        assertEquals(List.of(U1), harness.removed, "被拒者留大厅:仅放行者移账");
    }

    @Test
    void join全成功_立即移账恰一次() throws IOException {
        final Harness harness = new Harness(loadedGames(true));
        harness.vote.beginRound(false, 1);
        presetRuntimeFaces(harness.stub, new FakeSessionHandle("s-boot", List.of()));
        final MatchBootstrap.BootstrapResult result = harness.bootstrap.bootstrap(List.of(U1, U2));

        assertEquals("s-boot", result.sessionId(), "开局成功");
        assertTrue(result.rejections().isEmpty(), "无拒收");
        assertEquals(List.of(U1, U2), harness.removed, "每人恰一次移账(join 成功立即移出大厅账本)");
    }

    @Test
    void 成功路径_配方钉图与组件追加序_防双挂() throws IOException {
        final Harness harness = new Harness(loadedGames(true));
        harness.vote.beginRound(false, 1);
        presetRuntimeFaces(harness.stub, new FakeSessionHandle("s-boot", List.of()));
        harness.bootstrap.bootstrap(List.of(U1, U2));

        final SessionRecipe recipe = harness.stub.lastCreateRecipe();
        assertNotNull(recipe, "建会话恰被调用一次");
        assertEquals(MODE_ID, recipe.initialModeId(), "首模式=票选模式");
        assertEquals(Optional.of(MAP_ID), recipe.initialMapId(), "钉图=票选地图(补挂口重钉)");
        final List<SessionComponent.Specification<?>> extras = recipe.extraComponents();
        assertEquals(2, extras.size(), "静态模板+本局配置组件恰两件(防双挂)");
        assertEquals(EmptyArenaComponent.class, extras.get(0).providedType(), "空场解散组件在前(静态模板)");
        assertEquals(MatchConfigComponent.class, extras.get(1).providedType(), "本局配置组件在后(补挂追加)");
    }

    // ── 装置:离线装填 GameConfigManager(临时假 Bukkit Server)与落盘夹具 ──

    /** 玩法专用配置解析结果假件(解析成功路径用)。 */
    private record FakeModeConfig(int bossCount) {
    }

    /**
     * 落盘最小数据目录并装填:true=点名单图 bosswar/example(标记文件+图配置在场),
     * false=仅主配置(标记缺席→空图集,供零票且无轮换兜底用例)。
     */
    private GameConfigManager loadedGames(final boolean withMap) throws IOException {
        Files.writeString(this.dataDir.resolve("config.yml"), CONFIG_YML);
        if (withMap) {
            Files.createDirectories(this.dataDir.resolve("mode_config").resolve(MODE_ID));
            Files.writeString(this.dataDir.resolve("mode_config").resolve("map_modes.yaml"), """
                    map_modes:
                      - "bosswar/example"
                    """);
            Files.writeString(this.dataDir.resolve("mode_config").resolve(MODE_ID).resolve(MAP_ID + ".yaml"), """
                    map_path: "maps/bosswar/example.schem"
                    display_name: "门测固定图"
                    """);
        }
        return loadGames(new GameConfigManager(this.dataDir));
    }

    /**
     * 离线装填:GameConfigManager.init() 必经 LobbyConfig 世界在场检查(Bukkit.getWorld→Bukkit.server,
     * 离线为 null,已实证 NPE)——经反射临时挂仅答 getWorld 的假 Server(返回假 World,其余口返回类型默认值),
     * 装填毕在 finally 还原原值,不外溢其他用例;反射仅住测试面,主源集零 Bukkit 静态接触。
     */
    private static GameConfigManager loadGames(final GameConfigManager games) {
        final World world = (World) Proxy.newProxyInstance(World.class.getClassLoader(),
                new Class<?>[]{World.class},
                (proxy, method, args) -> proxyAnswer("FakeWorld", method.getName(), method.getReturnType()));
        final Server server = (Server) Proxy.newProxyInstance(Server.class.getClassLoader(),
                new Class<?>[]{Server.class}, (proxy, method, args) -> {
                    if ("getWorld".equals(method.getName())) {
                        return world;
                    }
                    return proxyAnswer("FakeServer", method.getName(), method.getReturnType());
                });
        Field serverField = null;
        Object original = null;
        try {
            serverField = Bukkit.class.getDeclaredField("server");
            serverField.setAccessible(true);
            original = serverField.get(null);
            serverField.set(null, server);
            games.init();
            return games;
        } catch (final ReflectiveOperationException e) {
            throw new IllegalStateException("离线装填失败:无法临时挂假 Bukkit Server", e);
        } finally {
            if (serverField != null) {
                try {
                    serverField.set(null, original);
                } catch (final IllegalAccessException e) {
                    throw new IllegalStateException("离线装填失败:无法还原 Bukkit Server", e);
                }
            }
        }
    }

    /** 代理口应答:Object 三件套按身份语义,其余按返回类型默认值(本组用例只依赖 getWorld)。 */
    private static Object proxyAnswer(final String name, final String methodName, final Class<?> type) {
        return switch (methodName) {
            case "toString" -> name;
            case "hashCode" -> System.identityHashCode(name);
            case "equals" -> false;
            default -> defaultValue(type);
        };
    }

    /** 原始返回类型默认值(引用类型含 void 返 null)。 */
    private static Object defaultValue(final Class<?> type) {
        if (type == boolean.class) {
            return Boolean.FALSE;
        }
        if (type == int.class) {
            return Integer.valueOf(0);
        }
        if (type == long.class) {
            return Long.valueOf(0L);
        }
        if (type == short.class) {
            return Short.valueOf((short) 0);
        }
        if (type == byte.class) {
            return Byte.valueOf((byte) 0);
        }
        if (type == char.class) {
            return Character.valueOf('\0');
        }
        if (type == float.class) {
            return Float.valueOf(0f);
        }
        if (type == double.class) {
            return Double.valueOf(0d);
        }
        return null;
    }

    // ── 扫描装置 ──

    /** 全 src/main 扫描 token 出现分布(相对路径→次数;只计剥离注释与字面量后的真实代码)。 */
    private static Map<String, Integer> tokenCounts(final String token) throws IOException {
        final Map<String, Integer> counts = new HashMap<>();
        try (final Stream<Path> files = Files.walk(MAIN_SOURCE_ROOT)) {
            files.filter(path -> path.toString().endsWith(".java")).forEach(path -> {
                final String stripped = stripped(read(path));
                int count = 0;
                int at = stripped.indexOf(token);
                while (at >= 0) {
                    count++;
                    at = stripped.indexOf(token, at + token.length());
                }
                if (count > 0) {
                    counts.put(MAIN_SOURCE_ROOT.relativize(path).toString().replace('\\', '/'), count);
                }
            });
        }
        return counts;
    }

    private static String read(final Path path) {
        try {
            return Files.readString(path);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * 剥离注释与字符串/字符字面量(含文本块):先吞字面量再判注释界符——
     * 字面量内的行注释/块注释界符不误判,注释内的引号不误判为字面量界。
     */
    private static String stripped(final String source) {
        final StringBuilder out = new StringBuilder(source.length());
        int i = 0;
        final int n = source.length();
        while (i < n) {
            final char c = source.charAt(i);
            if (c == '"') {
                i = i + 2 < n && source.charAt(i + 1) == '"' && source.charAt(i + 2) == '"'
                        ? skipTextBlock(source, i) : skipQuoted(source, i, '"');
                out.append(' ');
            } else if (c == '\'') {
                i = skipQuoted(source, i, '\'');
                out.append(' ');
            } else if (c == '/' && i + 1 < n && source.charAt(i + 1) == '/') {
                while (i < n && source.charAt(i) != '\n') {
                    i++;
                }
                out.append('\n');
            } else if (c == '/' && i + 1 < n && source.charAt(i + 1) == '*') {
                i += 2;
                while (i + 1 < n && !(source.charAt(i) == '*' && source.charAt(i + 1) == '/')) {
                    i++;
                }
                i = Math.min(i + 2, n);
                out.append(' ');
            } else {
                out.append(c);
                i++;
            }
        }
        return out.toString();
    }

    /** 吞普通字符串/字符字面量(处理反斜杠转义),返回界符后下标。 */
    private static int skipQuoted(final String source, final int start, final char quote) {
        int i = start + 1;
        final int n = source.length();
        while (i < n && source.charAt(i) != quote) {
            if (source.charAt(i) == '\\' && i + 1 < n) {
                i++;
            }
            i++;
        }
        return Math.min(i + 1, n);
    }

    /** 吞文本块(三引号开闭),返回闭合三引号后下标。 */
    private static int skipTextBlock(final String source, final int start) {
        int i = start + 3;
        final int n = source.length();
        while (i + 2 < n && !(source.charAt(i) == '"' && source.charAt(i + 1) == '"' && source.charAt(i + 2) == '"')) {
            i++;
        }
        return Math.min(i + 3, n);
    }
}
