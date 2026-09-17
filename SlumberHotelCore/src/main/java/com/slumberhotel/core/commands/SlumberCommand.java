package com.slumberhotel.core.commands;

import com.slumberhotel.core.SlumberHotelCore;
import com.slumberhotel.core.quests.QuestDefinition;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class SlumberCommand implements CommandExecutor {

    private final SlumberHotelCore plugin;

    public SlumberCommand(SlumberHotelCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) { sendHelp(sender); return true; }
        String sub = args[0].toLowerCase();

        if (sub.equals("tickets") || sub.equals("bal") || sub.equals("balance")) {
            handleBalance(sender, args); return true;
        }
        if (sub.equals("wallet")) { handleWallet(sender); return true; }
        if (sub.equals("daily") || sub.equals("dq")) { handleDaily(sender); return true; }
        if (sub.equals("quests") || sub.equals("quest") || sub.equals("log") || sub.equals("journal")) {
            handleQuestLog(sender); return true;
        }
        if (sub.equals("door") || sub.equals("doors")) { handleDoor(sender, args); return true; }
        if (sub.equals("machine") || sub.equals("tm") || sub.equals("roll")) {
            if (!(sender instanceof Player)) return true;
            plugin.getTicketMachineManager().roll((Player) sender);
            return true;
        }
        if (sub.equals("holo") || sub.equals("holograms")) {
            if (!sender.hasPermission("slumber.admin")) return true;
            sender.sendMessage(plugin.getConfigManager().color("&8&m-----&r &dDoor Hologram Setup &8&m-----"));
            sender.sendMessage(plugin.getConfigManager().color("&7Stand at each door and run:"));
            for (String id : plugin.getDoorManager().getIds()) {
                com.slumberhotel.core.doors.DoorManager.DoorDef d = plugin.getDoorManager().get(id);
                sender.sendMessage(plugin.getConfigManager().color(
                        "&e" + id + " &7cost &e" + d.cost + " &8- &f/npc create " + d.name));
                sender.sendMessage(plugin.getConfigManager().color(
                        "  &7/npc hologram add &e" + d.name));
                sender.sendMessage(plugin.getConfigManager().color(
                        "  &7/npc hologram add &eCost: " + d.cost + " tickets"));
                sender.sendMessage(plugin.getConfigManager().color(
                        "  &7/npc cmdadd -o slumber door " + id));
            }
            sender.sendMessage(plugin.getConfigManager().color(
                    "&7Or use walk-in triggers under doors.<id>.trigger in config.yml"));
            return true;
        }
        if (sub.equals("settrigger") && sender.hasPermission("slumber.admin")) {
            if (!(sender instanceof Player)) return true;
            if (args.length < 2) {
                sender.sendMessage("/slumber settrigger <doorId> &7- sets 3x3 trigger at your feet");
                return true;
            }
            String id = args[1].toLowerCase();
            if (plugin.getDoorManager().get(id) == null) {
                sender.sendMessage("Unknown door");
                return true;
            }
            Player pl = (Player) sender;
            org.bukkit.Location l = pl.getLocation();
            org.bukkit.configuration.file.FileConfiguration cfg = plugin.getConfig();
            String path = "doors." + id + ".trigger";
            cfg.set(path + ".world", l.getWorld().getName());
            cfg.set(path + ".x1", l.getBlockX() - 1);
            cfg.set(path + ".y1", l.getBlockY());
            cfg.set(path + ".z1", l.getBlockZ() - 1);
            cfg.set(path + ".x2", l.getBlockX() + 1);
            cfg.set(path + ".y2", l.getBlockY() + 2);
            cfg.set(path + ".z2", l.getBlockZ() + 1);
            plugin.saveConfig();
            plugin.getDoorManager().reload();
            sender.sendMessage(plugin.getConfigManager().color(
                    "&aTrigger set for &e" + id + " &aat your location (3x3x3). Walk in to enter."));
            return true;
        }
        if (sub.equals("settp") && sender.hasPermission("slumber.admin")) {
            if (!(sender instanceof Player)) return true;
            if (args.length < 2) {
                sender.sendMessage("/slumber settp <doorId>");
                return true;
            }
            String id = args[1].toLowerCase();
            if (plugin.getDoorManager().get(id) == null) {
                sender.sendMessage("Unknown door");
                return true;
            }
            Player pl = (Player) sender;
            org.bukkit.Location l = pl.getLocation();
            String path = "doors." + id + ".teleport";
            plugin.getConfig().set(path + ".world", l.getWorld().getName());
            plugin.getConfig().set(path + ".x", l.getX());
            plugin.getConfig().set(path + ".y", l.getY());
            plugin.getConfig().set(path + ".z", l.getZ());
            plugin.getConfig().set(path + ".yaw", l.getYaw());
            plugin.getConfig().set(path + ".pitch", l.getPitch());
            plugin.saveConfig();
            plugin.getDoorManager().reload();
            sender.sendMessage(plugin.getConfigManager().color("&aTeleport for &e" + id + " &aset."));
            return true;
        }
        if (sub.equals("inv") || sub.equals("inventory") || sub.equals("items")) {
            if (sender instanceof Player) plugin.getSlumberInventoryGUI().open((Player) sender);
            return true;
        }
        if (sub.equals("guide") || sub.equals("help") || sub.equals("menu")) {
            if (sender instanceof Player) plugin.getHotelGuideGUI().open((Player) sender);
            return true;
        }
        if (sub.equals("listquests") && sender.hasPermission("slumber.admin")) {
            for (String id : plugin.getQuestManager().getAllIds()) {
                sender.sendMessage("- " + id);
            }
            return true;
        }
        if (sub.equals("completequest") && sender.hasPermission("slumber.admin")) {
            if (args.length < 3) { sender.sendMessage("/slumber completequest <player> <questId>"); return true; }
            Player t = Bukkit.getPlayer(args[1]);
            if (t == null) return true;
            com.slumberhotel.core.quests.QuestDefinition q = plugin.getQuestManager().get(args[2]);
            if (q == null) { sender.sendMessage("Unknown quest"); return true; }
            if (!plugin.getQuestManager().isActive(t, q.id)) {
                plugin.getPlayerDataManager().addActiveQuest(t.getUniqueId(), q.id);
            }
            plugin.getQuestManager().completeQuest(t, q);
            sender.sendMessage("Forced complete " + q.id);
            return true;
        }
        if (sub.equals("cosmetics") || sub.equals("cosmetic")) {
            handleCosmetics(sender, args);
            return true;
        }
        if (sub.equals("stars") || sub.equals("star")) {
            if (!(sender instanceof Player)) return true;
            Player pl = (Player) sender;
            sender.sendMessage(plugin.getConfigManager().color(
                    "&6Hotel Stars: &e" + plugin.getStarsManager().getStars(pl)));
            return true;
        }
        if (sub.equals("giveitem") && sender.hasPermission("slumber.admin")) {
            handleGiveItem(sender, args); return true;
        }
        if (sub.equals("give") && sender.hasPermission("slumber.admin")) {
            handleGiveTickets(sender, args); return true;
        }
        if (sub.equals("setwallet") && sender.hasPermission("slumber.admin")) {
            handleSetWallet(sender, args); return true;
        }
        if (sub.equals("resetquest") && sender.hasPermission("slumber.admin")) {
            if (args.length < 3) { sender.sendMessage("Usage: /slumber resetquest <player> <questId|all>"); return true; }
            Player t = Bukkit.getPlayer(args[1]);
            if (t == null) { sender.sendMessage("Player offline"); return true; }
            if (args[2].equalsIgnoreCase("all")) {
                plugin.getPlayerDataManager().getData(t.getUniqueId()).set("quests", null);
                plugin.getPlayerDataManager().save(t.getUniqueId());
                sender.sendMessage("Reset all quests for " + t.getName());
            } else {
                sender.sendMessage("Use all for now or edit playerdata yml");
            }
            return true;
        }
        if (sub.equals("givestars") && sender.hasPermission("slumber.admin")) {
            if (args.length < 3) { sender.sendMessage("/slumber givestars <player> <amount>"); return true; }
            Player t = Bukkit.getPlayer(args[1]);
            if (t == null) return true;
            plugin.getStarsManager().addStars(t, Integer.parseInt(args[2]), "Admin");
            sender.sendMessage("OK");
            return true;
        }
        if (sub.equals("reload") && sender.hasPermission("slumber.admin")) {
            plugin.reload();
            sender.sendMessage(plugin.getConfigManager().getMessage("reloaded"));
            return true;
        }
        if (sub.equals("info")) { handleInfo(sender); return true; }
        sendHelp(sender);
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(plugin.getConfigManager().color("&8&m----------&r &dSlumber Hotel &8&m----------"));
        sender.sendMessage(plugin.getConfigManager().color("&e/slumber tickets [player]"));
        sender.sendMessage(plugin.getConfigManager().color("&e/slumber wallet"));
        sender.sendMessage(plugin.getConfigManager().color("&e/slumber quests &7- Quest log"));
        sender.sendMessage(plugin.getConfigManager().color("&e/slumber daily"));
        sender.sendMessage(plugin.getConfigManager().color("&e/slumber door <id> &7- Unlock/enter door"));
        sender.sendMessage(plugin.getConfigManager().color("&e/slumber machine &7- Ticket Machine"));
        sender.sendMessage(plugin.getConfigManager().color("&e/slumber guide &7- Hotel guide menu"));
        sender.sendMessage(plugin.getConfigManager().color("&e/slumber stars"));
        if (sender.hasPermission("slumber.admin")) {
            sender.sendMessage(plugin.getConfigManager().color("&c/slumber give|setwallet|giveitem|reload"));
        }
    }

    private void handleQuestLog(CommandSender sender) {
        if (!(sender instanceof Player)) { sender.sendMessage("Players only"); return; }
        Player p = (Player) sender;
        // GUI by default; chat with "chat" arg
        if (true) {
            plugin.getQuestLogGUI().open(p);
            return;
        }
        sender.sendMessage(plugin.getConfigManager().color("&8&m----------&r &dQuest Log &8&m----------"));
        List<String> active = plugin.getQuestManager().getActive(p);
        List<String> done = plugin.getQuestManager().getCompleted(p);
        if (active.isEmpty()) {
            sender.sendMessage(plugin.getConfigManager().color("&7No active quests. Talk to hotel guests!"));
        } else {
            sender.sendMessage(plugin.getConfigManager().color("&aActive:"));
            for (String id : active) {
                QuestDefinition q = plugin.getQuestManager().get(id);
                String name = q != null ? q.name : id;
                sender.sendMessage(plugin.getConfigManager().color("&7- &e" + name.replaceAll("&[0-9a-fk-or]", "")));
                if (q != null && "COLLECT_ITEM".equals(q.objectiveType)) {
                    int has = plugin.getSpecialItemManager().countItem(p, q.itemKey);
                    sender.sendMessage(plugin.getConfigManager().color(
                            "  &8" + has + "/" + q.itemAmount + " " + q.itemKey));
                }
            }
        }
        sender.sendMessage(plugin.getConfigManager().color("&7Completed: &e" + done.size()));
        sender.sendMessage(plugin.getConfigManager().color(
                "&7Games played: &e" + plugin.getPlayerDataManager().getGamesPlayed(p.getUniqueId())));
        sender.sendMessage(plugin.getConfigManager().color(
                "&7Entered hotel: &e" + plugin.getPlayerDataManager().hasEnteredHotel(p)));
    }

    private void handleDoor(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) return;
        Player p = (Player) sender;
        if (args.length < 2) {
            sender.sendMessage(plugin.getConfigManager().color("&7Unlocked doors:"));
            for (String id : plugin.getDoorManager().getIds()) {
                boolean u = plugin.getDoorManager().isUnlocked(p, id);
                com.slumberhotel.core.doors.DoorManager.DoorDef d = plugin.getDoorManager().get(id);
                sender.sendMessage(plugin.getConfigManager().color(
                        (u ? "&a" : "&c") + "- " + (d != null ? d.name : id)
                                + " &8(" + (d != null ? d.cost : "?") + " tickets)"));
            }
            sender.sendMessage(plugin.getConfigManager().color("&e/slumber door <id> &7to unlock/enter"));
            return;
        }
        plugin.getDoorManager().enter(p, args[1].toLowerCase());
    }

    private void handleDaily(CommandSender sender) {
        if (!(sender instanceof Player)) return;
        Player player = (Player) sender;
        plugin.getDailyQuestManager().ensureDaily(player);
        List<String> assigned = plugin.getDailyQuestManager().getAssigned(player);
        sender.sendMessage(plugin.getConfigManager().color("&8&m----------&r &dDaily Quests &8&m----------"));
        for (String id : assigned) {
            boolean done = plugin.getDailyQuestManager().isCompleted(player, id);
            int prog = plugin.getDailyQuestManager().getProgress(player, id);
            int target = plugin.getDailyQuestManager().getQuestTarget(id);
            String name = plugin.getDailyQuestManager().getQuestName(id);
            String status = done ? "&aDONE" : ("&e" + prog + "/" + target);
            sender.sendMessage(plugin.getConfigManager().color("&7- " + name + " &8[" + status + "&8]"));
        }
    }

    private void handleBalance(CommandSender sender, String[] args) {
        Player target;
        if (args.length >= 2) {
            if (!sender.hasPermission("slumber.tickets.see.others")) {
                sender.sendMessage(plugin.getConfigManager().getMessage("no-permission")); return;
            }
            target = Bukkit.getPlayer(args[1]);
            if (target == null) { sender.sendMessage(plugin.getConfigManager().getMessage("player-not-found")); return; }
            sender.sendMessage(plugin.getConfigManager().getMessage("tickets-balance-other")
                    .replace("{player}", target.getName())
                    .replace("{amount}", String.valueOf(plugin.getTicketManager().getTickets(target)))
                    .replace("{name}", plugin.getConfigManager().getTicketNamePlural()));
        } else {
            if (!(sender instanceof Player)) return;
            target = (Player) sender;
            sender.sendMessage(plugin.getConfigManager().getMessage("tickets-balance")
                    .replace("{amount}", String.valueOf(plugin.getTicketManager().getTickets(target)))
                    .replace("{name}", plugin.getConfigManager().getTicketNamePlural()));
        }
    }

    private void handleWallet(CommandSender sender) {
        if (!(sender instanceof Player)) return;
        Player player = (Player) sender;
        sender.sendMessage(plugin.getConfigManager().color(
                "&7Wallet: &e" + plugin.getTicketManager().getTickets(player)
                        + "&7/&e" + plugin.getWalletManager().getCurrentCapacity(player)
                        + " &8(Tier " + plugin.getWalletManager().getCurrentTier(player) + ")"));
    }

    private void handleGiveTickets(CommandSender sender, String[] args) {
        if (args.length < 3) { sender.sendMessage("/slumber give <player> <amount>"); return; }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) { sender.sendMessage("Player not found"); return; }
        int amount = Integer.parseInt(args[2]);
        int given = plugin.getTicketManager().giveTickets(target, amount, "Admin");
        sender.sendMessage(plugin.getConfigManager().color("&aGave &e" + given + " &ato &e" + target.getName()));
    }

    private void handleSetWallet(CommandSender sender, String[] args) {
        if (args.length < 3) { sender.sendMessage("/slumber setwallet <player> <tier>"); return; }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) return;
        int tier = Integer.parseInt(args[2]);
        plugin.getWalletManager().setTier(target, tier);
        sender.sendMessage("Set tier " + tier);
        target.sendMessage(plugin.getConfigManager().getMessage("wallet-upgraded")
                .replace("{max}", String.valueOf(plugin.getWalletManager().getCurrentCapacity(target))));
    }

    private void handleGiveItem(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("/slumber giveitem <player> <item> [amt]");
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) return;
        int amount = args.length >= 4 ? Integer.parseInt(args[3]) : 1;
        plugin.getSpecialItemManager().giveItem(target, args[2].toLowerCase(), amount);
        sender.sendMessage("Gave item");
    }

    private void handleInfo(CommandSender sender) {
        sender.sendMessage(plugin.getConfigManager().color("&8&m----------&r &dSlumberHotelCore &8&m----------"));
        sender.sendMessage(plugin.getConfigManager().color("&7Version: &f" + plugin.getDescription().getVersion()));
        sender.sendMessage(plugin.getConfigManager().color("&7Quests loaded: &f" + plugin.getQuestManager().getAll().size()));
        sender.sendMessage(plugin.getConfigManager().color("&7Doors: &f" + plugin.getDoorManager().getIds().size()));
        sender.sendMessage(plugin.getConfigManager().color("&7Storage: &f" +
                (plugin.getDatabaseManager().isEnabled() ? "MySQL" : "YAML")));
        sender.sendMessage(plugin.getConfigManager().color("&7BedWars: &f" +
                (plugin.getBedWars2023Hook() != null && plugin.getBedWars2023Hook().isHooked())));
    }

    private void handleCosmetics(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) return;
        Player player = (Player) sender;
        if (!plugin.getConfig().getBoolean("cosmetics-shop.enabled", true)) {
            sender.sendMessage(plugin.getConfigManager().color("&cCosmetics shop disabled."));
            return;
        }
        org.bukkit.configuration.ConfigurationSection root = plugin.getConfig().getConfigurationSection("cosmetics-shop.entries");
        if (root == null || root.getKeys(false).isEmpty()) {
            sender.sendMessage(plugin.getConfigManager().color("&7No cosmetics configured. Edit cosmetics-shop in config.yml"));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(plugin.getConfigManager().color("&8&m----------&r &dCosmetics &8&m----------"));
            for (String id : root.getKeys(false)) {
                org.bukkit.configuration.ConfigurationSection e = root.getConfigurationSection(id);
                if (e == null) continue;
                sender.sendMessage(plugin.getConfigManager().color(
                        "&e" + id + " &7- " + e.getString("name", id) + " &8(" + e.getInt("cost") + " tickets)"));
            }
            sender.sendMessage(plugin.getConfigManager().color("&e/slumber cosmetics buy <id>"));
            return;
        }
        if (!args[1].equalsIgnoreCase("buy") || args.length < 3) {
            sender.sendMessage(plugin.getConfigManager().color("&cUsage: /slumber cosmetics buy <id>"));
            return;
        }
        String id = args[2].toLowerCase();
        org.bukkit.configuration.ConfigurationSection e = root.getConfigurationSection(id);
        if (e == null) {
            sender.sendMessage(plugin.getConfigManager().color("&cUnknown cosmetic."));
            return;
        }
        int cost = e.getInt("cost", 0);
        if (plugin.getTicketManager().getTickets(player) < cost) {
            sender.sendMessage(plugin.getConfigManager().color("&cNeed &e" + cost + " &ctickets."));
            return;
        }
        plugin.getTicketManager().takeTickets(player, cost);
        for (String cmd : e.getStringList("commands")) {
            org.bukkit.Bukkit.dispatchCommand(org.bukkit.Bukkit.getConsoleSender(),
                    cmd.replace("%player%", player.getName()));
        }
        sender.sendMessage(plugin.getConfigManager().color("&aPurchased &e" + e.getString("name", id)));
    }

}
