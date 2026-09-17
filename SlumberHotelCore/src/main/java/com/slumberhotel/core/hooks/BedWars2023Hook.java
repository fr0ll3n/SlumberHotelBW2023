package com.slumberhotel.core.hooks;

import com.slumberhotel.core.SlumberHotelCore;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

/**
 * Soft hook into BedWars2023 5.0.0 events (exact class names from the jar).
 */
public class BedWars2023Hook implements Listener {

    private final SlumberHotelCore plugin;
    private boolean hooked = false;
    private final java.util.Map<java.util.UUID, Long> joinTimes = new java.util.concurrent.ConcurrentHashMap<java.util.UUID, Long>();

    public BedWars2023Hook(SlumberHotelCore plugin) {
        this.plugin = plugin;
    }

    public boolean hook() {
        Plugin bw = Bukkit.getPluginManager().getPlugin("BedWars2023");
        if (bw == null) {
            // also try alternate plugin names
            bw = Bukkit.getPluginManager().getPlugin("BedWars");
        }
        if (bw == null) return false;

        register("com.tomkeuper.bedwars.api.events.player.PlayerKillEvent", "onKill");
        register("com.tomkeuper.bedwars.api.events.player.PlayerBedBreakEvent", "onBed");
        register("com.tomkeuper.bedwars.api.events.gameplay.GameEndEvent", "onEnd");
        register("com.tomkeuper.bedwars.api.events.player.PlayerGeneratorCollectEvent", "onGen");
        register("com.tomkeuper.bedwars.api.events.shop.ShopBuyEvent", "onShop");
        register("com.tomkeuper.bedwars.api.events.player.PlayerLeaveArenaEvent", "onLeave");
        register("com.tomkeuper.bedwars.api.events.player.PlayerJoinArenaEvent", "onJoinArena");

        hooked = true;
        plugin.getLogger().info("BedWars2023 event hooks registered.");
        return true;
    }

