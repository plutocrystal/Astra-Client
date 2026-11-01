package dev.astra;

import dev.astra.command.CommandManager;
import dev.astra.command.impl.BindCommand;
import dev.astra.command.impl.FriendCommand;
import dev.astra.command.impl.ConfigCommand;
import dev.astra.command.impl.HelpCommand;
import dev.astra.command.impl.ReloadCommand;
import dev.astra.command.impl.SetValueCommand;
import dev.astra.command.impl.ToggleCommand;
import dev.astra.config.ConfigManager;
import dev.astra.event.EventBus;
import dev.astra.module.ModuleManager;
import de.florianmichael.viamcp.ViaMCP;
import de.florianmichael.vialoadingbase.ViaLoadingBase;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import org.lwjgl.opengl.Display;

public class Main {
    public static final String NAME = "Astra";

    public static final String VERSION = Version.VERSION;

    public static EventBus eventBus;

    public static ModuleManager moduleManager;

    public static CommandManager commandManager;

    public static void start() {
        Display.setTitle(NAME + VERSION);
        eventBus = new EventBus();
        moduleManager = new ModuleManager();
        commandManager = new CommandManager();
        dev.astra.util.inventory.LBInventoryManager.init();
        eventBus.register(moduleManager);
        commandManager.registerCommand(new ToggleCommand());
        commandManager.registerCommand(new FriendCommand());
        commandManager.registerCommand(new BindCommand());
        commandManager.registerCommand(new HelpCommand());
        commandManager.registerCommand(new SetValueCommand());
        commandManager.registerCommand(new ConfigCommand());
        commandManager.registerCommand(new ReloadCommand());
        dev.astra.gui.tabs.AstraTabs.init();
        ConfigManager.init();
        ConfigManager.loadConfig("default");
        ViaMCP.create();
        ViaMCP.INSTANCE.initAsyncSlider();
        ViaLoadingBase.getInstance().reload(ProtocolVersion.v1_8);
        new Thread(() -> {
            try {
                Thread.sleep(1000);
            } catch (Throwable ignored) {}
            try {
                ViaMCP.INSTANCE.applyFix();
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }).start();
    }

    public static void stop() {
        System.out.println("Stop!!!");
        ConfigManager.saveConfig("default");
    }
}
