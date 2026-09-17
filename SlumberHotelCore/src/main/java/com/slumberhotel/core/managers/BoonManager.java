package com.slumberhotel.core.managers;

import com.slumberhotel.core.SlumberHotelCore;
import org.bukkit.entity.Player;

/**
 * Permanent ticket multipliers (Hypixel-style "boons").
 * Stored as double in player YAML; applied in TicketManager.
 */
public class BoonManager {

    private final SlumberHotelCore plugin;

    public BoonManager(SlumberHotelCore plugin) {
        this.plugin = plugin;
    }

    public double getMultiplier(Player player) {
        double base = plugin.getConfig().getDouble("tickets.global-multiplier", 1.0);
        double personal = plugin.getPlayerDataManager().getTicketMultiplier(player.getUniqueId());
        if (personal < 1.0) personal = 1.0;
        return base * personal;
    }

    public void addMultiplier(Player player, double amount, String reason) {
        double now = plugin.getPlayerDataManager().getTicketMultiplier(player.getUniqueId());
        if (now < 1.0) now = 1.0;
        now += amount;
        plugin.getPlayerDataManager().setTicketMultiplier(player.getUniqueId(), now);
        player.sendMessage(plugin.getConfigManager().color(
                "&dBoon unlocked: &e+" + (int) (amount * 100) + "% &dticket gain &8(" + reason + ")"));
        player.sendMessage(plugin.getConfigManager().color(
                "&7Total ticket multiplier: &e" + String.format("%.2f", now) + "x"));
    }
}
