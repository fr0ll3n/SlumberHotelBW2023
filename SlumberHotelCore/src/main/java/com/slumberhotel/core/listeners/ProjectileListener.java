package com.slumberhotel.core.listeners;

import com.slumberhotel.core.SlumberHotelCore;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.projectiles.ProjectileSource;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks ender pearl travel distance → Ender Dust (Hypixel-style).
 */
public class ProjectileListener implements Listener {

    private final SlumberHotelCore plugin;
    private final Map<UUID, double[]> launchPos = new ConcurrentHashMap<UUID, double[]>();

    public ProjectileListener(SlumberHotelCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLaunch(ProjectileLaunchEvent event) {
        Projectile proj = event.getEntity();
        if (!(proj instanceof EnderPearl)) return;
        ProjectileSource src = proj.getShooter();
        if (!(src instanceof Player)) return;
        launchPos.put(proj.getUniqueId(), new double[]{
                proj.getLocation().getX(),
                proj.getLocation().getY(),
                proj.getLocation().getZ()
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHit(ProjectileHitEvent event) {
        Projectile proj = event.getEntity();
        if (!(proj instanceof EnderPearl)) return;
        double[] start = launchPos.remove(proj.getUniqueId());
        if (start == null) return;
        ProjectileSource src = proj.getShooter();
        if (!(src instanceof Player)) return;
        Player player = (Player) src;

        double dx = proj.getLocation().getX() - start[0];
        double dy = proj.getLocation().getY() - start[1];
        double dz = proj.getLocation().getZ() - start[2];
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);

        // Hypixel: dust scales with distance – 1 dust per ~2 blocks, min 1 if any travel
        int dust = (int) Math.floor(dist / 2.0);
        if (dust < 1 && dist >= 1.0) dust = 1;
        if (dust > 64) dust = 64;
        if (dust <= 0) return;

        plugin.getSpecialItemManager().giveItem(player, "ender-dust", dust);
        if (plugin.getConfigManager().isDebug()) {
            player.sendMessage(plugin.getConfigManager().color(
                    "&8[Debug] Pearl traveled " + String.format("%.1f", dist) + " → " + dust + " Ender Dust"));
        }
    }
}
