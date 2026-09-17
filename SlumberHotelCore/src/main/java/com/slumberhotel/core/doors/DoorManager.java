package com.slumberhotel.core.doors;

import com.slumberhotel.core.SlumberHotelCore;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DoorManager {

    private final SlumberHotelCore plugin;
    private final Map<String, DoorDef> doors = new HashMap<String, DoorDef>();
    /** Prevent spam teleport while standing in trigger */
    private final Map<UUID, Long> lastTrigger = new ConcurrentHashMap<UUID, Long>();

    public static class DoorDef {
        public String id;
        public String name;
        public int cost;
        public String permission;
        public Location teleport;
        public String region;
        /** Optional walk-in trigger cuboid (same world as trigger.world) */
        public String triggerWorld;
        public int minX, minY, minZ, maxX, maxY, maxZ;
        public boolean hasTrigger;
    }

    public DoorManager(SlumberHotelCore plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        doors.clear();
        ConfigurationSection root = plugin.getConfig().getConfigurationSection("doors");
        if (root == null) return;
        for (String id : root.getKeys(false)) {
            ConfigurationSection s = root.getConfigurationSection(id);
            if (s == null) continue;
            DoorDef d = new DoorDef();
            d.id = id;
            d.name = s.getString("name", id);
            d.cost = s.getInt("cost", 100);
            d.permission = s.getString("permission", "slumber.door." + id);
            d.region = s.getString("region", "");
            if (s.contains("teleport")) {
                String world = s.getString("teleport.world", "world");
                World w = Bukkit.getWorld(world);
                if (w != null) {
                    d.teleport = new Location(w,
                            s.getDouble("teleport.x"),
                            s.getDouble("teleport.y"),
                            s.getDouble("teleport.z"),
                            (float) s.getDouble("teleport.yaw", 0),
                            (float) s.getDouble("teleport.pitch", 0));
                }
            }
            if (s.contains("trigger")) {
                d.hasTrigger = true;
                d.triggerWorld = s.getString("trigger.world", "world");
                int x1 = s.getInt("trigger.x1");
                int y1 = s.getInt("trigger.y1");
                int z1 = s.getInt("trigger.z1");
                int x2 = s.getInt("trigger.x2", x1);
                int y2 = s.getInt("trigger.y2", y1);
                int z2 = s.getInt("trigger.z2", z1);
                d.minX = Math.min(x1, x2);
                d.maxX = Math.max(x1, x2);
                d.minY = Math.min(y1, y2);
                d.maxY = Math.max(y1, y2);
                d.minZ = Math.min(z1, z2);
                d.maxZ = Math.max(z1, z2);
            }
            doors.put(id, d);
        }
        plugin.getLogger().info("Loaded " + doors.size() + " hotel doors.");
    }

    public DoorDef get(String id) { return doors.get(id); }
    public Set<String> getIds() { return doors.keySet(); }
    public Iterable<DoorDef> all() { return doors.values(); }

    public boolean isUnlocked(Player player, String doorId) {
        return plugin.getPlayerDataManager().isDoorUnlocked(player.getUniqueId(), doorId);
    }

    public boolean unlock(Player player, String doorId) {
        DoorDef d = doors.get(doorId);
        if (d == null) return false;
        if (isUnlocked(player, doorId)) {
            player.sendMessage(plugin.getConfigManager().color("&7Door already unlocked."));
            return true;
        }
        int tickets = plugin.getTicketManager().getTickets(player);
        if (tickets < d.cost) {
            player.sendMessage(plugin.getConfigManager().color(
                    "&cNeed &e" + d.cost + " &ctickets to unlock &e" + d.name + "&c. You have &e"
                            + tickets + "&c."));
            return false;
        }
        plugin.getTicketManager().takeTickets(player, d.cost);
        plugin.getPlayerDataManager().unlockDoor(player.getUniqueId(), doorId);
        if (d.permission != null && !d.permission.isEmpty()) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                    "lp user " + player.getName() + " permission set " + d.permission + " true");
        }
        try {
            String sound = plugin.getConfig().getString("sounds.door-unlock", "ANVIL_LAND");
            player.playSound(player.getLocation(), org.bukkit.Sound.valueOf(sound), 1f, 1f);
        } catch (Exception ignored) {}
        player.sendMessage(plugin.getConfigManager().color(
                "&aUnlocked &e" + d.name + " &afor &e" + d.cost + " &atickets!"));
        return true;
    }

    public void enter(Player player, String doorId) {
        DoorDef d = doors.get(doorId);
        if (d == null) return;
        if (!isUnlocked(player, doorId)) {
            if (!unlock(player, doorId)) return;
        }
        if (d.teleport != null) {
            player.teleport(d.teleport);
            player.sendMessage(plugin.getConfigManager().color("&7Entering &e" + d.name + "&7..."));
        }
    }

    /** Called on player move – walk into trigger cuboid to enter. */
    public void handleMove(Player player, Location to) {
        if (to == null || to.getWorld() == null) return;
        if (!plugin.isLobby()) return;
        long now = System.currentTimeMillis();
        Long last = lastTrigger.get(player.getUniqueId());
        if (last != null && now - last < 2000L) return;

        for (DoorDef d : doors.values()) {
            if (!d.hasTrigger) continue;
            if (!to.getWorld().getName().equalsIgnoreCase(d.triggerWorld)) continue;
            int x = to.getBlockX();
            int y = to.getBlockY();
            int z = to.getBlockZ();
            if (x < d.minX || x > d.maxX || y < d.minY || y > d.maxY || z < d.minZ || z > d.maxZ) {
                continue;
            }
            lastTrigger.put(player.getUniqueId(), now);
            enter(player, d.id);
            return;
        }
    }
}
