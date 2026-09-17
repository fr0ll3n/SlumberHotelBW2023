package com.slumberhotel.core.managers;

import com.slumberhotel.core.SlumberHotelCore;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.*;

public class DailyQuestManager {

    private final SlumberHotelCore plugin;
    private final Map<UUID, Map<String, Integer>> progressCache = new HashMap<UUID, Map<String, Integer>>();
    private final Map<UUID, List<String>> assignedCache = new HashMap<UUID, List<String>>();
    private final Map<UUID, Set<String>> completedCache = new HashMap<UUID, Set<String>>();

    public DailyQuestManager(SlumberHotelCore plugin) {
        this.plugin = plugin;
    }

    private String todayKey() {
        return new SimpleDateFormat("yyyy-MM-dd").format(new Date());
    }

    public void ensureDaily(Player player) {
        if (!plugin.getConfig().getBoolean("daily-quests.enabled", true)) return;
        UUID uuid = player.getUniqueId();
        PlayerDataManager pdm = plugin.getPlayerDataManager();

        String last = pdm.getDailyDate(uuid);
        String today = todayKey();
        if (!today.equals(last)) {
            pdm.setDailyDate(uuid, today);
            assignRandomQuests(player);
            progressCache.remove(uuid);
            assignedCache.remove(uuid);
            completedCache.remove(uuid);
            player.sendMessage(plugin.getConfigManager().getMessage("daily-reset"));
        } else {
            List<String> assigned = pdm.getDailyAssigned(uuid);
            if (assigned == null || assigned.isEmpty()) {
                assignRandomQuests(player);
            }
        }
    }

    private void assignRandomQuests(Player player) {
        ConfigurationSection pool = plugin.getConfig().getConfigurationSection("daily-quests.pool");
        if (pool == null) return;
        List<String> keys = new ArrayList<String>(pool.getKeys(false));
        Collections.shuffle(keys);
        int max = plugin.getConfig().getInt("daily-quests.max-active", 3);
        List<String> assigned = new ArrayList<String>();
        for (int i = 0; i < Math.min(max, keys.size()); i++) assigned.add(keys.get(i));

        UUID uuid = player.getUniqueId();
        plugin.getPlayerDataManager().setDailyAssigned(uuid, assigned);
        plugin.getPlayerDataManager().setDailyProgress(uuid, new HashMap<String, Integer>());
        plugin.getPlayerDataManager().setDailyCompleted(uuid, new ArrayList<String>());
        assignedCache.put(uuid, assigned);
        progressCache.put(uuid, new HashMap<String, Integer>());
        completedCache.put(uuid, new HashSet<String>());
    }

    public List<String> getAssigned(Player player) {
        ensureDaily(player);
        UUID uuid = player.getUniqueId();
        if (assignedCache.containsKey(uuid)) return assignedCache.get(uuid);
        List<String> list = plugin.getPlayerDataManager().getDailyAssigned(uuid);
        assignedCache.put(uuid, list);
        return list;
    }

    public int getProgress(Player player, String questId) {
        UUID uuid = player.getUniqueId();
        Map<String, Integer> map = progressCache.get(uuid);
        if (map == null) {
            map = plugin.getPlayerDataManager().getDailyProgress(uuid);
            progressCache.put(uuid, map);
        }
        Integer v = map.get(questId);
        return v == null ? 0 : v;
    }

    public boolean isCompleted(Player player, String questId) {
        UUID uuid = player.getUniqueId();
        Set<String> set = completedCache.get(uuid);
        if (set == null) {
            set = new HashSet<String>(plugin.getPlayerDataManager().getDailyCompleted(uuid));
            completedCache.put(uuid, set);
        }
        return set.contains(questId);
    }

    public void addProgress(Player player, String type, int amount) {
        if (!plugin.getConfig().getBoolean("daily-quests.enabled", true)) return;
        if (player == null || !player.isOnline()) return;
        ensureDaily(player);

        List<String> assigned = getAssigned(player);
        ConfigurationSection pool = plugin.getConfig().getConfigurationSection("daily-quests.pool");
        if (pool == null) return;

        for (String questId : assigned) {
            if (isCompleted(player, questId)) continue;
            ConfigurationSection q = pool.getConfigurationSection(questId);
            if (q == null) continue;
            if (!type.equalsIgnoreCase(q.getString("type", ""))) continue;

            int target = q.getInt("target", 1);
            int current = getProgress(player, questId) + amount;
            setProgress(player, questId, current);

            if (current >= target) {
                complete(player, questId, q);
            }
        }
    }

    private void setProgress(Player player, String questId, int value) {
        UUID uuid = player.getUniqueId();
        Map<String, Integer> map = progressCache.get(uuid);
        if (map == null) {
            map = plugin.getPlayerDataManager().getDailyProgress(uuid);
            progressCache.put(uuid, map);
        }
        map.put(questId, value);
        plugin.getPlayerDataManager().setDailyProgress(uuid, map);
    }

    private void complete(Player player, String questId, ConfigurationSection q) {
        UUID uuid = player.getUniqueId();
        Set<String> set = completedCache.get(uuid);
        if (set == null) {
            set = new HashSet<String>(plugin.getPlayerDataManager().getDailyCompleted(uuid));
            completedCache.put(uuid, set);
        }
        set.add(questId);
        plugin.getPlayerDataManager().setDailyCompleted(uuid, new ArrayList<String>(set));

        int reward = q.getInt("reward-tickets", 10);
        plugin.getTicketManager().giveTickets(player, reward, "Daily Quest");
        player.sendMessage(plugin.getConfigManager().getMessage("daily-complete")
                .replace("{name}", strip(q.getString("name", questId)))
                .replace("{reward}", String.valueOf(reward)));
    }

    private String strip(String s) {
        return s == null ? "" : s.replaceAll("&[0-9a-fk-or]", "");
    }

    public String getQuestName(String questId) {
        return plugin.getConfig().getString("daily-quests.pool." + questId + ".name", questId);
    }

    public String getQuestDescription(String questId) {
        return plugin.getConfig().getString("daily-quests.pool." + questId + ".description", "");
    }

    public int getQuestTarget(String questId) {
        return plugin.getConfig().getInt("daily-quests.pool." + questId + ".target", 1);
    }
}
