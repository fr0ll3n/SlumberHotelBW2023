package com.slumberhotel.core.listeners;

import com.slumberhotel.core.SlumberHotelCore;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.projectiles.ProjectileSource;

import java.util.List;

public class PlayerListener implements Listener {

    private final SlumberHotelCore plugin;

    public PlayerListener(SlumberHotelCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        plugin.getPlayerDataManager().getData(event.getPlayer().getUniqueId());
        plugin.getDailyQuestManager().ensureDaily(event.getPlayer());
        giveJournal(event.getPlayer());
        startActionBar(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(org.bukkit.event.player.PlayerMoveEvent event) {
        if (event.getTo() == null || event.getFrom() == null) return;
        // only when block changed
        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockY() == event.getTo().getBlockY()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) return;
        plugin.getDoorManager().handleMove(event.getPlayer(), event.getTo());
    }

    private void giveJournal(Player player) {
        if (!plugin.isLobby()) return;
        if (!plugin.getConfig().getBoolean("lobby-journal.enabled", true)) return;
        org.bukkit.Material mat;
        try {
            mat = org.bukkit.Material.valueOf(plugin.getConfig().getString("lobby-journal.material", "BED"));
        } catch (Exception e) {
            mat = org.bukkit.Material.BOOK;
        }
        org.bukkit.inventory.ItemStack item = new org.bukkit.inventory.ItemStack(mat);
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(plugin.getConfigManager().color(
                plugin.getConfig().getString("lobby-journal.name", "&dSlumber Quest Log")));
        java.util.List<String> lore = new java.util.ArrayList<String>();
        for (String line : plugin.getConfig().getStringList("lobby-journal.lore")) {
            lore.add(plugin.getConfigManager().color(line));
        }
        lore.add(plugin.getConfigManager().color("&8SHC:JOURNAL"));
        meta.setLore(lore);
        item.setItemMeta(meta);
        int slot = plugin.getConfig().getInt("lobby-journal.slot", 7);
        player.getInventory().setItem(slot, item);
    }

    @EventHandler
    public void onJournalClick(org.bukkit.event.player.PlayerInteractEvent event) {
        if (event.getItem() == null) return;
        String key = plugin.getSpecialItemManager().getSpecialKey(event.getItem());
        // journal uses SHC:JOURNAL in lore via getSpecialKey only if starts SHC:
        if (event.getItem().hasItemMeta() && event.getItem().getItemMeta().hasLore()) {
            for (String line : event.getItem().getItemMeta().getLore()) {
                String s = org.bukkit.ChatColor.stripColor(line);
                if (s != null && s.contains("SHC:JOURNAL")) {
                    event.setCancelled(true);
                    plugin.getQuestLogGUI().open(event.getPlayer());
                    return;
                }
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.getPlayerDataManager().unload(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Material type = event.getBlock().getType();
        List<String> blocks = plugin.getConfig().getStringList("special-items.treasure-map.break-blocks");
        if (blocks == null || blocks.isEmpty()) return;
        boolean match = false;
        for (String b : blocks) {
            if (type.name().equalsIgnoreCase(b)) { match = true; break; }
        }
        if (!match) return;
        double chance = plugin.getConfig().getDouble("special-items.treasure-map.chance", 0.02);
        if (Math.random() <= chance) {
            plugin.getSpecialItemManager().giveItem(player, "treasure-map", 1);
            player.sendMessage(plugin.getConfigManager().color("&6You found an &eOld Treasure Map&6!"));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPearl(ProjectileLaunchEvent event) {
        if (!(event.getEntity() instanceof org.bukkit.entity.EnderPearl)) return;
        ProjectileSource source = event.getEntity().getShooter();
        if (!(source instanceof Player)) return;
        plugin.getSpecialItemManager().giveItem((Player) source, "ender-dust", 1);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onWoolBreak(BlockBreakEvent event) {
        org.bukkit.Material type = event.getBlock().getType();
        String n = type.name();
        if (!n.contains("WOOL")) return;
        Player player = event.getPlayer();
        double chance = plugin.getConfig().getDouble("special-items.wool-cable.wool-break-chance", 0.30);
        if (Math.random() <= chance) {
            plugin.getSpecialItemManager().giveItem(player, "wool-cable", 1);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(org.bukkit.event.entity.PlayerDeathEvent event) {
        Player victim = event.getEntity();
        // Void-ish: y very low or last damage void
        boolean voidish = victim.getLocation().getY() < 0;
        try {
            org.bukkit.event.entity.EntityDamageEvent last = victim.getLastDamageCause();
            if (last != null && last.getCause() != null) {
                String c = last.getCause().name();
                if (c.contains("VOID") || c.contains("FALL")) {
                    // only void, not all falls
                    if (c.contains("VOID")) voidish = true;
                }
            }
        } catch (Exception ignored) {}
        if (!voidish) return;
        Player killer = victim.getKiller();
        if (killer != null && killer != victim) {
            plugin.getSpecialItemManager().giveItem(killer, "void-husk", 1);
        }
        // assist-like: nearby? skip for 1.8 simplicity
    }

    private final java.util.Set<java.util.UUID> actionBar = new java.util.HashSet<java.util.UUID>();

    private void startActionBar(final Player player) {
        if (!plugin.getConfig().getBoolean("action-bar.enabled", true)) return;
        if (!plugin.isLobby()) return;
        if (!actionBar.add(player.getUniqueId())) return;
        new org.bukkit.scheduler.BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    actionBar.remove(player.getUniqueId());
                    cancel();
                    return;
                }
                if (!plugin.isLobby()) return;
                int t = plugin.getTicketManager().getTickets(player);
                int cap = plugin.getWalletManager().getCurrentCapacity(player);
                String msg = plugin.getConfigManager().color(
                        plugin.getConfig().getString("action-bar.format",
                                "&dTickets: &e%tickets%&7/&e%cap%")
                                .replace("%tickets%", String.valueOf(t))
                                .replace("%cap%", String.valueOf(cap)));
                sendActionBar(player, msg);
            }
        }.runTaskTimer(plugin, 40L, 40L);
    }


    @EventHandler(priority = EventPriority.HIGH)
    public void onSpark(org.bukkit.event.player.PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) return;
        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return;
        if (!plugin.getConfig().getBoolean("spark-plug.enabled", true)) return;
        org.bukkit.Location loc = event.getClickedBlock().getLocation();
        int x = plugin.getConfig().getInt("spark-plug.x", Integer.MIN_VALUE);
        int y = plugin.getConfig().getInt("spark-plug.y", 0);
        int z = plugin.getConfig().getInt("spark-plug.z", 0);
        String world = plugin.getConfig().getString("spark-plug.world", "");
        if (x == Integer.MIN_VALUE) return;
        if (!loc.getWorld().getName().equalsIgnoreCase(world)) return;
        if (loc.getBlockX() != x || loc.getBlockY() != y || loc.getBlockZ() != z) return;
        event.setCancelled(true);
        Player player = event.getPlayer();
        if (plugin.getSpecialItemManager().countItem(player, "spark-plug") > 0) {
            player.sendMessage(plugin.getConfigManager().color("&7You already have a Spark Plug."));
            return;
        }
        plugin.getSpecialItemManager().giveItem(player, "spark-plug", 1);
        player.sendMessage(plugin.getConfigManager().color("&aYou found a &cSpark Plug&a!"));
    }

    private void sendActionBar(Player player, String message) {
        try {
            Object spigot = player.getClass().getMethod("spigot").invoke(player);
            Class<?> chatMsgType = Class.forName("net.md_5.bungee.api.ChatMessageType");
            Object actionBarEnum = null;
            for (Object c : chatMsgType.getEnumConstants()) {
                if (c.toString().equals("ACTION_BAR")) { actionBarEnum = c; break; }
            }
            if (actionBarEnum == null) return;
            Class<?> textComp = Class.forName("net.md_5.bungee.api.chat.TextComponent");
            Object component = textComp.getConstructor(String.class).newInstance(message);
            Class<?> base = Class.forName("net.md_5.bungee.api.chat.BaseComponent");
            Object arr = java.lang.reflect.Array.newInstance(base, 1);
            java.lang.reflect.Array.set(arr, 0, component);
            for (java.lang.reflect.Method m : spigot.getClass().getMethods()) {
                if (m.getName().equals("sendMessage") && m.getParameterTypes().length == 2) {
                    m.invoke(spigot, actionBarEnum, arr);
                    break;
                }
            }
        } catch (Throwable ignored) {}
    }

}