    private void register(final String className, final String handler) {
        try {
            @SuppressWarnings("unchecked")
            Class<? extends Event> clazz = (Class<? extends Event>) Class.forName(className);
            EventExecutor exec = new EventExecutor() {
                @Override
                public void execute(Listener listener, Event event) {
                    try {
                        Method m = BedWars2023Hook.this.getClass().getDeclaredMethod(handler, Event.class);
                        m.setAccessible(true);
                        m.invoke(BedWars2023Hook.this, event);
                    } catch (NoSuchMethodException ignored) {
                    } catch (Exception ex) {
                        if (plugin.getConfigManager().isDebug()) ex.printStackTrace();
                    }
                }
            };
            Bukkit.getPluginManager().registerEvent(clazz, this, EventPriority.MONITOR, exec, plugin, true);
            plugin.getLogger().info("  hooked " + className);
        } catch (ClassNotFoundException e) {
            if (plugin.getConfigManager().isDebug()) {
                plugin.getLogger().info("Event not found: " + className);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed " + className + ": " + e.getMessage());
        }
    }

    @SuppressWarnings("unused")
    private void onKill(Event event) {
        try {
            Method getKiller = event.getClass().getMethod("getKiller");
            Object k = getKiller.invoke(event);
            if (!(k instanceof Player)) return;
            Player killer = (Player) k;

            boolean finalKill = false;
            try {
                Method getCause = event.getClass().getMethod("getCause");
                Object cause = getCause.invoke(event);
                if (cause != null) {
                    Method isFinal = cause.getClass().getMethod("isFinalKill");
                    Object r = isFinal.invoke(cause);
                    if (r instanceof Boolean) finalKill = (Boolean) r;
                }
            } catch (Exception ignored) {}

            if (finalKill) {
                plugin.getTicketManager().awardFromAction(killer, "final-kill", "Final Kill");
                plugin.getDailyQuestManager().addProgress(killer, "FINAL_KILLS", 1);
            } else {
                plugin.getTicketManager().awardFromAction(killer, "kill", "Kill");
                plugin.getDailyQuestManager().addProgress(killer, "KILLS", 1);
            }
            // Token of Ferocity (King Flut quests)
            double ferChance = plugin.getConfig().getDouble(
                    finalKill ? "special-items.token-of-ferocity.final-kill-chance"
                              : "special-items.token-of-ferocity.kill-chance",
                    finalKill ? 0.35 : 0.20);
            if (Math.random() <= ferChance) {
                plugin.getSpecialItemManager().giveItem(killer, "token-of-ferocity", 1);
            }
            if (finalKill && Math.random() <= 0.25) {
                plugin.getSpecialItemManager().giveItem(killer, "void-husk", 1);
            }
        } catch (Exception e) {
            if (plugin.getConfigManager().isDebug()) e.printStackTrace();
        }
    }

    @SuppressWarnings("unused")
    private void onBed(Event event) {
        try {
            Method getPlayer = event.getClass().getMethod("getPlayer");
            Object p = getPlayer.invoke(event);
            if (!(p instanceof Player)) return;
            Player player = (Player) p;

            plugin.getTicketManager().awardFromAction(player, "bed-destroy", "Bed Destroyed");
            plugin.getDailyQuestManager().addProgress(player, "BEDS", 1);

            double chance = plugin.getConfig().getDouble("special-items.bed-sheet.drop-chance-on-bed", 1.0);
            if (Math.random() <= chance) {
                plugin.getSpecialItemManager().giveItem(player, "bed-sheet", 1);
                player.sendMessage(plugin.getConfigManager().color("&aYou found a &fBed Sheet&a!"));
            }
        } catch (Exception e) {
            if (plugin.getConfigManager().isDebug()) e.printStackTrace();
        }
    }

    @SuppressWarnings("unused")
    private void onEnd(Event event) {
        try {
            Method getWinners = event.getClass().getMethod("getWinners");
            Object winners = getWinners.invoke(event);
            if (winners instanceof List) {
                for (Object o : (List<?>) winners) {
                    UUID uuid = null;
                    if (o instanceof UUID) uuid = (UUID) o;
                    else if (o instanceof Player) uuid = ((Player) o).getUniqueId();
                    if (uuid == null) continue;
                    Player pl = Bukkit.getPlayer(uuid);
                    if (pl != null && pl.isOnline()) {
                        plugin.getTicketManager().awardFromAction(pl, "win", "Victory");
                        plugin.getDailyQuestManager().addProgress(pl, "WINS", 1);
                        plugin.getDailyQuestManager().addProgress(pl, "GAMES", 1);
                        if (Math.random() <= plugin.getConfig().getDouble("special-items.oasis-water.win-chance", 0.25)) {
                            plugin.getSpecialItemManager().giveItem(pl, "oasis-water", 1);
                            pl.sendMessage(plugin.getConfigManager().color("&bYou found &3Oasis Water&b!"));
                        }
                        if (Math.random() <= plugin.getConfig().getDouble("special-items.proof-of-success.win-chance", 1.0)) {
                            plugin.getSpecialItemManager().giveItem(pl, "proof-of-success", 1);
                        }
                        // Gold Bars: 1 per unspent gold ingot (capped)
                        int gold = 0;
                        for (org.bukkit.inventory.ItemStack is : pl.getInventory().getContents()) {
                            if (is != null && is.getType().name().contains("GOLD")) {
                                gold += is.getAmount();
                            }
                        }
                        if (gold > 0) {
                            int bars = Math.min(gold, 10);
                            plugin.getSpecialItemManager().giveItem(pl, "gold-bar", bars);
                            pl.sendMessage(plugin.getConfigManager().color(
                                    "&6You secured &e" + bars + " Gold Bar(s)&6 from leftover gold!"));
                        }
                        plugin.getPlayerDataManager().addGamePlayed(uuid);
                    }
                }
            }
            // Also count losers as games played
            try {
                Method getLosers = event.getClass().getMethod("getLosers");
                Object losers = getLosers.invoke(event);
                if (losers instanceof List) {
                    for (Object o : (List<?>) losers) {
                        UUID uuid = o instanceof UUID ? (UUID) o : null;
                        if (uuid == null) continue;
                        Player pl = Bukkit.getPlayer(uuid);
                        if (pl != null && pl.isOnline()) {
                            plugin.getDailyQuestManager().addProgress(pl, "GAMES", 1);
                            plugin.getPlayerDataManager().addGamePlayed(uuid);
                        }
                    }
                }
            } catch (Exception ignored) {}
        } catch (Exception e) {
            if (plugin.getConfigManager().isDebug()) e.printStackTrace();
        }
    }

    @SuppressWarnings("unused")
    private void onGen(Event event) {
        try {
            Method getPlayer = event.getClass().getMethod("getPlayer");
            Object p = getPlayer.invoke(event);
            if (!(p instanceof Player)) return;
            Player player = (Player) p;

            double chance = plugin.getConfig().getDouble("special-items.comfy-pillow.generator-collect-chance", 0.08);
            if (Math.random() <= chance) {
                plugin.getSpecialItemManager().giveItem(player, "comfy-pillow", 1);
                player.sendMessage(plugin.getConfigManager().color("&aYou found a &fComfy Pillow&a!"));
            }
            double ironChance = plugin.getConfig().getDouble("special-items.iron-nugget.generator-chance", 0.15);
            if (Math.random() <= ironChance) {
                plugin.getSpecialItemManager().giveItem(player, "iron-nugget", 1);
            }
            try {
                java.lang.reflect.Method getIS = event.getClass().getMethod("getItemStack");
                Object is = getIS.invoke(event);
                if (is != null && is.toString().toUpperCase().contains("DIAMOND")) {
                    double df = plugin.getConfig().getDouble("special-items.diamond-fragment.diamond-collect-chance", 0.12);
                    if (Math.random() <= df) {
                        plugin.getSpecialItemManager().giveItem(player, "diamond-fragment", 1);
                    }
                }
            } catch (Exception ignored) {}
        } catch (Exception e) {
            if (plugin.getConfigManager().isDebug()) e.printStackTrace();
        }
    }

    @SuppressWarnings("unused")
    private void onShop(Event event) {
        try {
            Method getPlayer = event.getClass().getMethod("getPlayer");
            Object p = getPlayer.invoke(event);
            if (!(p instanceof Player)) return;
            Player player = (Player) p;

            double chance = plugin.getConfig().getDouble("special-items.silver-coin.shop-buy-chance", 0.12);
            if (Math.random() <= chance) {
                plugin.getSpecialItemManager().giveItem(player, "silver-coin", 1);
            }
        } catch (Exception e) {
            // ShopBuyEvent may have different method names across versions
        }
    }

    @SuppressWarnings("unused")
    private void onJoinArena(Event event) {
        try {
            java.lang.reflect.Method getPlayer = event.getClass().getMethod("getPlayer");
            Object p = getPlayer.invoke(event);
            if (p instanceof org.bukkit.entity.Player) {
                joinTimes.put(((org.bukkit.entity.Player) p).getUniqueId(), System.currentTimeMillis());
            }
        } catch (Exception ignored) {}
    }

    @SuppressWarnings("unused")
    private void onLeave(Event event) {
        try {
            java.lang.reflect.Method getPlayer = event.getClass().getMethod("getPlayer");
            Object p = getPlayer.invoke(event);
            if (!(p instanceof org.bukkit.entity.Player)) return;
            org.bukkit.entity.Player player = (org.bukkit.entity.Player) p;
            Long start = joinTimes.remove(player.getUniqueId());
            if (start == null) return;
            long minutes = (System.currentTimeMillis() - start) / 60000L;
            // Award play-time tickets once per game leave (Hypixel: 5 tickets for time played)
            if (minutes >= 0) {
                plugin.getTicketManager().awardFromAction(player, "play-time-minutes", "Time Played");
            }
        } catch (Exception e) {
            if (plugin.getConfigManager().isDebug()) e.printStackTrace();
        }
    }

    public boolean isHooked() {
        return hooked;
    }
}
