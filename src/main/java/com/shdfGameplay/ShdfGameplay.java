package com.shdfGameplay;

import java.nio.file.Path;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * 宿主独立插件主类:生命周期壳——onEnable 构造 {@link Assembly}(全工程唯一 L0 装配点,
 * 装配序与 TODO 全部住彼)并委托 assemble;onDisable 委托 disable 后弃引用。
 */
public final class ShdfGameplay extends JavaPlugin {

    /** 装配器(onEnable 构造;onDisable 收尾后置 null,重载重启经新装配器全新装配)。 */
    private Assembly assembly;

    @Override
    public void onEnable() {
        this.assembly = new Assembly(this);
        this.assembly.assemble();
    }

    @Override
    public void onDisable() {
        if (this.assembly != null) {
            this.assembly.disable();
            this.assembly = null;
        }
    }

    /** 本插件 jar 路径(包私有资源口:getFile 为 protected,经主类透出给同包 Assembly 供首启资源释放)。 */
    Path jarFile() {
        return getFile().toPath();
    }
}
