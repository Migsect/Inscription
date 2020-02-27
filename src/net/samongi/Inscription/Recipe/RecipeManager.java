package net.samongi.Inscription.Recipe;

import net.samongi.Inscription.Glyphs.Glyph;
import net.samongi.Inscription.Glyphs.Types.GlyphElement;
import net.samongi.Inscription.Glyphs.Types.GlyphRarity;
import net.samongi.Inscription.Inscription;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;

public class RecipeManager implements Listener {

    private static NamespacedKey GLYPH_UPGRADE_KEY = new NamespacedKey(Inscription.getInstance(), "glyph_upgrade");
    private ItemStack getPlaceholderUpgradeItem() {
        ItemStack item = new ItemStack(Material.PAPER);

        ItemMeta itemMeta = item.getItemMeta();
        itemMeta.setDisplayName("Upgrade Glyph Placeholder");
        item.setItemMeta(itemMeta);

        return item;

    }

    public void registerRecipes() {
        ShapedRecipe recipe = new ShapedRecipe(GLYPH_UPGRADE_KEY, getPlaceholderUpgradeItem());
        recipe.shape(" P ", "PPP", " P ");
        recipe.setIngredient('P', Material.PAPER);
        Bukkit.addRecipe(recipe);
    }

    @EventHandler(priority = EventPriority.LOW) void onCraftItemEvent(CraftItemEvent event) {
        Recipe recipe = event.getRecipe();
        if (!(recipe instanceof ShapedRecipe)) {
            return;
        }
        ShapedRecipe shapedRecipe = (ShapedRecipe) recipe;
        if (!shapedRecipe.getKey().getKey().equals(GLYPH_UPGRADE_KEY.getKey())) {
            return;
        }

        ItemStack result = event.getInventory().getResult();
        if (result == null || result.getType().equals(Material.AIR)) {
            event.setCancelled(true);
        }

    }

    private Glyph getEnhancedGlyph(Glyph target, Glyph[] componentGlyphs) {
        GlyphRarity targetRarity = target.getRarity();
        GlyphRarity nextRarity = Inscription.getInstance().getGlyphTypesManager().getRarityByRank(targetRarity.getRank() + 1);
        if (nextRarity == null) {
            Inscription.logger.finest("Next Rarity did not exist: " + targetRarity.getRank());
            return null;
        }

        Map<String, Integer> summedExperience = new HashMap<>();
        for (Glyph glyph : componentGlyphs) {
            Map<String, Integer> totalExperience = glyph.getTotalExperience_LEGACY();
            for (String experienceType : totalExperience.keySet()) {
                int experience = totalExperience.get(experienceType) / 4;
                int experienceSum = summedExperience.getOrDefault(experienceType, 0) + experience;
                summedExperience.put(experienceType, experienceSum);
                Inscription.logger.finest("Added experience for " + experienceType + " " + experience + " total: " + experienceSum);
            }
        }

        Glyph nextGlyph = target.clone();
        nextGlyph.setRarity(nextRarity);
        nextGlyph.setLevel_LEGACY(1);
        nextGlyph.setExperience_LEGACY(new HashMap<>());
        Inscription.logger.finest("Glyph Level: " + nextGlyph.getLevel_LEGACY());
        for (String experienceType : nextGlyph.getRelevantExperienceTypes()) {

            Inscription.logger.finest(" - " + experienceType + " : " + nextGlyph.getTotalExperience_LEGACY().get(experienceType));
        }
        nextGlyph.addExperience_LEGACY(summedExperience);
        nextGlyph.addExperience_LEGACY(target.getTotalExperience_LEGACY());
        Inscription.logger.finest("Glyph Level: " + nextGlyph.getLevel_LEGACY());
        while (nextGlyph.attemptLevelup_LEGACY()) {
            // Do nothing
        }
        Inscription.logger.finest("Glyph Level: " + nextGlyph.getLevel_LEGACY());
        return nextGlyph;
    }

    @EventHandler void onPrepareItemCraftEvent(PrepareItemCraftEvent event) {
        Recipe recipe = event.getRecipe();
        if (!(recipe instanceof ShapedRecipe)) {
            return;
        }
        ShapedRecipe shapedRecipe = (ShapedRecipe) recipe;
        if (!shapedRecipe.getKey().equals(GLYPH_UPGRADE_KEY)) {
            return;
        }

        Inscription.logger.finest("Saw glyph_upgrade Recipe");
        CraftingInventory inventory = event.getInventory();
        ItemStack[] items = inventory.getMatrix();

        ItemStack targetItem = items[4];
        if (!targetItem.hasItemMeta()) {
            inventory.setResult(new ItemStack(Material.AIR));
            return;
        }

        ItemStack[] componentItems = new ItemStack[]{items[1], items[3], items[5], items[7]};
        for (ItemStack item : componentItems) {
            if (!item.hasItemMeta()) {
                Inscription.logger.finest("Component didn't have item meta");
                inventory.setResult(new ItemStack(Material.AIR));
                return;
            }
        }

        Glyph targetGlyph = Glyph.getGlyph(targetItem);
        if (targetGlyph == null) {
            Inscription.logger.finest("Target item was not a glyph");
            inventory.setResult(new ItemStack(Material.AIR));
            return;
        }
        GlyphElement targetElement = targetGlyph.getElement();
        GlyphRarity targetRarity = targetGlyph.getRarity();

        Glyph[] componentGlyphs = new Glyph[4];
        for (int index = 0; index < componentItems.length; index++) {
            Glyph glyph = Glyph.getGlyph(componentItems[index]);
            if (glyph == null || !glyph.getElement().equals(targetElement) || !glyph.getRarity().equals(targetRarity)) {
                inventory.setResult(new ItemStack(Material.AIR));
                return;
            }
            componentGlyphs[index] = glyph;
        }

        Glyph enhancedGlyph = getEnhancedGlyph(targetGlyph, componentGlyphs);
        if (enhancedGlyph == null) {
            inventory.setResult(new ItemStack(Material.AIR));
            return;
        }
        inventory.setResult(enhancedGlyph.getItemStack());
    }
}
