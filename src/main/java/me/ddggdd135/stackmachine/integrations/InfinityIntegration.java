package me.ddggdd135.stackmachine.integrations;

import org.bukkit.Bukkit;

public class InfinityIntegration implements Integration {
    private boolean cache = false;
    private boolean isCached = false;

    @Override
    public boolean isLoaded() {
        if (!isCached) {
            cache = Bukkit.getPluginManager().isPluginEnabled("InfinityExpansion");
            isCached = true;
        }
        return cache;
    }
}
