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
import java.util.Map;

/**
 * Hypixel-style Slumber Inventory: special items + boons overview.
 */
public class SlumberInventoryGUI implements Listener {

    private final SlumberHotelCore plugin;
    public static final String TITLE = "§8Slumber Inventory";
    public static final String TITLE_BOONS = "§8Slumber Boons";

    public SlumberInventoryGUI(SlumberHotelCore plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);

        ItemStack info = named(Material.BOOK, "&dSlumber Inventory",
                "&7Quest items you hold",
                "&7Finished items stay until used",
                "",
                "&eTickets: &f" + plugin.getTicketManager().getTickets(player),
                "&eWallet: &f" + plugin.getWalletManager().getCurrentCapacity(player),
                "&eStars: &f" + plugin.getStarsManager().getStars(player),
                "&eMultiplier: &f" + String.format("%.2f", plugin.getBoonManager().getMultiplier(player)) + "x");
        inv.setItem(4, info);

        int slot = 9;
        Map<String, Integer> counts = plugin.getSpecialItemManager().countAll(player);
        for (Map.Entry<String, Integer> e : counts.entrySet()) {
            if (slot >= 45) break;
            if (e.getValue() <= 0) continue;
            ItemStack sample = plugin.getSpecialItemManager().getItem(e.getKey(), Math.min(e.getValue(), 64));
            if (sample == null) continue;
            inv.setItem(slot++, sample);
        }

        inv.setItem(49, named(Material.NETHER_STAR, "&6Boons", "&7Click to view ticket multiplier"));
        inv.setItem(53, named(Material.BARRIER, "&cClose"));
        inv.setItem(45, named(Material.PAPER, "&eQuest Log", "&7Open quest log"));
        inv.setItem(46, named(Material.COMPASS, "&aHotel Guide", "&7Open guide"));

        player.openInventory(inv);
    }

    public void openBoons(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE_BOONS);
        double mult = plugin.getBoonManager().getMultiplier(player);
        inv.setItem(13, named(Material.EXP_BOTTLE, "&dTicket Multiplier",
                "&7Current: &e" + String.format("%.2f", mult) + "x",
                "&7Permanent boons from quests",
                "&7Oasis, star milestones, Sandman, etc."));
        inv.setItem(22, named(Material.ARROW, "&7Back"));
        player.openInventory(inv);
    }

    private ItemStack named(Material mat, String name, String... lore) {
        ItemStack i = new ItemStack(mat);
        ItemMeta m = i.getItemMeta();
        m.setDisplayName(plugin.getConfigManager().color(name));
        List<String> l = new ArrayList<String>();
        for (String s : lore) l.add(plugin.getConfigManager().color(s));
        m.setLore(l);
        i.setItemMeta(m);
        return i;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (event.getInventory() == null || event.getInventory().getTitle() == null) return;
        String t = event.getInventory().getTitle();
        if (!t.equals(TITLE) && !t.equals(TITLE_BOONS)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player p = (Player) event.getWhoClicked();
        ItemStack cur = event.getCurrentItem();
        if (cur == null || cur.getType() == Material.AIR) return;

        if (t.equals(TITLE_BOONS)) {
            if (cur.getType() == Material.ARROW) open(p);
            return;
        }
        if (cur.getType() == Material.BARRIER) p.closeInventory();
        else if (cur.getType() == Material.NETHER_STAR) openBoons(p);
        else if (cur.getType() == Material.PAPER) {
            p.closeInventory();
            plugin.getQuestLogGUI().open(p);
        } else if (cur.getType() == Material.COMPASS) {
            p.closeInventory();
            plugin.getHotelGuideGUI().open(p);
        }
    }
}
