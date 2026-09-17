package com.slumberhotel.core.managers;

import com.slumberhotel.core.SlumberHotelCore;
import org.bukkit.entity.Player;

/**
 * Hotel Stars – Hypixel-style progression currency (not the same as tickets).
 * Earned mainly from completing story quests; some gates require stars.
 */
public class StarsManager {

    private final SlumberHotelCore plugin;

    public StarsManager(SlumberHotelCore plugin) {
        this.plugin = plugin;
    }

    public int getStars(Player player) {
        return plugin.getPlayerDataManager().getStars(player.getUniqueId());
    }

    public void addStars(Player player, int amount, String reason) {
        if (amount <= 0) return;
        int now = getStars(player) + amount;
        plugin.getPlayerDataManager().setStars(player.getUniqueId(), now);
        player.sendMessage(plugin.getConfigManager().color(
                "&6+" + amount + " Hotel Star" + (amount == 1 ? "" : "s") + " &8(" + reason + ") &7[Total: &e" + now + "&7]"));
        if (plugin.getStarMilestoneListener() != null) {
            plugin.getStarMilestoneListener().check(player, now);
        }
    }

    public boolean hasStars(Player player, int amount) {
        return getStars(player) >= amount;
    }
}
