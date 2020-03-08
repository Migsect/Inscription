package net.samongi.Inscription.Experience.ExperienceSource;

import net.samongi.Inscription.Experience.ExperienceReward;
import net.samongi.Inscription.Inscription;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.projectiles.ProjectileSource;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class KillExperienceSource implements ExperienceSource {

    // ---------------------------------------------------------------------------------------------------------------//
    public static final String CONFIG_KEY = "entity-kill";

    // ---------------------------------------------------------------------------------------------------------------//
    private Map<EntityType, ExperienceReward> m_experiencePerKill = new HashMap<>();

    // ---------------------------------------------------------------------------------------------------------------//
    public void setExpPerKill(EntityType entityType, ExperienceReward reward) {
        m_experiencePerKill.put(entityType, reward);
    }
    public ExperienceReward getExpPerKill(EntityType entityType) {
        return this.m_experiencePerKill.get(entityType);
    }

    // ---------------------------------------------------------------------------------------------------------------//
    @EventHandler public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity killed = event.getEntity();
        EntityDamageEvent damageEvent = killed.getLastDamageCause();
        if (!(damageEvent instanceof EntityDamageByEntityEvent)) {
            return;
        }
        EntityDamageByEntityEvent entityDamageEvent = (EntityDamageByEntityEvent) damageEvent;

        Entity damaged = entityDamageEvent.getEntity();
        Entity damager = entityDamageEvent.getDamager();

        // If the damage is from the arrow, we'll promote the shooter to the damager.
        if (damager instanceof Arrow) {
            ProjectileSource shooter = ((Arrow) damager).getShooter();
            if (!(shooter instanceof Entity)) {
                return;
            }
            damager = (Entity) shooter;
        }

        EntityType damagedType = damaged.getType();
        ExperienceReward reward = this.getExpPerKill(damagedType);
        if (reward == null) {
            return;
        }

        if (reward.doDistributeArea()) {
            reward.reward(damaged.getLocation());
        } else if (damager instanceof Player) {
            reward.reward((Player) damager);
        }

    }

    // ---------------------------------------------------------------------------------------------------------------//
    @Override public boolean parseRewards(ConfigurationSection section) {
        if (section == null) {
            return false;
        }

        Inscription.logger.fine("Parsing entity kill experience rewards...");

        Set<String> entityDamageKeys = section.getKeys(false);
        for (String key : entityDamageKeys) {
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
            setExpPerKill(entityType, reward);

            Inscription.logger.fine("  Parsed: " + key + " registered: " + entityType.toString());
        }

        return true;
    }

    // ---------------------------------------------------------------------------------------------------------------//
}
