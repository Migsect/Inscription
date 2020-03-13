package net.samongi.Inscription.Experience;

import java.io.File;
import java.util.*;

import net.samongi.Inscription.Experience.ExperienceSource.ExperienceSource;
import net.samongi.Inscription.Inscription;
import net.samongi.Inscription.Player.PlayerData;
import net.samongi.SamongiLib.Configuration.ConfigFile;
import net.samongi.SamongiLib.Configuration.ConfigurationParsing;

import net.samongi.SamongiLib.Items.MaskedBlockData;
import net.samongi.SamongiLib.Recipes.RecipeGraph;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.craftbukkit.libs.jline.internal.Nullable;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scheduler.BukkitRunnable;

import javax.annotation.Nonnull;

public class ExperienceManager implements ConfigurationParsing, Listener {

    // ---------------------------------------------------------------------------------------------------------------//
    private Map<String, ExperienceSource> m_experienceSources = new HashMap<>();

    // ---------------------------------------------------------------------------------------------------------------//
    public ExperienceManager() {
    }

    // ---------------------------------------------------------------------------------------------------------------//
    public void registerExperienceSource(@Nonnull String configKey, @Nonnull ExperienceSource experienceSource) {
        m_experienceSources.put(configKey, experienceSource);
        Bukkit.getPluginManager().registerEvents(experienceSource, Inscription.getInstance());
    }

    // ---------------------------------------------------------------------------------------------------------------//
    @Override public boolean parseConfigFile(@Nonnull File file, @Nonnull ConfigFile config) {
        Inscription.logger.info("Parsing TypeClass Configurations in: '" + file.getAbsolutePath() + "'");
        FileConfiguration root = config.getConfig();

        boolean parsedSomething = false;
        for (String configKey : m_experienceSources.keySet()) {
            boolean result = m_experienceSources.get(configKey).parseRewards(root.getConfigurationSection(configKey));
            if (!result) {
                continue;
            }
            Inscription.logger.info(String.format(" - Registered: '%s'", configKey));
            parsedSomething = true;
        }

        if (!parsedSomething) {
            Inscription.logger.warning(String.format("Didn't find anything to parse in '%s'", file.getAbsolutePath()));
        }
        return parsedSomething;
    }

    // ---------------------------------------------------------------------------------------------------------------//
}
