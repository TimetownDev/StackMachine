package me.ddggdd135.stackmachine;

import io.github.thebusybiscuit.slimefun4.api.SlimefunAddon;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import me.ddggdd135.stackmachine.core.StackMachineItems;
import me.ddggdd135.stackmachine.integrations.InfinityIntegration;
import me.ddggdd135.stackmachine.integrations.RykenSlimefunCustomizerIntegration;
import me.ddggdd135.stackmachine.integrations.SlimeAEIntegration;
import me.ddggdd135.stackmachine.integrations.SlimeCustomizerIntegration;
import org.bukkit.plugin.java.JavaPlugin;

public final class StackMachine extends JavaPlugin implements SlimefunAddon {

    private static StackMachine Instance;
    private InfinityIntegration infinityIntegration;
    private SlimeCustomizerIntegration slimeCustomizerIntegration;
    private RykenSlimefunCustomizerIntegration rykenSlimefunCustomizerIntegration;
    private SlimeAEIntegration slimeAEIntegration;

    @Override
    public void onEnable() {
        // Plugin startup logic
        Instance = this;

        infinityIntegration = new InfinityIntegration();
        slimeCustomizerIntegration = new SlimeCustomizerIntegration();
        rykenSlimefunCustomizerIntegration = new RykenSlimefunCustomizerIntegration();
        slimeAEIntegration = new SlimeAEIntegration();

        if (infinityIntegration.isLoaded()) getLogger().info("InfinityExpansion已接入");
        if (slimeCustomizerIntegration.isLoaded()) getLogger().info("SlimeCustomizer已接入");
        if (rykenSlimefunCustomizerIntegration.isLoaded()) getLogger().info("RykenSlimeCustomizer已接入");
        if (slimeAEIntegration.isLoaded()) getLogger().info("SlimeAE已接入");

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

    @Nonnull
    public static InfinityIntegration getInfinityIntegration() {
        return getInstance().infinityIntegration;
    }

    @Nonnull
    public static SlimeCustomizerIntegration getSlimeCustomizerIntegration() {
        return getInstance().slimeCustomizerIntegration;
    }

    @Nonnull
    public static RykenSlimefunCustomizerIntegration getRykenSlimefunCustomizerIntegration() {
        return getInstance().rykenSlimefunCustomizerIntegration;
    }

    @Nonnull
    public static SlimeAEIntegration getSlimeAEIntegration() {
        return getInstance().slimeAEIntegration;
    }
}
