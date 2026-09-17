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

public class HotelGuideGUI implements Listener {

    private final SlumberHotelCore plugin;
    public static final String TITLE = "§8Hotel Guide";

    public HotelGuideGUI(SlumberHotelCore plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE);

        inv.setItem(10, item(Material.PAPER, "&dSlumber Tickets",
                "&7Earn by kills, finals, beds, wins,",
                "&7play time, and quests.",
                "&7Balance: &e" + plugin.getTicketManager().getTickets(player)));

        inv.setItem(11, item(Material.GOLD_INGOT, "&6Wallet",
                "&7Tier: &e" + plugin.getWalletManager().getCurrentTier(player),
                "&7Capacity: &e" + plugin.getWalletManager().getCurrentCapacity(player),
                "&7Talk to the Receptionist to upgrade."));

        inv.setItem(12, item(Material.EMERALD, "&aDoors",
                "&7Spend tickets: &e/slumber door",
                "&7Each room has guests and quests."));

        inv.setItem(13, item(Material.BOOK, "&eQuest Log",
                "&7Open: &e/slumber quests",
                "&7Or right-click the journal bed."));

        inv.setItem(14, item(Material.NETHER_STAR, "&6Hotel Stars",
                "&7Stars: &e" + plugin.getStarsManager().getStars(player),
                "&7Earned from completing quests."));

        inv.setItem(15, item(Material.REDSTONE_COMPARATOR, "&dTicket Machine",
                "&7Cost: &e" + plugin.getTicketMachineManager().getCost(),
                "&7/slumber machine or NPC"));

        inv.setItem(16, item(Material.EXP_BOTTLE, "&bBoons",
                "&7Ticket multiplier: &e" + String.format("%.2f", plugin.getBoonManager().getMultiplier(player)) + "x",
                "&7Permanent boosts from special quests."));

        ItemStack close = item(Material.BARRIER, "&cClose");
        inv.setItem(22, close);
        player.openInventory(inv);
    }

    private ItemStack item(Material mat, String name, String... loreLines) {
        ItemStack i = new ItemStack(mat);
        ItemMeta m = i.getItemMeta();
        m.setDisplayName(plugin.getConfigManager().color(name));
        List<String> lore = new ArrayList<String>();
        for (String l : loreLines) lore.add(plugin.getConfigManager().color(l));
        m.setLore(lore);
        i.setItemMeta(m);
        return i;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (event.getInventory() == null || event.getInventory().getTitle() == null) return;
        if (!event.getInventory().getTitle().equals(TITLE)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player p = (Player) event.getWhoClicked();
        ItemStack cur = event.getCurrentItem();
        if (cur == null) return;
        if (cur.getType() == Material.BARRIER) p.closeInventory();
        if (cur.getType() == Material.BOOK) {
            p.closeInventory();
            plugin.getQuestLogGUI().open(p);
        }
    }
}
