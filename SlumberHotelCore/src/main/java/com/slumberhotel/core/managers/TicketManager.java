package com.slumberhotel.core.managers;

import com.slumberhotel.core.SlumberHotelCore;
import com.slumberhotel.core.hooks.PlayerPointsHook;
import org.bukkit.entity.Player;

public class TicketManager {

    private final SlumberHotelCore plugin;

    public TicketManager(SlumberHotelCore plugin) {
        this.plugin = plugin;
    }

    public int getTickets(Player player) {
        PlayerPointsHook hook = plugin.getPlayerPointsHook();
        if (hook == null || !hook.isEnabled()) return 0;
        return hook.getPoints(player);
    }

    /**
     * Give tickets respecting wallet capacity.
     * Returns the actual amount given (may be less if wallet is almost full).
     */
    public int giveTickets(Player player, int amount, String reason) {
        if (amount <= 0) return 0;

        PlayerPointsHook hook = plugin.getPlayerPointsHook();
        if (hook == null || !hook.isEnabled()) {
            plugin.getLogger().warning("Tried to give tickets but PlayerPoints is not available!");
            return 0;
        }

        int current = getTickets(player);
        int capacity = plugin.getWalletManager().getCurrentCapacity(player);
        int space = capacity - current;

        if (space <= 0) {
            player.sendMessage(plugin.getConfigManager().getMessage("wallet-full")
                    .replace("{current}", String.valueOf(current))
                    .replace("{max}", String.valueOf(capacity)));
            return 0;
        }

        int toGive = Math.min(amount, space);
        boolean success = hook.givePoints(player, toGive);

        if (success) {
            String name = toGive == 1
                    ? plugin.getConfigManager().getTicketName()
                    : plugin.getConfigManager().getTicketNamePlural();

            player.sendMessage(plugin.getConfigManager().getMessage("tickets-gained")
                    .replace("{amount}", String.valueOf(toGive))
                    .replace("{name}", name)
                    .replace("{reason}", reason != null ? reason : "Game"));
        }

        return success ? toGive : 0;
    }

    public boolean takeTickets(Player player, int amount) {
        PlayerPointsHook hook = plugin.getPlayerPointsHook();
        if (hook == null || !hook.isEnabled()) return false;
        return hook.takePoints(player, amount);
    }

    public boolean hasTickets(Player player, int amount) {
        return getTickets(player) >= amount;
    }

    /**
     * Award tickets from a BedWars action (kill, bed, win...).
     * Applies global multiplier.
     */
    public void awardFromAction(Player player, String actionKey, String reason) {
        int base = plugin.getConfigManager().getTicketGain(actionKey);
        if (base <= 0) return;

        double multi = plugin.getConfigManager().getGlobalMultiplier();
        if (plugin.getBoonManager() != null) {
            multi = plugin.getBoonManager().getMultiplier(player);
        }
        int finalAmount = (int) Math.round(base * multi);
        if (finalAmount < 1) finalAmount = 1;

        giveTickets(player, finalAmount, reason);
        try {
            String sound = plugin.getConfig().getString("sounds.ticket-earn", "ORB_PICKUP");
            player.playSound(player.getLocation(), org.bukkit.Sound.valueOf(sound), 0.7f, 1.2f);
        } catch (Exception ignored) {}
    }
}
