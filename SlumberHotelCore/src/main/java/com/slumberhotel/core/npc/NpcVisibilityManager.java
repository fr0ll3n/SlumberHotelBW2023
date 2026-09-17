package com.slumberhotel.core.npc;

import com.slumberhotel.core.SlumberHotelCore;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Hypixel-style: hide guest NPCs until the player has completed required quests
 * (e.g. receptionist). Uses Citizens show/hide per player via reflection.
 */
public class NpcVisibilityManager implements Listener {

    private final SlumberHotelCore plugin;
    /** npc config key -> required quest ids (all must be complete) */
    private final Map<String, List<String>> requirements = new HashMap<String, List<String>>();

    public NpcVisibilityManager(SlumberHotelCore plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        requirements.clear();
        ConfigurationSection root = plugin.getConfig().getConfigurationSection("npcs");
        if (root == null) return;
        for (String key : root.getKeys(false)) {
            ConfigurationSection s = root.getConfigurationSection(key);
            if (s == null) continue;
            List<String> req = s.getStringList("requires-quests");
            if (req != null && !req.isEmpty()) {
                requirements.put(key, req);
            }
            // also support single require-quest string
            String single = s.getString("require-quest", "");
            if (single != null && !single.isEmpty()) {
                requirements.put(key, java.util.Collections.singletonList(single));
            }
        }
        plugin.getLogger().info("NPC visibility rules: " + requirements.size());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
            @Override
            public void run() {
                if (player.isOnline()) applyAll(player);
            }
        }, 40L);
    }

    public void applyAll(Player player) {
        if (Bukkit.getPluginManager().getPlugin("Citizens") == null) return;
        ConfigurationSection root = plugin.getConfig().getConfigurationSection("npcs");
        if (root == null) return;
        for (String key : root.getKeys(false)) {
            ConfigurationSection s = root.getConfigurationSection(key);
            if (s == null) continue;
            int id = s.getInt("citizens-id", -1);
            if (id < 0) continue;
            boolean visible = isVisible(player, key);
            setVisible(player, id, visible);
        }
    }

    public boolean isVisible(Player player, String npcKey) {
        // Always show doorman + receptionist so the chain can start
        if ("doorman_dave".equals(npcKey) || "receptionist".equals(npcKey) || "ticket_machine".equals(npcKey)) {
            return true;
        }
        List<String> req = requirements.get(npcKey);
        if (req == null || req.isEmpty()) {
            // default: require hotel entered OR meet_receptionist if that quest exists
            UUID uuid = player.getUniqueId();
            if (plugin.getPlayerDataManager().isQuestCompleted(uuid, "meet_receptionist")
                    || plugin.getPlayerDataManager().isQuestCompleted(uuid, "ready_for_nap")
                    || plugin.getPlayerDataManager().hasEnteredHotel(player)) {
                return true;
            }
            // if no progress yet, only always-visible NPCs
            return false;
        }
        for (String q : req) {
            if (!plugin.getPlayerDataManager().isQuestCompleted(player.getUniqueId(), q)) {
                return false;
            }
        }
        return true;
    }

    private void setVisible(Player player, int citizensId, boolean visible) {
        try {
            Class<?> citizensAPI = Class.forName("net.citizensnpcs.api.CitizensAPI");
            Object registry = citizensAPI.getMethod("getNPCRegistry").invoke(null);
            Object npc = registry.getClass().getMethod("getById", int.class).invoke(registry, citizensId);
            if (npc == null) return;
            // NPC.hide(Player) / NPC.show(Player) in Citizens 2
            if (visible) {
                try {
                    npc.getClass().getMethod("show", Player.class).invoke(npc, player);
                } catch (NoSuchMethodException e) {
                    // older API: spawn for player via entity
                }
            } else {
                try {
                    npc.getClass().getMethod("hide", Player.class).invoke(npc, player);
                } catch (NoSuchMethodException e) {
                    // ignore
                }
            }
        } catch (Throwable ignored) {}
    }

    /** Call after quest complete so new NPCs appear */
    public void refresh(Player player) {
        applyAll(player);
    }
}
