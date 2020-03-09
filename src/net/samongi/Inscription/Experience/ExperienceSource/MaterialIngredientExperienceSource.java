package net.samongi.Inscription.Experience.ExperienceSource;

import net.samongi.Inscription.Experience.ExperienceReward;
import net.samongi.Inscription.Inscription;
import net.samongi.SamongiLib.Crafting.ItemCraftedEvent;
import net.samongi.SamongiLib.Items.ItemUtil;
import net.samongi.SamongiLib.Recipes.RecipeGraph;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;

import java.util.*;

public class MaterialIngredientExperienceSource implements ExperienceSource {

    // ---------------------------------------------------------------------------------------------------------------//
    public static final String CONFIG_KEY = "material-ingredient";

    // ---------------------------------------------------------------------------------------------------------------//
    private Map<Material, ExperienceReward> m_experiencePerIngredient = new HashMap<>();
    private Set<Material> m_recipeCycles = new HashSet<>();

    // ---------------------------------------------------------------------------------------------------------------//
    public MaterialIngredientExperienceSource()  {
        calculateRecipeCycles();
    }

    private void calculateRecipeCycles() {
        Inscription.logger.fine("Calculating Recipe Cycles");
        RecipeGraph graph = new RecipeGraph();
        Iterator<Recipe> recipeIterator = Bukkit.recipeIterator();
        while (recipeIterator.hasNext()) {
            Recipe recipe = recipeIterator.next();
            graph.addRecipe(recipe);
        }

        List<Material> cycles = graph.getCycles();
        for (Material material : cycles) {
            Inscription.logger.fine(" - " + material);
        }
        m_recipeCycles.addAll(cycles);
    }

    // ---------------------------------------------------------------------------------------------------------------//
    public void setExpPerIngredient(Material material, ExperienceReward reward) {
        m_experiencePerIngredient.put(material, reward);
    }
    public ExperienceReward getExpPerIngredient(Material material) {
        return m_experiencePerIngredient.get(material);
    }

    // ---------------------------------------------------------------------------------------------------------------//
    @EventHandler public void onItemCraftedEvent(ItemCraftedEvent event) {
        CraftItemEvent triggerEvent = event.getBaseEvent();
        int totalCrafts = event.getAmountCrafted();

        Player player = event.getPlayer();
        Recipe recipe = event.getRecipe();
        ItemStack result = recipe.getResult();
        Material resultMaterial = recipe.getResult().getType();

        // Calculating the ingredients for material-ingredient experience.
        ItemStack[] craftingMatrix = event.getMatrix();
        List<Material> ingredients = new ArrayList<>();

        // We aren't going to reward experience if the result is not cycling result.
        if (!m_recipeCycles.contains(resultMaterial)) {
            for (ItemStack ingredientItem : craftingMatrix) {
                if (ingredientItem == null) {
                    continue;
                }
                ingredients.add(ingredientItem.getType());
            }
        }

        for (Material ingredient : ingredients) {
            ExperienceReward reward = getExpPerIngredient(ingredient);
            if (reward != null) {
                reward.reward(player, totalCrafts);
            }
        }
    }

    // ---------------------------------------------------------------------------------------------------------------//
    @Override public boolean parseRewards(ConfigurationSection section) {
        if (section == null) {
            return false;
        }

        Inscription.logger.fine("Parsing material ingredient experience rewards");

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
            setExpPerIngredient(material, reward);

            Inscription.logger.fine("  Parsed: " + key + " registered: " + material.toString());
        }

        return true;
    }

    // ---------------------------------------------------------------------------------------------------------------//
}
