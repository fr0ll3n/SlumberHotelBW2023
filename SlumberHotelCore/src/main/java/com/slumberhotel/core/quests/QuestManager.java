package com.slumberhotel.core.quests;

import com.slumberhotel.core.SlumberHotelCore;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Built-in quest system – no BetonQuest required (1.8.8 safe).
 */
public class QuestManager {

    private final SlumberHotelCore plugin;
    private final Map<String, QuestDefinition> quests = new LinkedHashMap<String, QuestDefinition>();

    public QuestManager(SlumberHotelCore plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        quests.clear();
        ConfigurationSection root = plugin.getConfig().getConfigurationSection("quests");
        if (root == null) return;
        for (String id : root.getKeys(false)) {
            ConfigurationSection s = root.getConfigurationSection(id);
            if (s == null) continue;
            QuestDefinition q = new QuestDefinition();
            q.id = id;
            q.name = s.getString("name", id);
            q.description = s.getString("description", "");
            q.npcId = s.getString("npc", "");
            q.requiresQuests = s.getStringList("requires-quests");
            q.requiresEnteredHotel = s.getBoolean("requires-entered", false);
            q.requiresWalletTier = s.getInt("requires-wallet-tier", -1);
            q.requiresGamesPlayed = s.getInt("requires-games", 0);
            q.objectiveType = s.getString("objective.type", "TALK").toUpperCase();
            q.itemKey = s.getString("objective.item", "");
            q.itemAmount = s.getInt("objective.amount", 1);
            q.ticketCost = s.getInt("objective.tickets", 0);
            q.rewardTickets = s.getInt("rewards.tickets", 0);
            q.rewardWalletTier = s.getInt("rewards.wallet-tier", -1);
            q.upgradeWallet = s.getBoolean("rewards.upgrade-wallet", false);
            q.rewardCommands = s.getStringList("rewards.commands");
            q.unlockDoors = s.getStringList("rewards.unlock-doors");
            q.startDialogue = s.getStringList("dialogue.start");
            q.progressDialogue = s.getStringList("dialogue.progress");
            q.completeDialogue = s.getStringList("dialogue.complete");
            q.autoStart = s.getBoolean("auto-start", false);
            q.repeatable = s.getBoolean("repeatable", false);
            q.rewardStars = s.getInt("rewards.stars", 0);
            q.requiresStars = s.getInt("requires-stars", 0);
            q.rewardBoon = s.getDouble("rewards.boon", 0);
            quests.put(id, q);
        }
        plugin.getLogger().info("Loaded " + quests.size() + " built-in quests.");
    }

    public Collection<QuestDefinition> getAll() { return quests.values(); }
    public QuestDefinition get(String id) { return quests.get(id); }

    public boolean isCompleted(Player player, String questId) {
        return plugin.getPlayerDataManager().isQuestCompleted(player.getUniqueId(), questId);
    }

    public boolean isActive(Player player, String questId) {
        return plugin.getPlayerDataManager().isQuestActive(player.getUniqueId(), questId);
    }

    public List<String> getActive(Player player) {
        return plugin.getPlayerDataManager().getActiveQuests(player.getUniqueId());
    }

    public List<String> getCompleted(Player player) {
        return plugin.getPlayerDataManager().getCompletedQuests(player.getUniqueId());
    }

    public boolean canStart(Player player, QuestDefinition q) {
        if (q == null) return false;
        if (isCompleted(player, q.id) && !q.repeatable) return false;
        if (isActive(player, q.id)) return false;
        if (q.requiresEnteredHotel && !plugin.getPlayerDataManager().hasEnteredHotel(player)) return false;
        if (q.requiresWalletTier >= 0 && plugin.getWalletManager().getCurrentTier(player) < q.requiresWalletTier) return false;
        if (q.requiresStars > 0 && plugin.getStarsManager().getStars(player) < q.requiresStars) return false;
        if (q.requiresGamesPlayed > 0) {
            int games = plugin.getPlayerDataManager().getGamesPlayed(player.getUniqueId());
            if (games < q.requiresGamesPlayed) return false;
        }
        for (String req : q.requiresQuests) {
            if (!isCompleted(player, req)) return false;
        }
        return true;
    }

    public void startQuest(Player player, String questId) {
        QuestDefinition q = get(questId);
        if (q == null || !canStart(player, q)) {
            if (q != null && !canStart(player, q)) {
                player.sendMessage(plugin.getConfigManager().color("&cYou cannot start this quest yet."));
                return;
            }
            return;
        }
        plugin.getPlayerDataManager().addActiveQuest(player.getUniqueId(), questId);
        sendDialogue(player, q.startDialogue);
        player.sendMessage(plugin.getConfigManager().color("&aQuest started: &e" + strip(q.name)));
        player.sendMessage(plugin.getConfigManager().color("&7" + strip(q.description)));
    }

