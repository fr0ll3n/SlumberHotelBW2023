package com.slumberhotel.core.managers;

import com.slumberhotel.core.SlumberHotelCore;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Unified player data: uses MySQL (Hikari) when enabled, otherwise YAML files.
 */
public class PlayerDataManager {

    private final SlumberHotelCore plugin;
    private final File dataFolder;
    private final Map<UUID, FileConfiguration> yamlCache = new HashMap<UUID, FileConfiguration>();

    public PlayerDataManager(SlumberHotelCore plugin) {
        this.plugin = plugin;
        this.dataFolder = new File(plugin.getDataFolder(), "playerdata");
        if (!dataFolder.exists()) dataFolder.mkdirs();
    }

    public void load() {
        // nothing
    }

    private boolean useMysql() {
        return plugin.getDatabaseManager() != null && plugin.getDatabaseManager().isEnabled();
    }

    // ---------- Wallet ----------
    public int getWalletTier(Player player) {
        if (useMysql()) {
            return plugin.getDatabaseManager().loadPlayer(player.getUniqueId()).walletTier;
        }
        return getYaml(player.getUniqueId()).getInt("wallet-tier", 0);
    }

    public void setWalletTier(Player player, int tier) {
        if (useMysql()) {
            DatabaseManager.PlayerRow row = plugin.getDatabaseManager().loadPlayer(player.getUniqueId());
            row.walletTier = tier;
            row.dirty = true;
            plugin.getDatabaseManager().savePlayer(player.getUniqueId());
            return;
        }
        FileConfiguration conf = getYaml(player.getUniqueId());
        conf.set("wallet-tier", tier);
        saveYaml(player.getUniqueId());
    }

    // ---------- Entered hotel ----------
    public boolean hasEnteredHotel(Player player) {
        if (useMysql()) {
            return plugin.getDatabaseManager().loadPlayer(player.getUniqueId()).enteredHotel;
        }
        return getYaml(player.getUniqueId()).getBoolean("entered-hotel", false);
    }

    public void setEnteredHotel(Player player, boolean value) {
        if (useMysql()) {
            DatabaseManager.PlayerRow row = plugin.getDatabaseManager().loadPlayer(player.getUniqueId());
            row.enteredHotel = value;
            row.dirty = true;
            plugin.getDatabaseManager().savePlayer(player.getUniqueId());
            return;
        }
        getYaml(player.getUniqueId()).set("entered-hotel", value);
        saveYaml(player.getUniqueId());
    }

    // ---------- Daily quests (string fields for MySQL, section for YAML) ----------
    public String getDailyDate(UUID uuid) {
        if (useMysql()) return plugin.getDatabaseManager().loadPlayer(uuid).dailyDate;
        return getYaml(uuid).getString("daily.date", "");
    }

    public void setDailyDate(UUID uuid, String date) {
        if (useMysql()) {
            DatabaseManager.PlayerRow row = plugin.getDatabaseManager().loadPlayer(uuid);
            row.dailyDate = date;
            plugin.getDatabaseManager().savePlayer(uuid);
            return;
        }
        getYaml(uuid).set("daily.date", date);
        saveYaml(uuid);
    }

    public List<String> getDailyAssigned(UUID uuid) {
        if (useMysql()) {
            return DatabaseManager.parseList(plugin.getDatabaseManager().loadPlayer(uuid).dailyAssigned);
        }
        return getYaml(uuid).getStringList("daily.assigned");
    }

    public void setDailyAssigned(UUID uuid, List<String> list) {
        if (useMysql()) {
            DatabaseManager.PlayerRow row = plugin.getDatabaseManager().loadPlayer(uuid);
            row.dailyAssigned = DatabaseManager.serializeList(list);
            plugin.getDatabaseManager().savePlayer(uuid);
            return;
        }
        getYaml(uuid).set("daily.assigned", list);
        saveYaml(uuid);
    }

    public Map<String, Integer> getDailyProgress(UUID uuid) {
        if (useMysql()) {
            return DatabaseManager.parseProgress(plugin.getDatabaseManager().loadPlayer(uuid).dailyProgress);
        }
        Map<String, Integer> map = new HashMap<String, Integer>();
        FileConfiguration conf = getYaml(uuid);
        if (conf.isConfigurationSection("daily.progress")) {
            for (String key : conf.getConfigurationSection("daily.progress").getKeys(false)) {
                map.put(key, conf.getInt("daily.progress." + key, 0));
            }
        }
        return map;
    }

    public void setDailyProgress(UUID uuid, Map<String, Integer> map) {
        if (useMysql()) {
            DatabaseManager.PlayerRow row = plugin.getDatabaseManager().loadPlayer(uuid);
            row.dailyProgress = DatabaseManager.serializeProgress(map);
            plugin.getDatabaseManager().savePlayer(uuid);
            return;
        }
        FileConfiguration conf = getYaml(uuid);
        conf.set("daily.progress", null);
        if (map != null) {
            for (Map.Entry<String, Integer> e : map.entrySet()) {
                conf.set("daily.progress." + e.getKey(), e.getValue());
            }
        }
        saveYaml(uuid);
    }

