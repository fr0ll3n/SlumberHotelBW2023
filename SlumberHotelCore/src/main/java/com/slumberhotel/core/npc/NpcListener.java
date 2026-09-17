package com.slumberhotel.core.npc;

import com.slumberhotel.core.SlumberHotelCore;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.plugin.EventExecutor;

import java.lang.reflect.Method;

/**
 * Citizens NPC right-click → quest conversation.
 * Maps Citizens NPC id OR name to config npc key under npcs:
 */
public class NpcListener implements Listener {

    private final SlumberHotelCore plugin;

    public NpcListener(SlumberHotelCore plugin) {
        this.plugin = plugin;
        tryHookCitizens();
    }

    private void tryHookCitizens() {
        if (Bukkit.getPluginManager().getPlugin("Citizens") == null) {
            plugin.getLogger().info("Citizens not found – using vanilla entity interact only.");
            return;
        }
        // net.citizensnpcs.api.event.NPCRightClickEvent
        try {
            @SuppressWarnings("unchecked")
            Class<? extends Event> clazz = (Class<? extends Event>) Class.forName("net.citizensnpcs.api.event.NPCRightClickEvent");
            EventExecutor exec = new EventExecutor() {
                @Override
                public void execute(Listener listener, Event event) {
                    handleCitizensClick(event);
                }
            };
            Bukkit.getPluginManager().registerEvent(clazz, this, EventPriority.NORMAL, exec, plugin);
            plugin.getLogger().info("Hooked Citizens NPCRightClickEvent.");
        } catch (ClassNotFoundException e) {
            plugin.getLogger().warning("Citizens API class not found.");
        }
    }

    private void handleCitizensClick(Event event) {
        try {
            Method getNPC = event.getClass().getMethod("getNPC");
            Object npc = getNPC.invoke(event);
            Method getClicker = event.getClass().getMethod("getClicker");
            Object clicker = getClicker.invoke(event);
            if (!(clicker instanceof Player)) return;
            Player player = (Player) clicker;

            int id = -1;
            String name = "";
            try {
                Method getId = npc.getClass().getMethod("getId");
                Object idObj = getId.invoke(npc);
                if (idObj instanceof Integer) id = (Integer) idObj;
            } catch (Exception ignored) {}
            try {
                Method getName = npc.getClass().getMethod("getName");
                Object n = getName.invoke(npc);
                if (n != null) name = n.toString().replaceAll("§[0-9a-fk-or]", "").replaceAll("&[0-9a-fk-or]", "");
            } catch (Exception ignored) {}

            String key = resolveNpcKey(id, name);
            if (key == null) return;
            if ("ticket_machine".equalsIgnoreCase(key)) {
                plugin.getTicketMachineManager().roll(player);
                return;
            }
            plugin.getQuestManager().handleNpcClick(player, key);
        } catch (Exception e) {
            if (plugin.getConfigManager().isDebug()) e.printStackTrace();
        }
    }

    @org.bukkit.event.EventHandler
    public void onEntityInteract(PlayerInteractEntityEvent event) {
        // Fallback when not Citizens NPC
        if (Bukkit.getPluginManager().getPlugin("Citizens") != null) return;
        Entity ent = event.getRightClicked();
        String name = ent.getCustomName();
        if (name == null) return;
        name = name.replaceAll("§[0-9a-fk-or]", "").replaceAll("&[0-9a-fk-or]", "");
        String key = resolveNpcKey(-1, name);
        if (key != null) {
            plugin.getQuestManager().handleNpcClick(event.getPlayer(), key);
        }
    }

    private String resolveNpcKey(int citizensId, String name) {
        // npcs:
        //   doorman_dave:
        //     citizens-id: 1
        //     names: [Doorman Dave, Dave]
        org.bukkit.configuration.ConfigurationSection root = plugin.getConfig().getConfigurationSection("npcs");
        if (root == null) return null;
        for (String key : root.getKeys(false)) {
            org.bukkit.configuration.ConfigurationSection s = root.getConfigurationSection(key);
            if (s == null) continue;
            if (citizensId >= 0 && s.getInt("citizens-id", -1) == citizensId) return key;
            for (String n : s.getStringList("names")) {
                if (n.equalsIgnoreCase(name)) return key;
            }
            if (key.replace('_', ' ').equalsIgnoreCase(name)) return key;
        }
        return null;
    }
}
