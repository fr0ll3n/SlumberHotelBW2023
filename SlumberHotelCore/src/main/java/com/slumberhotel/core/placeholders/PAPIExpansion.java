package com.slumberhotel.core.placeholders;

import com.slumberhotel.core.SlumberHotelCore;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * %slumber_*% placeholders – tickets, wallet, needed amounts (Hypixel-style).
 */
public class PAPIExpansion extends PlaceholderExpansion {

    private final SlumberHotelCore plugin;

    public PAPIExpansion(SlumberHotelCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "slumber";
    }

    @Override
    public String getAuthor() {
        return "SlumberHotel";
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer offline, String params) {
        if (params == null) return null;
        String id = params.toLowerCase();

        // Non-player safe
        if (id.equals("name")) return plugin.getConfigManager().getTicketName();
        if (id.equals("name_plural")) return plugin.getConfigManager().getTicketNamePlural();
        if (id.equals("entry_cost") || id.equals("needed_entry_total")) {
            return String.valueOf(plugin.getConfig().getInt("costs.hotel-entry", 25));
        }

        if (offline == null || !offline.isOnline()) {
            // Still try offline for some values if PlayerPoints supports UUID
            return null;
        }
        Player player = offline.getPlayer();
        if (player == null) return null;

        int tickets = plugin.getTicketManager().getTickets(player);
        int capacity = plugin.getWalletManager().getCurrentCapacity(player);
        int tier = plugin.getWalletManager().getCurrentTier(player);

        // ---- balances ----
        if (id.equals("tickets") || id.equals("points") || id.equals("balance")) {
            return String.valueOf(tickets);
        }
        if (id.equals("stars") || id.equals("hotel_stars")) {
            return String.valueOf(plugin.getStarsManager().getStars(player));
        }
        if (id.equals("multiplier") || id.equals("boon") || id.equals("ticket_multiplier")) {
            return String.format("%.2f", plugin.getBoonManager().getMultiplier(player));
        }
        if (id.equals("machine_cost")) {
            return String.valueOf(plugin.getTicketMachineManager().getCost());
        }
        if (id.equals("games_played") || id.equals("games")) {
            return String.valueOf(plugin.getPlayerDataManager().getGamesPlayed(player.getUniqueId()));
        }
        if (id.equals("quests_active")) {
            return String.valueOf(plugin.getQuestManager().getActive(player).size());
        }
        if (id.equals("quests_completed")) {
            return String.valueOf(plugin.getQuestManager().getCompleted(player).size());
        }
        if (id.equals("wallet") || id.equals("capacity") || id.equals("wallet_capacity")) {
            return String.valueOf(capacity);
        }
        if (id.equals("wallet_tier") || id.equals("tier")) {
            return String.valueOf(tier);
        }
        if (id.equals("tickets_formatted") || id.equals("formatted")) {
            return tickets + "/" + capacity;
        }
        if (id.equals("tickets_space") || id.equals("space")) {
            return String.valueOf(Math.max(0, capacity - tickets));
        }

        // ---- hotel entry ----
        int entryCost = plugin.getConfig().getInt("costs.hotel-entry", 25);
        if (id.equals("needed_entry") || id.equals("remaining_entry")) {
            return String.valueOf(Math.max(0, entryCost - tickets));
        }
        if (id.equals("can_enter")) {
            return tickets >= entryCost ? "true" : "false";
        }
        if (id.equals("has_entered") || id.equals("entered")) {
            return plugin.getPlayerDataManager().hasEnteredHotel(player) ? "true" : "false";
        }
        if (id.equals("entry_progress")) {
            return Math.min(tickets, entryCost) + "/" + entryCost;
        }

        // ---- next wallet upgrade ----
        int nextTierCost = getNextWalletCost(tier);
        if (id.equals("needed_wallet") || id.equals("remaining_wallet") || id.equals("needed_upgrade")) {
            if (nextTierCost < 0) return "0"; // max tier
            return String.valueOf(Math.max(0, nextTierCost - tickets));
        }
        if (id.equals("wallet_upgrade_cost") || id.equals("next_wallet_cost")) {
            return nextTierCost < 0 ? "0" : String.valueOf(nextTierCost);
        }
        if (id.equals("can_upgrade_wallet")) {
            return nextTierCost >= 0 && tickets >= nextTierCost ? "true" : "false";
        }
        if (id.equals("next_wallet_capacity")) {
            List<Integer> tiers = plugin.getConfig().getIntegerList("wallet.tiers");
            if (tiers == null || tier + 1 >= tiers.size()) return String.valueOf(capacity);
            return String.valueOf(tiers.get(tier + 1));
        }
        if (id.equals("wallet_upgrade_progress")) {
            if (nextTierCost < 0) return "MAX";
            return Math.min(tickets, nextTierCost) + "/" + nextTierCost;
        }

        // ---- doors: needed_door_<key>  e.g. needed_door_room-1 ----
        if (id.startsWith("needed_door_") || id.startsWith("remaining_door_")) {
            String key = id.substring(id.indexOf("door_") + 5);
            int cost = plugin.getConfig().getInt("costs.doors." + key, 0);
            return String.valueOf(Math.max(0, cost - tickets));
        }
        if (id.startsWith("door_cost_")) {
            String key = id.substring("door_cost_".length());
            return String.valueOf(plugin.getConfig().getInt("costs.doors." + key, 0));
        }
        if (id.startsWith("can_unlock_")) {
            String key = id.substring("can_unlock_".length());
            int cost = plugin.getConfig().getInt("costs.doors." + key, 0);
            return tickets >= cost ? "true" : "false";
        }
        if (id.startsWith("door_progress_")) {
            String key = id.substring("door_progress_".length());
            int cost = plugin.getConfig().getInt("costs.doors." + key, 0);
            return Math.min(tickets, cost) + "/" + cost;
        }

        // ---- daily quests ----
        if (id.equals("daily_count")) {
            return String.valueOf(plugin.getDailyQuestManager().getAssigned(player).size());
        }
        if (id.startsWith("daily_progress_")) {
            String qid = id.substring("daily_progress_".length());
            int prog = plugin.getDailyQuestManager().getProgress(player, qid);
            int target = plugin.getDailyQuestManager().getQuestTarget(qid);
            return prog + "/" + target;
        }
        if (id.startsWith("daily_done_")) {
            String qid = id.substring("daily_done_".length());
            return plugin.getDailyQuestManager().isCompleted(player, qid) ? "true" : "false";
        }
        if (id.startsWith("daily_name_")) {
            String qid = id.substring("daily_name_".length());
            return strip(plugin.getDailyQuestManager().getQuestName(qid));
        }

        // ---- generic cost lookup: needed_<path> with dots as _ ----
        // e.g. needed_doors_vip-suite already handled; keep flexible
        return null;
    }

    private int getNextWalletCost(int currentTier) {
        List<Integer> costs = plugin.getConfig().getIntegerList("costs.wallet-upgrades");
        if (costs == null || costs.isEmpty()) return -1;
        int next = currentTier + 1;
        if (next >= costs.size()) return -1;
        return costs.get(next);
    }

    private String strip(String s) {
        return s == null ? "" : s.replaceAll("&[0-9a-fk-or]", "");
    }
}
