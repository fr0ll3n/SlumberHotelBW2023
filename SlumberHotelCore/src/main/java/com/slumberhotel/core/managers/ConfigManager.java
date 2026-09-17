package com.slumberhotel.core.managers;

import com.slumberhotel.core.SlumberHotelCore;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;

public class ConfigManager {

    private final SlumberHotelCore plugin;
    private FileConfiguration config;

    public ConfigManager(SlumberHotelCore plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.reloadConfig();
        this.config = plugin.getConfig();
    }

    public String getMessage(String path) {
        String msg = config.getString("messages." + path, path);
        return color(config.getString("messages.prefix", "") + msg);
    }

    public String getRawMessage(String path) {
        return color(config.getString("messages." + path, path));
    }

    public String color(String text) {
        if (text == null) return "";
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    public int getTicketGain(String type) {
        return config.getInt("tickets.gains." + type, 0);
    }

    public double getGlobalMultiplier() {
        return config.getDouble("tickets.global-multiplier", 1.0);
    }

    public int getStartingWallet() {
        return config.getInt("wallet.starting-capacity", 25);
    }

    public List<Integer> getWalletTiers() {
        return config.getIntegerList("wallet.tiers");
    }

    public String getTicketName() {
        return config.getString("tickets.name", "Slumber Ticket");
    }

    public String getTicketNamePlural() {
        return config.getString("tickets.name-plural", "Slumber Tickets");
    }

    public boolean isDebug() {
        return config.getBoolean("debug", false);
    }

    public FileConfiguration getConfig() {
        return config;
    }
}
