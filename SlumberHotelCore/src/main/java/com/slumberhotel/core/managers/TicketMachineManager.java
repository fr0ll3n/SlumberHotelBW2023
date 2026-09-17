package com.slumberhotel.core.managers;

import com.slumberhotel.core.SlumberHotelCore;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Ticket Machine – spend tickets for random rewards (cosmetics cmd / tickets / items).
 */
public class TicketMachineManager {

    private final SlumberHotelCore plugin;
    private final Random random = new Random();

    public TicketMachineManager(SlumberHotelCore plugin) {
        this.plugin = plugin;
    }

    public boolean isEnabled() {
        return plugin.getConfig().getBoolean("ticket-machine.enabled", true);
    }

    public int getCost() {
        return plugin.getConfig().getInt("ticket-machine.cost", 10);
    }

    /**
     * Roll the machine once. Returns true if rolled.
     */
    public boolean roll(Player player) {
        if (!isEnabled()) {
            player.sendMessage(plugin.getConfigManager().color("&cTicket Machine is disabled."));
            return false;
        }
        int cost = getCost();
        if (plugin.getTicketManager().getTickets(player) < cost) {
            player.sendMessage(plugin.getConfigManager().color(
                    "&cNeed &e" + cost + " &cSlumber Tickets to use the machine."));
            return false;
        }
        plugin.getTicketManager().takeTickets(player, cost);

        ConfigurationSection rewards = plugin.getConfig().getConfigurationSection("ticket-machine.rewards");
        if (rewards == null || rewards.getKeys(false).isEmpty()) {
            player.sendMessage(plugin.getConfigManager().color("&7Nothing happened... (no rewards configured)"));
            return true;
        }

        // Weighted pick
        List<String> pool = new ArrayList<String>();
        List<Integer> weights = new ArrayList<Integer>();
        int total = 0;
        for (String key : rewards.getKeys(false)) {
            int w = rewards.getInt(key + ".weight", 1);
            pool.add(key);
            weights.add(w);
            total += w;
        }
        int roll = random.nextInt(Math.max(1, total));
        int cursor = 0;
        String chosen = pool.get(0);
        for (int i = 0; i < pool.size(); i++) {
            cursor += weights.get(i);
            if (roll < cursor) {
                chosen = pool.get(i);
                break;
            }
        }

        ConfigurationSection r = rewards.getConfigurationSection(chosen);
        if (r == null) return true;

        String name = r.getString("name", chosen);
        player.sendMessage(plugin.getConfigManager().color("&d✦ Ticket Machine &7→ &e" + name));

        int giveTickets = r.getInt("tickets", 0);
        if (giveTickets > 0) {
            plugin.getTicketManager().giveTickets(player, giveTickets, "Ticket Machine");
        }
        String item = r.getString("item", "");
        int itemAmt = r.getInt("item-amount", 1);
        if (item != null && !item.isEmpty()) {
            plugin.getSpecialItemManager().giveItem(player, item, itemAmt);
        }
        for (String cmd : r.getStringList("commands")) {
            org.bukkit.Bukkit.dispatchCommand(
                    org.bukkit.Bukkit.getConsoleSender(),
                    cmd.replace("%player%", player.getName()));
        }
        int stars = r.getInt("stars", 0);
        if (stars > 0 && plugin.getStarsManager() != null) {
            plugin.getStarsManager().addStars(player, stars, "Ticket Machine");
        }
        return true;
    }
}
