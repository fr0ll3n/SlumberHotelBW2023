package com.slumberhotel.core.hooks;

import com.slumberhotel.core.SlumberHotelCore;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Soft dependency on PlayerPoints 3.x (Rosewood) – uses getInstance().getAPI()
 */
public class PlayerPointsHook {

    private final SlumberHotelCore plugin;
    private Object apiInstance;
    private boolean enabled = false;

    public PlayerPointsHook(SlumberHotelCore plugin) {
        this.plugin = plugin;
    }

    public boolean setup() {
        Plugin pp = Bukkit.getPluginManager().getPlugin("PlayerPoints");
        if (pp == null || !pp.isEnabled()) return false;
        try {
            Class<?> ppClass = Class.forName("org.black_ixx.playerpoints.PlayerPoints");
            Method getInstance = ppClass.getMethod("getInstance");
            Object instance = getInstance.invoke(null);
            Method getAPI = instance.getClass().getMethod("getAPI");
            apiInstance = getAPI.invoke(instance);
            enabled = true;
            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("PlayerPoints API hook failed: " + e.getMessage());
            return false;
        }
    }

    public boolean isEnabled() {
        return enabled && apiInstance != null;
    }

    public int getPoints(Player player) {
        if (!isEnabled()) return 0;
        try {
            Method look = apiInstance.getClass().getMethod("look", UUID.class);
            Object result = look.invoke(apiInstance, player.getUniqueId());
            return result instanceof Integer ? (Integer) result : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    public boolean givePoints(Player player, int amount) {
        if (!isEnabled() || amount <= 0) return false;
        try {
            Method give = apiInstance.getClass().getMethod("give", UUID.class, int.class);
            Object result = give.invoke(apiInstance, player.getUniqueId(), amount);
            return !(result instanceof Boolean) || (Boolean) result;
        } catch (Exception e) {
            plugin.getLogger().warning("give points failed: " + e.getMessage());
            return false;
        }
    }

    public boolean takePoints(Player player, int amount) {
        if (!isEnabled() || amount <= 0) return false;
        try {
            Method take = apiInstance.getClass().getMethod("take", UUID.class, int.class);
            Object result = take.invoke(apiInstance, player.getUniqueId(), amount);
            return result instanceof Boolean && (Boolean) result;
        } catch (Exception e) {
            return false;
        }
    }
}
