package net.samongi.Inscription.Experience.ExperienceSource;

import net.samongi.Inscription.Experience.ExperienceReward;
import net.samongi.Inscription.Inscription;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.projectiles.ProjectileSource;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class DamageDoneExperienceSource implements ExperienceSource {
    // ---------------------------------------------------------------------------------------------------------------//
    public static final String CONFIG_KEY = "entity-damage";

    // ---------------------------------------------------------------------------------------------------------------//
    private Map<EntityType, ExperienceReward> m_experiencePerDamage = new HashMap<>();

    // ---------------------------------------------------------------------------------------------------------------//
    public void setExpPerDamage(EntityType entityType, ExperienceReward reward) {
        m_experiencePerDamage.put(entityType, reward);
    }
    public ExperienceReward getExpPerDamage(EntityType entityType) {
        return this.m_experiencePerDamage.get(entityType);
    }

    // ---------------------------------------------------------------------------------------------------------------//
    @EventHandler public void onEntityDamageEntity(EntityDamageByEntityEvent event) {
        Entity damaged = event.getEntity();
        Entity damager = event.getDamager();

        // If the damage is from the arrow, we'll promote the shooter to the damager.
        if (damager instanceof Arrow) {
            ProjectileSource shooter = ((Arrow) damager).getShooter();
            if (!(shooter instanceof Entity)) {
                return;
            }
            damager = (Entity) shooter;
        }

        // We need to see if the entity is a player or arrow (arrow might be players)
        if (!(damager instanceof Player)) {
            return;
        }

        EntityType damagedType = damaged.getType(); // Getting the type of the damage entity.
        double damageDealt = event.getFinalDamage();

        ExperienceReward reward = this.getExpPerDamage(damagedType);
        if (reward == null) {
            return;
        }

        if (reward.doDistributeArea()) {
            reward.reward(damaged.getLocation(), damageDealt);
        } else if (damager instanceof Player) {
            reward.reward((Player) damager, damageDealt);
        }
    }

    // ---------------------------------------------------------------------------------------------------------------//
    @Override public boolean parseRewards(ConfigurationSection section) {
        if (section == null) {
            return false;
        }

        Inscription.logger.fine("Parsing entity damage experience rewards...");

        Set<String> entity_damage_keys = section.getKeys(false);
        for (String key : entity_damage_keys) {
            if (!section.isConfigurationSection(key)) {
                Inscription.logger.warning(String.format("'%s is not a configuration section'", key));
                continue;
            }

            EntityType entityType;
            try {
                // NOTE We should probably check to see if NamespacedKey works fine here.
                entityType = EntityType.valueOf(key);
            }
            catch (IllegalArgumentException e) {
                Inscription.logger.warning("  " + key + " is not a valid entity type.");
                continue;
            }

            if (entityType == null) {
                Inscription.logger.warning("  " + key + " is not a valid entity type.");
                continue;
            }

            ConfigurationSection experienceRewardSection = section.getConfigurationSection(key);
            if (experienceRewardSection == null) {
                Inscription.logger.warning("  " + key + "'s configuration section is null.");
                continue;
            }

            ExperienceReward reward = ExperienceReward.parse(experienceRewardSection);
            setExpPerDamage(entityType, reward);

            Inscription.logger.fine("  Parsed: " + key + " registered: " + entityType.toString());
        }

        return true;
    }

    // ---------------------------------------------------------------------------------------------------------------//
}
