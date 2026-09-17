package com.slumberhotel.core.placeholders;

import com.slumberhotel.core.SlumberHotelCore;
import org.bukkit.Bukkit;

/**
 * Registers the real PlaceholderAPI expansion when PAPI is present.
 */
public class SlumberExpansion {

    private final SlumberHotelCore plugin;

    public SlumberExpansion(SlumberHotelCore plugin) {
        this.plugin = plugin;
    }

    public void register() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
            plugin.getLogger().info("PlaceholderAPI not found – placeholders disabled.");
            return;
        }
        try {
            PAPIExpansion expansion = new PAPIExpansion(plugin);
            if (expansion.register()) {
                plugin.getLogger().info("PlaceholderAPI expansion registered: %slumber_*%");
            } else {
                plugin.getLogger().warning("PlaceholderAPI expansion failed to register.");
            }
        } catch (NoClassDefFoundError e) {
            plugin.getLogger().warning("PlaceholderAPI classes missing: " + e.getMessage());
        } catch (Exception e) {
            plugin.getLogger().warning("PAPI register error: " + e.getMessage());
        }
    }
}
