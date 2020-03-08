package net.samongi.Inscription.Experience.ExperienceSource;

import net.samongi.Inscription.Experience.ExperienceReward;
import net.samongi.Inscription.Inscription;
import net.samongi.SamongiLib.Crafting.ItemCraftedEvent;
import net.samongi.SamongiLib.Items.ItemUtil;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;

import java.util.*;

public class MaterialCraftExperienceSource implements ExperienceSource {

    // ---------------------------------------------------------------------------------------------------------------//
    public static final String CONFIG_KEY = "material-craft";

    // ---------------------------------------------------------------------------------------------------------------//
    private Map<Material, ExperienceReward> m_experiencePerCraft = new HashMap<>();

    // ---------------------------------------------------------------------------------------------------------------//
    public void setExpPerCraft(Material material, ExperienceReward reward) {
        m_experiencePerCraft.put(material, reward);
    }
    public ExperienceReward getExpPerCraft(Material material) {
        return this.m_experiencePerCraft.get(material);
    }

    // ---------------------------------------------------------------------------------------------------------------//
    @EventHandler public void onItemCraftedEvent(ItemCraftedEvent event) {
        CraftItemEvent triggerEvent = event.getBaseEvent();
        int totalCrafts = event.getAmountCrafted();

        Player player = (Player) triggerEvent.getWhoClicked();
        Recipe recipe = triggerEvent.getRecipe();
        Material resultMaterial = recipe.getResult().getType();

        ExperienceReward craftReward = getExpPerCraft(resultMaterial);
        if (craftReward != null) {
            craftReward.reward(player, totalCrafts);
        }
    }

    // ---------------------------------------------------------------------------------------------------------------//
    @Override public boolean parseRewards(ConfigurationSection section) {
        if (section == null) {
            return false;
        }

        Inscription.logger.fine("Parsing material craft experience rewards");

        Set<String> blockDataKeys = section.getKeys(false);
        for (String key : blockDataKeys) {
            if (!section.isConfigurationSection(key)) {
                Inscription.logger.warning(String.format("'%s is not a configuration section'", key));
                continue;
            }
            Material material = ItemUtil.parseMaterialData(key);
            if (material == null) {
                Inscription.logger.warning("  " + key + " is not valid material data.");
                continue;
            }

            ConfigurationSection experienceRewardSection = section.getConfigurationSection(key);
            if (experienceRewardSection == null) {
                Inscription.logger.warning("  " + key + "'s configuration section is null.");
                continue;
            }

            ExperienceReward reward = ExperienceReward.parse(experienceRewardSection);
            setExpPerCraft(material, reward);

            Inscription.logger.fine("  Parsed: " + key + " registered: " + material.toString());
        }

        return true;
    }

    // ---------------------------------------------------------------------------------------------------------------//
}
