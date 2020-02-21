package net.samongi.Inscription.Experience;

import net.samongi.Inscription.Inscription;
import net.samongi.Inscription.Player.GlyphInventory;
import net.samongi.Inscription.Player.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Represents a reward from experience.
 * Wraps a map but also stores other flags to indicate how experience is handled.
 */
public class ExperienceReward {

    private Map<String, Integer> m_experienceRewards = new HashMap<>();

    // Distributes the experience of an area around a location that it occured.
    // Used for reward(Location).
    private int m_distributeRadius = 0;
    // Will split the area distribution equal across the players.
    private boolean m_splitDistribution = false;

    // ---------------------------------------------------------------------------------------------------------------//
    public static ExperienceReward parse(ConfigurationSection section) {
        ExperienceReward reward = new ExperienceReward();

        ConfigurationSection experienceTypeSection = section.getConfigurationSection("experience");

        // If the section doesn't have the experience keyed section, then the section is acting as that section.
        // (This is a legacy migration for some configs)
        if (experienceTypeSection == null) {
            experienceTypeSection = section;
        } else {
            reward.m_distributeRadius = section.getInt("distribute-radius", 0);
            reward.m_splitDistribution = section.getBoolean("split-distribution", false);
        }

        Set<String> experienceTypeSectionKeys = experienceTypeSection.getKeys(false);
        for (String experienceType : experienceTypeSectionKeys) {
            reward.m_experienceRewards.put(experienceType, experienceTypeSection.getInt(experienceType, 0));
        }

        return reward;
    }

    // ---------------------------------------------------------------------------------------------------------------//
    private ExperienceReward() {
    }

    // ---------------------------------------------------------------------------------------------------------------//

    public Map<String, Integer> getScaledExperienceRewards(double scalar) {
        Map<String, Integer> scaledRewards = new HashMap<>();

        for (String key : m_experienceRewards.keySet()) {
            int experience = (int) (m_experienceRewards.get(key) * scalar);
            scaledRewards.put(key, experience);
        }

        return scaledRewards;
    }

    public boolean doDistributeArea() {
        return m_distributeRadius > 0;
    }

    public boolean doSplitDistribution() {
        return m_splitDistribution;
    }

    // ---------------------------------------------------------------------------------------------------------------//
    public void reward(Player player, double scalar) {

        PlayerData data = Inscription.getInstance().getPlayerManager().getData((Player) player);
        if (data == null) {
            Inscription.logger.severe("Player data return null on call for: " + player.getName() + ":" + player.getUniqueId());
            return;
        }
        GlyphInventory glyphInventory = data.getGlyphInventory();

        Map<String, Integer> scaledRewards = getScaledExperienceRewards(scalar);
        for (String experienceType : scaledRewards.keySet()) {
            int experienceAmount = scaledRewards.get(experienceType);

            boolean experienceDistributed = glyphInventory.distributeExperience(experienceType, experienceAmount);

            // If the experience wasn't actually distributed, we need to trigger an event that the experience overflowed
            // back to the character profile.
            if (!experienceDistributed) {
                PlayerExperienceOverflowEvent overflowEvent = new PlayerExperienceOverflowEvent(player, experienceType, experienceAmount);
                Bukkit.getPluginManager().callEvent(overflowEvent);

                // The amount may have been changed during the event calls.
                data.addExperience(experienceType, overflowEvent.getAmount());
            }
        }
    }

    public void reward(Player player) {
        reward(player, 1);
    }

    public void reward(Location location, double scalar) {
        List<Player> players = new ArrayList<>();

        for (Entity entity : location.getWorld().getEntities()) {
            if ((entity instanceof Player) && entity.getLocation().distanceSquared(location) <= m_distributeRadius * m_distributeRadius) {
                players.add((Player) entity);
            }

        }

        for (Player player : players) {
            reward(player, m_splitDistribution ? scalar / players.size() : scalar);
        }
    }
    public void reward(Location location) {
        reward(location, 1);
    }

    // ---------------------------------------------------------------------------------------------------------------//
}
