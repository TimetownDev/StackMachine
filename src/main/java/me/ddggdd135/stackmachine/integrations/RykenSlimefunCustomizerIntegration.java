package me.ddggdd135.stackmachine.integrations;

import org.bukkit.Bukkit;

public class RykenSlimefunCustomizerIntegration implements Integration {
    private boolean cache = false;
    private boolean isCached = false;

    @Override
    public boolean isLoaded() {
        if (!isCached) {
            cache = Bukkit.getPluginManager().isPluginEnabled("RykenSlimefunCustomizer");
            isCached = true;
        }
        return cache;
    }
}