    public List<String> getDailyCompleted(UUID uuid) {
        if (useMysql()) {
            return DatabaseManager.parseList(plugin.getDatabaseManager().loadPlayer(uuid).dailyCompleted);
        }
        return getYaml(uuid).getStringList("daily.completed");
    }

    public void setDailyCompleted(UUID uuid, List<String> list) {
        if (useMysql()) {
            DatabaseManager.PlayerRow row = plugin.getDatabaseManager().loadPlayer(uuid);
            row.dailyCompleted = DatabaseManager.serializeList(list);
            plugin.getDatabaseManager().savePlayer(uuid);
            return;
        }
        getYaml(uuid).set("daily.completed", list);
        saveYaml(uuid);
    }

    // ---------- YAML helpers ----------
    public FileConfiguration getData(UUID uuid) {
        // Legacy API used by older code – for YAML mode
        return getYaml(uuid);
    }

    private FileConfiguration getYaml(UUID uuid) {
        if (yamlCache.containsKey(uuid)) return yamlCache.get(uuid);
        File file = new File(dataFolder, uuid.toString() + ".yml");
        FileConfiguration conf = YamlConfiguration.loadConfiguration(file);
        yamlCache.put(uuid, conf);
        return conf;
    }

    private void saveYaml(UUID uuid) {
        FileConfiguration conf = yamlCache.get(uuid);
        if (conf == null) return;
        try {
            conf.save(new File(dataFolder, uuid.toString() + ".yml"));
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save YAML for " + uuid);
        }
    }

    public void save(UUID uuid) {
        if (useMysql()) {
            plugin.getDatabaseManager().savePlayer(uuid);
        } else {
            saveYaml(uuid);
        }
    }

    public void saveAll() {
        if (useMysql()) {
            plugin.getDatabaseManager().saveAll();
        } else {
            for (UUID uuid : yamlCache.keySet()) saveYaml(uuid);
        }
    }

    public void unload(UUID uuid) {
        if (useMysql()) {
            plugin.getDatabaseManager().unload(uuid);
        } else {
            saveYaml(uuid);
            yamlCache.remove(uuid);
        }
    }

    
    // ---------- Games played (YAML) ----------
    public int getGamesPlayed(java.util.UUID uuid) {
        return getYaml(uuid).getInt("games-played", 0);
    }

    public void addGamePlayed(java.util.UUID uuid) {
        getYaml(uuid).set("games-played", getGamesPlayed(uuid) + 1);
        saveYaml(uuid);
    }

    // ---------- Quests (YAML – reliable on 1.8) ----------
    public boolean isQuestCompleted(java.util.UUID uuid, String questId) {
        return getCompletedQuests(uuid).contains(questId);
    }

    public boolean isQuestActive(java.util.UUID uuid, String questId) {
        return getActiveQuests(uuid).contains(questId);
    }

    public java.util.List<String> getActiveQuests(java.util.UUID uuid) {
        return getYaml(uuid).getStringList("quests.active");
    }

    public java.util.List<String> getCompletedQuests(java.util.UUID uuid) {
        return getYaml(uuid).getStringList("quests.completed");
    }

    public void addActiveQuest(java.util.UUID uuid, String questId) {
        java.util.List<String> list = new java.util.ArrayList<String>(getActiveQuests(uuid));
        if (!list.contains(questId)) list.add(questId);
        getYaml(uuid).set("quests.active", list);
        saveYaml(uuid);
    }

    public void completeQuest(java.util.UUID uuid, String questId) {
        java.util.List<String> active = new java.util.ArrayList<String>(getActiveQuests(uuid));
        active.remove(questId);
        getYaml(uuid).set("quests.active", active);
        java.util.List<String> done = new java.util.ArrayList<String>(getCompletedQuests(uuid));
        if (!done.contains(questId)) done.add(questId);
        getYaml(uuid).set("quests.completed", done);
        saveYaml(uuid);
    }

    // ---------- Doors (YAML) ----------
    public boolean isDoorUnlocked(java.util.UUID uuid, String doorId) {
        return getUnlockedDoors(uuid).contains(doorId);
    }

    public java.util.List<String> getUnlockedDoors(java.util.UUID uuid) {
        return getYaml(uuid).getStringList("doors.unlocked");
    }

    public void unlockDoor(java.util.UUID uuid, String doorId) {
        java.util.List<String> list = new java.util.ArrayList<String>(getUnlockedDoors(uuid));
        if (!list.contains(doorId)) list.add(doorId);
        getYaml(uuid).set("doors.unlocked", list);
        saveYaml(uuid);
    }

    // ---------- Stars ----------
    public int getStars(java.util.UUID uuid) {
        return getYaml(uuid).getInt("stars", 0);
    }

    public void setStars(java.util.UUID uuid, int amount) {
        getYaml(uuid).set("stars", Math.max(0, amount));
        saveYaml(uuid);
    }

    public double getTicketMultiplier(java.util.UUID uuid) {
        return getYaml(uuid).getDouble("ticket-multiplier", 1.0);
    }

    public void setTicketMultiplier(java.util.UUID uuid, double mult) {
        getYaml(uuid).set("ticket-multiplier", Math.max(1.0, mult));
        saveYaml(uuid);
    }
}
