package com.slumberhotel.core.quests;

import com.slumberhotel.core.SlumberHotelCore;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple 1.8-compatible quest log inventory GUI.
 */
public class QuestLogGUI implements Listener {

    private final SlumberHotelCore plugin;
    public static final String TITLE = "§8Slumber Quest Log";

    public QuestLogGUI(SlumberHotelCore plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);

        // Info item
        ItemStack info = new ItemStack(Material.BOOK);
        ItemMeta im = info.getItemMeta();
        im.setDisplayName("§d§lYour Progress");
        List<String> il = new ArrayList<String>();
        il.add("§7Tickets: §e" + plugin.getTicketManager().getTickets(player)
                + "§7/§e" + plugin.getWalletManager().getCurrentCapacity(player));
        il.add("§7Stars: §6" + plugin.getStarsManager().getStars(player));
        il.add("§7Games played: §e" + plugin.getPlayerDataManager().getGamesPlayed(player.getUniqueId()));
        il.add("§7Active quests: §a" + plugin.getQuestManager().getActive(player).size());
        il.add("§7Completed: §a" + plugin.getQuestManager().getCompleted(player).size());
        im.setLore(il);
        info.setItemMeta(im);
        inv.setItem(4, info);

        int slot = 9;
        for (String id : plugin.getQuestManager().getActive(player)) {
            if (slot >= 36) break;
            QuestDefinition q = plugin.getQuestManager().get(id);
            if (q == null) continue;
            ItemStack paper = new ItemStack(Material.PAPER);
            ItemMeta meta = paper.getItemMeta();
            meta.setDisplayName(plugin.getConfigManager().color("&a" + strip(q.name)));
            List<String> lore = new ArrayList<String>();
            lore.add(plugin.getConfigManager().color("&7" + strip(q.description)));
            lore.add("");
            lore.add(plugin.getConfigManager().color("&eACTIVE"));
            if ("COLLECT_ITEM".equals(q.objectiveType)) {
                int has = plugin.getSpecialItemManager().countItem(player, q.itemKey);
                lore.add(plugin.getConfigManager().color(
                        "&7" + has + "/" + q.itemAmount + " " + q.itemKey));
            } else if ("PAY_TICKETS".equals(q.objectiveType)) {
                lore.add(plugin.getConfigManager().color("&7Cost: &e" + q.ticketCost + " tickets"));
            }
            meta.setLore(lore);
            paper.setItemMeta(meta);
            inv.setItem(slot++, paper);
        }

        slot = 36;
        for (String id : plugin.getQuestManager().getCompleted(player)) {
            if (slot >= 54) break;
            QuestDefinition q = plugin.getQuestManager().get(id);
            String name = q != null ? strip(q.name) : id;
            ItemStack item = new ItemStack(Material.EMERALD);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName("§a✔ " + name);
            List<String> lore = new ArrayList<String>();
            lore.add("§7Completed");
            meta.setLore(lore);
            item.setItemMeta(meta);
            inv.setItem(slot++, item);
        }

        // Close
        ItemStack close = new ItemStack(Material.BARRIER);
        ItemMeta cm = close.getItemMeta();
        cm.setDisplayName("§cClose");
        close.setItemMeta(cm);
        inv.setItem(49, close);

        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (event.getInventory() == null) return;
        String title = event.getInventory().getTitle();
        if (title == null || !title.equals(TITLE)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        ItemStack cur = event.getCurrentItem();
        if (cur == null || cur.getType() == Material.AIR) return;
        if (cur.getType() == Material.BARRIER) {
            player.closeInventory();
        }
    }

    private String strip(String s) {
        return s == null ? "" : s.replaceAll("&[0-9a-fk-or]", "").replaceAll("§[0-9a-fk-or]", "");
    }
}