    public void tryComplete(Player player, String questId) {
        QuestDefinition q = get(questId);
        if (q == null || !isActive(player, questId)) return;

        if ("COLLECT_ITEM".equals(q.objectiveType)) {
            int has = plugin.getSpecialItemManager().countItem(player, q.itemKey);
            if (has < q.itemAmount) {
                sendDialogue(player, q.progressDialogue);
                player.sendMessage(plugin.getConfigManager().color(
                        "&7Progress: &e" + has + "&7/&e" + q.itemAmount + " &7" + q.itemKey));
                return;
            }
            plugin.getSpecialItemManager().takeItem(player, q.itemKey, q.itemAmount);
        } else if ("PAY_TICKETS".equals(q.objectiveType)) {
            int tickets = plugin.getTicketManager().getTickets(player);
            if (tickets < q.ticketCost) {
                sendDialogue(player, q.progressDialogue);
                player.sendMessage(plugin.getConfigManager().color(
                        "&cNeed &e" + q.ticketCost + " &ctickets (you have &e" + tickets + "&c)."));
                return;
            }
            plugin.getTicketManager().takeTickets(player, q.ticketCost);
        }
        // TALK objectives complete on this call

        completeQuest(player, q);
    }

    public void completeQuest(Player player, QuestDefinition q) {
        plugin.getPlayerDataManager().completeQuest(player.getUniqueId(), q.id);
        if (plugin.getNpcVisibilityManager() != null) {
            plugin.getNpcVisibilityManager().refresh(player);
        }
        sendDialogue(player, q.completeDialogue);
        if (q.rewardTickets > 0) {
            plugin.getTicketManager().giveTickets(player, q.rewardTickets, "Quest: " + strip(q.name));
        }
        if (q.rewardStars > 0) {
            plugin.getStarsManager().addStars(player, q.rewardStars, strip(q.name));
        }
        if (q.rewardBoon > 0) {
            plugin.getBoonManager().addMultiplier(player, q.rewardBoon, strip(q.name));
        }
        if (q.upgradeWallet) {
            plugin.getWalletManager().upgradeWallet(player);
        }
        if (q.rewardWalletTier >= 0) {
            plugin.getWalletManager().setTier(player, q.rewardWalletTier);
            player.sendMessage(plugin.getConfigManager().getMessage("wallet-upgraded")
                    .replace("{max}", String.valueOf(plugin.getWalletManager().getCurrentCapacity(player))));
        }
        for (String door : q.unlockDoors) {
            plugin.getDoorManager().unlock(player, door);
        }
        for (String cmd : q.rewardCommands) {
            String c = cmd.replace("%player%", player.getName());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), c);
        }
        player.sendMessage(plugin.getConfigManager().color("&a&lQUEST COMPLETE: &e" + strip(q.name)));

        if ("enter_hotel".equals(q.id)) {
            plugin.getPlayerDataManager().setEnteredHotel(player, true);
            // Ensure mini wallet tier 0 capacity
            if (plugin.getWalletManager().getCurrentTier(player) < 0) {
                plugin.getWalletManager().setTier(player, 0);
            }
        }

        // Auto-start follow-ups
        for (QuestDefinition other : quests.values()) {
            if (other.autoStart && canStart(player, other)) {
                startQuest(player, other.id);
            }
        }
    }

    /** Handle NPC click – find quests for this NPC and interact. */
    public void handleNpcClick(Player player, String npcKey) {
        List<QuestDefinition> related = new ArrayList<QuestDefinition>();
        for (QuestDefinition q : quests.values()) {
            if (npcKey.equalsIgnoreCase(q.npcId)) related.add(q);
        }
        if (related.isEmpty()) {
            player.sendMessage(plugin.getConfigManager().color("&7They have nothing for you right now."));
            return;
        }

        // Prefer active quest to turn in
        for (QuestDefinition q : related) {
            if (isActive(player, q.id)) {
                tryComplete(player, q.id);
                return;
            }
        }
        // Start first available
        for (QuestDefinition q : related) {
            if (canStart(player, q)) {
                startQuest(player, q.id);
                // If TALK type with no items, complete immediately on second interaction flow:
                // For pure TALK, complete on same click after start if objective is TALK and no cost
                if ("TALK".equals(q.objectiveType) && q.ticketCost <= 0 && (q.itemKey == null || q.itemKey.isEmpty())) {
                    // leave active so player clicks again to complete, OR complete now for simple intros
                    if (plugin.getConfig().getBoolean("quests-settings.talk-complete-on-start", false)) {
                        completeQuest(player, q);
                    }
                }
                return;
            }
        }
        // Show locked reason
        for (QuestDefinition q : related) {
            if (isCompleted(player, q.id) && !q.repeatable) {
                player.sendMessage(plugin.getConfigManager().color("&7You've already helped them."));
                return;
            }
        }
        player.sendMessage(plugin.getConfigManager().color("&7Come back when you've progressed further."));
    }

    private void sendDialogue(Player player, List<String> lines) {
        if (lines == null) return;
        for (String line : lines) {
            player.sendMessage(plugin.getConfigManager().color(line));
        }
    }

    private String strip(String s) {
        return s == null ? "" : s.replaceAll("&[0-9a-fk-or]", "");
    }

    public java.util.Set<String> getAllIds() {
        return quests.keySet();
    }
}
