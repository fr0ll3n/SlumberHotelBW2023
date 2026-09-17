package com.slumberhotel.core.managers;

import com.slumberhotel.core.SlumberHotelCore;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

/**
 * When stars cross configured thresholds, grant permanent ticket multiplier boons.
 */
public class StarMilestoneListener {

    private final SlumberHotelCore plugin;

    public StarMilestoneListener(SlumberHotelCore plugin) {
        this.plugin = plugin;
    }

    public void check(Player player, int newStars) {
        ConfigurationSection root = plugin.getConfig().getConfigurationSection("star-milestones");
        if (root == null) return;
        for (String key : root.getKeys(false)) {
            try {
                int need = Integer.parseInt(key);
                if (newStars < need) continue;
                String flag = "star_ms_" + need;
                if (plugin.getPlayerDataManager().isQuestCompleted(player.getUniqueId(), flag)) continue;
                double boon = root.getDouble(key + ".boon", root.getDouble(key));
                if (boon <= 0 && root.isConfigurationSection(key)) {
                    boon = root.getDouble(key + ".boon", 0);
                }
                // support both:
                // star-milestones:
                //   5: 0.05
                //   10:
                //     boon: 0.1
                if (!root.isConfigurationSection(key)) {
                    boon = root.getDouble(key, 0);
                }
                if (boon <= 0) continue;
                plugin.getBoonManager().addMultiplier(player, boon, "Star milestone " + need);
                plugin.getPlayerDataManager().completeQuest(player.getUniqueId(), flag);
                player.sendMessage(plugin.getConfigManager().color(
                        "&d★ Star milestone &e" + need + "&d! Ticket multiplier +" + boon));
            } catch (NumberFormatException ignored) {}
        }
    }
}
