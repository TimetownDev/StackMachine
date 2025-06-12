package me.ddggdd135.stackmachine.integrations;

import org.bukkit.Bukkit;

public class SlimeAEIntegration implements Integration {
    private boolean cache = false;
    private boolean isCached = false;

    @Override
    public boolean isLoaded() {
        if (!isCached) {
            cache = Bukkit.getPluginManager().isPluginEnabled("SlimeAEPlugin");
            isCached = true;
        }
        return cache;
    }
}
