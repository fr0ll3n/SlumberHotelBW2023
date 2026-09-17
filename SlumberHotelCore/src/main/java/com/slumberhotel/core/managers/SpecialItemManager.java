package com.slumberhotel.core.managers;

import com.slumberhotel.core.SlumberHotelCore;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Creates and identifies special quest items used by the hotel.
 * Items are identified by a special lore line + name so BetonQuest can also detect them.
 */
public class SpecialItemManager {

    private final SlumberHotelCore plugin;
    private final Map<String, ItemStack> cache = new HashMap<String, ItemStack>();

    public SpecialItemManager(SlumberHotelCore plugin) {
        this.plugin = plugin;
        loadItems();
    }

    public void loadItems() {
        cache.clear();
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("special-items");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            ConfigurationSection itemSec = section.getConfigurationSection(key);
            if (itemSec == null) continue;

            String matName = itemSec.getString("material", "STONE");
            Material mat = Material.matchMaterial(matName);
            if (mat == null) {
                plugin.getLogger().warning("Unknown material for special item " + key + ": " + matName);
                continue;
            }

            short data = (short) itemSec.getInt("data", 0);
            ItemStack item = new ItemStack(mat, 1, data);
            ItemMeta meta = item.getItemMeta();

            String name = itemSec.getString("name", key);
            meta.setDisplayName(plugin.getConfigManager().color(name));

            List<String> lore = new ArrayList<String>();
            for (String line : itemSec.getStringList("lore")) {
                lore.add(plugin.getConfigManager().color(line));
            }
            // Hidden identifier
            lore.add(plugin.getConfigManager().color("&8SHC:" + key.toUpperCase()));
            meta.setLore(lore);
            item.setItemMeta(meta);

            cache.put(key.toLowerCase(), item);
        }
        plugin.getLogger().info("Loaded " + cache.size() + " special quest items.");
    }

    public ItemStack getItem(String key) {
        ItemStack base = cache.get(key.toLowerCase());
        if (base == null) return null;
        return base.clone();
    }

    public ItemStack getItem(String key, int amount) {
        ItemStack item = getItem(key);
        if (item == null) return null;
        item.setAmount(amount);
        return item;
    }

    public void giveItem(Player player, String key, int amount) {
        ItemStack item = getItem(key, amount);
        if (item == null) {
            plugin.getLogger().warning("Tried to give unknown special item: " + key);
            return;
        }
        HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(item);
        if (!leftover.isEmpty()) {
            for (ItemStack left : leftover.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), left);
            }
        }
    }

    /**
     * Check if an ItemStack is one of our special items.
     */
    public String getSpecialKey(ItemStack item) {
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasLore()) return null;
        List<String> lore = item.getItemMeta().getLore();
        if (lore == null) return null;
        for (String line : lore) {
            String stripped = org.bukkit.ChatColor.stripColor(line);
            if (stripped != null && stripped.startsWith("SHC:")) {
                return stripped.substring(4).toLowerCase();
            }
        }
        return null;
    }

    public boolean isSpecialItem(ItemStack item, String key) {
        String found = getSpecialKey(item);
        return found != null && found.equalsIgnoreCase(key);
    }

    public int countItem(org.bukkit.entity.Player player, String key) {
        int count = 0;
        for (org.bukkit.inventory.ItemStack stack : player.getInventory().getContents()) {
            if (isSpecialItem(stack, key)) count += stack.getAmount();
        }
        return count;
    }

    public boolean takeItem(org.bukkit.entity.Player player, String key, int amount) {
        if (countItem(player, key) < amount) return false;
        int remaining = amount;
        org.bukkit.inventory.ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            org.bukkit.inventory.ItemStack stack = contents[i];
            if (!isSpecialItem(stack, key)) continue;
            int take = Math.min(remaining, stack.getAmount());
            if (take >= stack.getAmount()) {
                player.getInventory().setItem(i, null);
            } else {
                stack.setAmount(stack.getAmount() - take);
            }
            remaining -= take;
            if (remaining <= 0) break;
        }
        player.updateInventory();
        return remaining <= 0;
    }

    public java.util.Map<String, Integer> countAll(org.bukkit.entity.Player player) {
        java.util.Map<String, Integer> map = new java.util.LinkedHashMap<String, Integer>();
        org.bukkit.configuration.ConfigurationSection root = plugin.getConfig().getConfigurationSection("special-items");
        if (root == null) return map;
        for (String key : root.getKeys(false)) {
            int c = countItem(player, key);
            if (c > 0) map.put(key, c);
        }
        return map;
    }
}
