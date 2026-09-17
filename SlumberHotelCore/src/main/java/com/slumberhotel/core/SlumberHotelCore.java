package com.slumberhotel.core;

import com.slumberhotel.core.commands.SlumberCommand;
import com.slumberhotel.core.doors.DoorManager;
import com.slumberhotel.core.hooks.BedWars2023Hook;
import com.slumberhotel.core.hooks.PlayerPointsHook;
import com.slumberhotel.core.listeners.PlayerListener;
import com.slumberhotel.core.listeners.ProjectileListener;
import com.slumberhotel.core.managers.ConfigManager;
import com.slumberhotel.core.managers.DailyQuestManager;
import com.slumberhotel.core.managers.DatabaseManager;
import com.slumberhotel.core.managers.PlayerDataManager;
import com.slumberhotel.core.managers.SpecialItemManager;
import com.slumberhotel.core.managers.TicketManager;
import com.slumberhotel.core.managers.WalletManager;
import com.slumberhotel.core.npc.NpcListener;
import com.slumberhotel.core.npc.NpcVisibilityManager;
import com.slumberhotel.core.managers.StarMilestoneListener;
import com.slumberhotel.core.placeholders.SlumberExpansion;
import com.slumberhotel.core.quests.QuestManager;
import com.slumberhotel.core.quests.QuestLogGUI;
import com.slumberhotel.core.quests.HotelGuideGUI;
import com.slumberhotel.core.quests.SlumberInventoryGUI;
import com.slumberhotel.core.managers.StarsManager;
import com.slumberhotel.core.managers.BoonManager;
import com.slumberhotel.core.managers.TicketMachineManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class SlumberHotelCore extends JavaPlugin {

    private static SlumberHotelCore instance;

    private ConfigManager configManager;
    private DatabaseManager databaseManager;
    private PlayerDataManager playerDataManager;
    private TicketManager ticketManager;
    private WalletManager walletManager;
    private SpecialItemManager specialItemManager;
    private DailyQuestManager dailyQuestManager;
    private QuestManager questManager;
    private QuestLogGUI questLogGUI;
    private HotelGuideGUI hotelGuideGUI;
    private SlumberInventoryGUI slumberInventoryGUI;
    private DoorManager doorManager;
    private StarsManager starsManager;
    private BoonManager boonManager;
    private TicketMachineManager ticketMachineManager;
    private NpcVisibilityManager npcVisibilityManager;
    private StarMilestoneListener starMilestoneListener;

    private PlayerPointsHook playerPointsHook;
    private BedWars2023Hook bedWars2023Hook;

    private boolean isLobby = false;
    private boolean isArena = false;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        configManager = new ConfigManager(this);
        configManager.load();

        databaseManager = new DatabaseManager(this);
        databaseManager.setup();

        playerDataManager = new PlayerDataManager(this);
        playerDataManager.load();

        walletManager = new WalletManager(this);
        ticketManager = new TicketManager(this);
        specialItemManager = new SpecialItemManager(this);
        dailyQuestManager = new DailyQuestManager(this);
        questManager = new QuestManager(this);
        questLogGUI = new QuestLogGUI(this);
        hotelGuideGUI = new HotelGuideGUI(this);
        slumberInventoryGUI = new SlumberInventoryGUI(this);
        doorManager = new DoorManager(this);
        starsManager = new StarsManager(this);
        boonManager = new BoonManager(this);
        ticketMachineManager = new TicketMachineManager(this);
        npcVisibilityManager = new NpcVisibilityManager(this);
        starMilestoneListener = new StarMilestoneListener(this);

        setupHooks();
        detectServerType();

        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new ProjectileListener(this), this);
        getServer().getPluginManager().registerEvents(new NpcListener(this), this);
        getServer().getPluginManager().registerEvents(npcVisibilityManager, this);
        getServer().getPluginManager().registerEvents(questLogGUI, this);
        getServer().getPluginManager().registerEvents(hotelGuideGUI, this);
        getServer().getPluginManager().registerEvents(slumberInventoryGUI, this);

        SlumberCommand cmd = new SlumberCommand(this);
        getCommand("slumber").setExecutor(cmd);
        getCommand("slumberadmin").setExecutor(cmd);

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null
                && getConfig().getBoolean("integrations.placeholderapi", true)) {
            new SlumberExpansion(this).register();
        }

        getLogger().info("========================================");
        getLogger().info("  SlumberHotelCore v" + getDescription().getVersion());
        getLogger().info("  Storage: " + (databaseManager.isEnabled() ? "MySQL (HikariCP)" : "YAML"));
        getLogger().info("  Quests: " + questManager.getAll().size() + " built-in (no BetonQuest)");
        getLogger().info("  Doors: " + doorManager.getIds().size());
        getLogger().info("  Ticket Machine: " + ticketMachineManager.isEnabled());
        getLogger().info("  Server: " + (isLobby ? "LOBBY" : (isArena ? "ARENA" : "UNKNOWN")));
        getLogger().info("  PlayerPoints: " + (playerPointsHook != null && playerPointsHook.isEnabled()));
        getLogger().info("  BedWars2023: " + (bedWars2023Hook != null && bedWars2023Hook.isHooked()));
        getLogger().info("========================================");
    }

    @Override
    public void onDisable() {
        if (playerDataManager != null) playerDataManager.saveAll();
        if (databaseManager != null) databaseManager.shutdown();
        getLogger().info("SlumberHotelCore disabled.");
    }

    private void setupHooks() {
        if (getConfig().getBoolean("integrations.playerpoints", true)
                && Bukkit.getPluginManager().getPlugin("PlayerPoints") != null) {
            playerPointsHook = new PlayerPointsHook(this);
            if (playerPointsHook.setup()) getLogger().info("Hooked PlayerPoints.");
            else { getLogger().warning("PlayerPoints hook failed!"); playerPointsHook = null; }
        }
        if (getConfig().getBoolean("integrations.bedwars2023", true)
                && (Bukkit.getPluginManager().getPlugin("BedWars2023") != null
                || Bukkit.getPluginManager().getPlugin("BedWars") != null)) {
            bedWars2023Hook = new BedWars2023Hook(this);
            bedWars2023Hook.hook();
        }
    }

    private void detectServerType() {
        String type = getConfig().getString("server-type", "AUTO").toUpperCase();
        if ("LOBBY".equals(type)) { isLobby = true; return; }
        if ("ARENA".equals(type)) { isArena = true; return; }
        if (Bukkit.getPluginManager().getPlugin("BWProxy2023") != null
                || Bukkit.getPluginManager().getPlugin("BedWarsProxy") != null
                || Bukkit.getPluginManager().getPlugin("proxy-plugin") != null) {
            isLobby = true;
        } else if (Bukkit.getPluginManager().getPlugin("BedWars2023") != null
                || Bukkit.getPluginManager().getPlugin("BedWars") != null) {
            isArena = true;
        }
    }

    public void reload() {
        reloadConfig();
        configManager.load();
        specialItemManager.loadItems();
        questManager.reload();
        doorManager.reload();
        playerDataManager.saveAll();
        getLogger().info("Reloaded.");
    }

    public static SlumberHotelCore getInstance() { return instance; }
    public ConfigManager getConfigManager() { return configManager; }
    public DatabaseManager getDatabaseManager() { return databaseManager; }
    public PlayerDataManager getPlayerDataManager() { return playerDataManager; }
    public TicketManager getTicketManager() { return ticketManager; }
    public WalletManager getWalletManager() { return walletManager; }
    public SpecialItemManager getSpecialItemManager() { return specialItemManager; }
    public DailyQuestManager getDailyQuestManager() { return dailyQuestManager; }
    public QuestManager getQuestManager() { return questManager; }
    public QuestLogGUI getQuestLogGUI() { return questLogGUI; }
    public HotelGuideGUI getHotelGuideGUI() { return hotelGuideGUI; }
    public SlumberInventoryGUI getSlumberInventoryGUI() { return slumberInventoryGUI; }
    public DoorManager getDoorManager() { return doorManager; }
    public StarsManager getStarsManager() { return starsManager; }
    public BoonManager getBoonManager() { return boonManager; }
    public NpcVisibilityManager getNpcVisibilityManager() { return npcVisibilityManager; }
    public StarMilestoneListener getStarMilestoneListener() { return starMilestoneListener; }
    public TicketMachineManager getTicketMachineManager() { return ticketMachineManager; }
    public PlayerPointsHook getPlayerPointsHook() { return playerPointsHook; }
    public BedWars2023Hook getBedWars2023Hook() { return bedWars2023Hook; }
    public boolean isLobby() { return isLobby; }
    public boolean isArena() { return isArena; }
}
