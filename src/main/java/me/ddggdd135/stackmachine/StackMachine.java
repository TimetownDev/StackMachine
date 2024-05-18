package me.ddggdd135.stackmachine;

import io.github.thebusybiscuit.slimefun4.api.SlimefunAddon;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import me.ddggdd135.stackmachine.core.StackMachineItems;
import org.bukkit.plugin.java.JavaPlugin;

public final class StackMachine extends JavaPlugin implements SlimefunAddon {

    private static StackMachine Instance;

    @Override
    public void onEnable() {
        // Plugin startup logic
        Instance = this;
        StackMachineItems.onSetup(this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    @Nonnull
    @Override
    public JavaPlugin getJavaPlugin() {
        return Instance;
    }

    public static StackMachine getInstance() {
        return Instance;
    }

    @Nullable @Override
    public String getBugTrackerURL() {
        return null;
    }
}