package me.ddggdd135.stackmachine.integrations;

import org.bukkit.Bukkit;

public class SlimeCustomizerIntegration implements Integration {
    private boolean cache = false;
    private boolean isCached = false;

    @Override
    public boolean isLoaded() {
        if (!isCached) {
            cache = Bukkit.getPluginManager().isPluginEnabled("SlimeCustomizer");
            isCached = true;
        }
        return cache;
    }
}
