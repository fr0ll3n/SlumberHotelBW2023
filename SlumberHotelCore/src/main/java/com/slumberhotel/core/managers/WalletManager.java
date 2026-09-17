package com.slumberhotel.core.managers;

import com.slumberhotel.core.SlumberHotelCore;
import org.bukkit.entity.Player;

import java.util.List;

public class WalletManager {

    private final SlumberHotelCore plugin;

    public WalletManager(SlumberHotelCore plugin) {
        this.plugin = plugin;
    }

    public int getCurrentCapacity(Player player) {
        int tier = plugin.getPlayerDataManager().getWalletTier(player);
        List<Integer> tiers = plugin.getConfigManager().getWalletTiers();
        if (tiers == null || tiers.isEmpty()) {
            return plugin.getConfigManager().getStartingWallet();
        }
        if (tier < 0) tier = 0;
        if (tier >= tiers.size()) tier = tiers.size() - 1;
        return tiers.get(tier);
    }

    public int getCurrentTier(Player player) {
        return plugin.getPlayerDataManager().getWalletTier(player);
    }

    /**
     * Upgrade wallet to next tier. Returns true if upgraded.
     */
    public boolean upgradeWallet(Player player) {
        int current = getCurrentTier(player);
        List<Integer> tiers = plugin.getConfigManager().getWalletTiers();
        if (current + 1 >= tiers.size()) {
            return false; // already max
        }
        plugin.getPlayerDataManager().setWalletTier(player, current + 1);
        int newCap = getCurrentCapacity(player);
        player.sendMessage(plugin.getConfigManager().getMessage("wallet-upgraded")
                .replace("{max}", String.valueOf(newCap)));
        return true;
    }

    /**
     * Set specific tier (admin / quest reward)
     */
    public void setTier(Player player, int tier) {
        List<Integer> tiers = plugin.getConfigManager().getWalletTiers();
        if (tier < 0) tier = 0;
        if (tier >= tiers.size()) tier = tiers.size() - 1;
        plugin.getPlayerDataManager().setWalletTier(player, tier);
    }

    public boolean canHoldMore(Player player, int amountToAdd) {
        int current = plugin.getTicketManager().getTickets(player);
        int capacity = getCurrentCapacity(player);
        return (current + amountToAdd) <= capacity;
    }
}
