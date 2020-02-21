package net.samongi.Inscription.Experience;

import java.io.File;
import java.util.*;

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

    private static final String ENTITY_DAMAGE_SECTION_KEY = "entity-damage";
    private static final String ENTITY_KILL_SECTION_KEY = "entity-kill";
    private static final String MATERIAL_BREAK_SECTION_KEY = "material-break";
    private static final String MATERIAL_CRAFT_SECTION_KEY = "material-craft";
    private static final String MATERIAL_INGREDIENT_SECTION_KEY = "material-ingredient";
    private static final String MATERIAL_PLACE_SECTION_KEY = "material-place";

    // Experience mapping for entity related events
    private Map<EntityType, ExperienceReward> m_experiencePerKill = new HashMap<>();
    private Map<EntityType, ExperienceReward> m_experiencePerDamage = new HashMap<>();

    //    BlockDataComparator m_comparator = new BlockDataGroupComparator(new BlockDataComparator[]{
    //        new BlockDataMaterialComparator(),
    //        new BlockDataAgeableComparator()
    //    });
    // Experience mapping for material related events
    private final static MaskedBlockData.Mask[] BLOCKDATA_MASKS = new MaskedBlockData.Mask[]{MaskedBlockData.Mask.MATERIAL, MaskedBlockData.Mask.AGEABLE};
    private Map<MaskedBlockData, ExperienceReward> m_experiencePerBreak = new HashMap<>();
    private Map<MaskedBlockData, ExperienceReward> m_experiencePerPlace = new HashMap<>();
    private Map<Material, ExperienceReward> m_experiencePerCraft = new HashMap<>();
    private Map<Material, ExperienceReward> m_experiencePerIngredient = new HashMap<>();

    private Set<Material> m_recipeCycles = new HashSet<>();

    // Tracks blocks that wouldn't be valid for experience because they may have been used to cheat.
    private BlockTracker tracker;

    // ---------------------------------------------------------------------------------------------------------------//
    public ExperienceManager() {
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

    private void handleExperience(@Nonnull Map<String, Integer> experienceMapping, @Nullable Player player) {

        PlayerData data = Inscription.getInstance().getPlayerManager().getData((Player) player);
        if (data == null) {
            Inscription.logger.severe("Player data return null on call for: " + player.getName() + ":" + player.getUniqueId());
            return;
        }

        for (String experienceType : experienceMapping.keySet()) {
            int experienceAmount = experienceMapping.get(experienceType);
            boolean experienceDistributed = data.getGlyphInventory().distributeExperience(experienceType, experienceAmount);

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

    // ---------------------------------------------------------------------------------------------------------------//
    /**
     * Method that encompasses logic to be handled by the experience manager
     * when another entity damages another. Generally for player experience
     * rewards.
     *
     * @param event
     */
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

    @EventHandler public void onBlockBreak(BlockBreakEvent event) {
        Location location = event.getBlock().getLocation();
        Material material = event.getBlock().getType();
        if (this.tracker.isTracked(material) && this.tracker.isPlaced(location)) {
            // This block break cannot trigger experience.
            return;
        }

        BlockData blockData = event.getBlock().getBlockData();
        Player player = event.getPlayer();

        ExperienceReward reward = this.getExpPerBreak(blockData);
        if (reward == null) {
            return;
        }

        reward.reward(player);
    }

    @EventHandler public void onBlockPlace(BlockPlaceEvent event) {
        BlockData blockData = event.getBlock().getBlockData();
        Player player = event.getPlayer();

        ExperienceReward reward = this.getExpPerPlace(blockData);
        if (reward == null) {
            return;
        }

        reward.reward(player);
    }

    @EventHandler public void onCraftItem(CraftItemEvent event) {
        if (event.isCancelled())
            return;

        Recipe recipe = event.getRecipe(); // getting the recipe
        ItemStack result = recipe.getResult(); // getting the result of the craft event

        Inscription.logger.fine("onCraftItem: Item Crafted Type: " + result.getType());
        Material resultMaterial = recipe.getResult().getType();

        // Calculating the ingredients for material-ingredient experience.
        ItemStack[] craftingMatrix = event.getInventory().getMatrix();
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

        // Grabbing the experience mapping that is the reward if there is none then we will just stop the event handling now.
        ExperienceReward craftReward = Inscription.getInstance().getExperienceManager().getExpPerCraft(resultMaterial);
        if (craftReward == null) {
            return;
        }

        // Note that this result will not be the total number of items that were crafted.
        Player player = (Player) event.getWhoClicked();
        ItemStack[] currentContents = player.getInventory().getContents();
        ItemStack[] priorContents = new ItemStack[currentContents.length];
        // We're doing a copy since we can't be sure if the current contents are immutable
        for (int i = 0; i < priorContents.length; i++) {
            if (currentContents[i] == null) {
                continue;
            }
            priorContents[i] = currentContents[i].clone();
        }

        // We are going to see how many times the item was actually crafted.
        if (event.isShiftClick()) {
            BukkitRunnable task = new BukkitRunnable() {

                @Override public void run() {
                    final ItemStack[] new_contents = player.getInventory().getContents();

                    // We are now going to count the number of items that the player has crafted.
                    int itemsCrafted = 0;
                    for (int i = 0; i < new_contents.length; i++) {
                        ItemStack newItem = new_contents[i];
                        ItemStack oldItem = priorContents[i];
                        if (newItem != oldItem) {
                            if (newItem.getType() != resultMaterial) {
                                continue;
                            }

                            Inscription.logger.finest("Found Inv Difference: " + newItem + " =/= " + oldItem);
                            if (oldItem == null || oldItem.getType().equals(Material.AIR)) {
                                // This occurs when the item is a new stack altogether.
                                itemsCrafted += newItem.getAmount();
                            } else {
                                // We are going to add the difference of the items.
                                itemsCrafted += newItem.getAmount() - oldItem.getAmount();
                            }
                        }
                    }
                    // The total times the item was crafted
                    int totalCrafts = itemsCrafted / result.getAmount();
                    Inscription.logger.fine("  items crafted: " + itemsCrafted + " result amount: " + result.getAmount() + " total crafts: " + totalCrafts);

                    PlayerData data = Inscription.getInstance().getPlayerManager().getData(player);
                    if (data == null) {
                        Inscription.logger.fine("ERROR: player data return null on call for: " + player.getName() + ":" + player.getUniqueId());
                        return;
                    }
                    craftReward.reward(player, totalCrafts);
                    for (Material ingredient : ingredients) {
                        ExperienceReward reward = getExpPerIngredient(ingredient);
                        if (reward != null) {
                            reward.reward(player, totalCrafts);
                        }
                    }
                }
            };
            task.runTask(Inscription.getInstance()); // Running the task
        } else {
            craftReward.reward(player);
            for (Material ingredient : ingredients) {
                ExperienceReward reward = getExpPerIngredient(ingredient);
                if (reward != null) {
                    reward.reward(player);
                }
            }
        }
    }
    @EventHandler public void onEnchantItem(EnchantItemEvent event) {
        // TODO
    }
    //    @EventHandler public void onAnvilRepair() {
    //    } // TODO Anvil events are most likely an inventory interact event

    // ---------------------------------------------------------------------------------------------------------------//
    public void setExpPerKill(EntityType entityType, ExperienceReward reward) {
        m_experiencePerKill.put(entityType, reward);
    }
    public ExperienceReward getExpPerKill(EntityType entityType) {
        return this.m_experiencePerKill.get(entityType);
    }

    // ---------------------------------------------------------------------------------------------------------------//
    public void setExpPerDamage(EntityType entityType, ExperienceReward reward) {
        m_experiencePerDamage.put(entityType, reward);
    }
    public ExperienceReward getExpPerDamage(EntityType entityType) {
        return this.m_experiencePerDamage.get(entityType);
    }
    // ---------------------------------------------------------------------------------------------------------------//

    public void setExpPerBreak(BlockData blockData, ExperienceReward reward) {
        MaskedBlockData key = new MaskedBlockData(blockData, BLOCKDATA_MASKS);
        m_experiencePerBreak.put(key, reward);
    }
    public ExperienceReward getExpPerBreak(BlockData blockData) {
        MaskedBlockData key = new MaskedBlockData(blockData, BLOCKDATA_MASKS);
        return m_experiencePerBreak.get(key);
    }

    // ---------------------------------------------------------------------------------------------------------------//

    public void setExpPerPlace(BlockData blockData, ExperienceReward reward) {
        MaskedBlockData key = new MaskedBlockData(blockData, BLOCKDATA_MASKS);
        m_experiencePerPlace.put(key, reward);
    }
    public ExperienceReward getExpPerPlace(BlockData blockData) {
        MaskedBlockData key = new MaskedBlockData(blockData, BLOCKDATA_MASKS);
        return m_experiencePerPlace.get(key);
    }

    // ---------------------------------------------------------------------------------------------------------------//

    public void setExpPerCraft(Material material, ExperienceReward reward) {
        m_experiencePerCraft.put(material, reward);
    }
    public ExperienceReward getExpPerCraft(Material material) {
        return this.m_experiencePerCraft.get(material);
    }
    // ---------------------------------------------------------------------------------------------------------------//

    public void setExpPerIngredient(Material material, ExperienceReward reward) {
        m_experiencePerIngredient.put(material, reward);
    }
    public ExperienceReward getExpPerIngredient(Material material) {
        return m_experiencePerIngredient.get(material);
    }
    // ---------------------------------------------------------------------------------------------------------------//

    public static BlockData parseBlockData(@Nonnull String parseable) {
        try {
            Material materialType = Material.valueOf(parseable);
            return Bukkit.createBlockData(materialType);
        }
        catch (IllegalArgumentException error) {
        }

        try {
            BlockData blockData = Bukkit.createBlockData(parseable);
            return blockData;
        }
        catch (IllegalArgumentException error) {
        }

        Inscription.logger.warning("Could not find type for: " + parseable);
        return null;
    }

    public static Material parseMaterialData(@Nonnull String parseable) {

        try {
            Material materialType = Material.valueOf(parseable);
            return materialType;
        }
        catch (IllegalArgumentException error) {
        }

        Inscription.logger.warning("Could not find type for: " + parseable);
        return null;
    }

    // ---------------------------------------------------------------------------------------------------------------//

    public boolean parseEntityDamageReward(@Nullable ConfigurationSection section) {
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

    public boolean parseEntityKillReward(@Nullable ConfigurationSection section) {
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

    public boolean parseMaterialBreakReward(@Nullable ConfigurationSection section) {
        if (section == null) {
            return false;
        }

        Inscription.logger.fine("Parsing material break experience rewards");

        Set<String> blockDataKeys = section.getKeys(false);
        for (String key : blockDataKeys) {
            if (!section.isConfigurationSection(key)) {
                Inscription.logger.warning(String.format("'%s is not a configuration section'", key));
                continue;
            }
            BlockData blockData = parseBlockData(key);
            if (blockData == null) {
                Inscription.logger.warning("  " + key + " is not valid material data.");
                continue;
            }

            ConfigurationSection experienceRewardSection = section.getConfigurationSection(key);
            if (experienceRewardSection == null) {
                Inscription.logger.warning("  " + key + "'s configuration section is null.");
                continue;
            }

            ExperienceReward reward = ExperienceReward.parse(experienceRewardSection);
            setExpPerBreak(blockData, reward);

            Inscription.logger.fine("  Parsed: " + key + " registered: " + blockData.getAsString(true));
        }

        return true;
    }

    public boolean parseMaterialCraft(@Nullable ConfigurationSection section) {
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
            Material material = parseMaterialData(key);
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

    public boolean parseMaterialIngredient(@Nullable ConfigurationSection section) {
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
            Material material = parseMaterialData(key);
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

    public boolean parseMaterialPlace(@Nullable ConfigurationSection section) {
        if (section == null) {
            return false;
        }

        Inscription.logger.fine("Parsing material place experience rewards");

        Set<String> blockDataKeys = section.getKeys(false);
        for (String key : blockDataKeys) {
            if (!section.isConfigurationSection(key)) {
                Inscription.logger.warning(String.format("'%s is not a configuration section'", key));
                continue;
            }
            BlockData blockData = parseBlockData(key);
            if (blockData == null) {
                Inscription.logger.warning("  " + key + " is not valid material data.");
                continue;
            }

            ConfigurationSection experienceTypeSection = section.getConfigurationSection(key);
            if (experienceTypeSection == null) {
                Inscription.logger.warning("  " + key + "'s configuration section is null.");
                continue;
            }

            ExperienceReward reward = ExperienceReward.parse(experienceTypeSection);
            setExpPerPlace(blockData, reward);

            Inscription.logger.fine("  Parsed: " + key + " registered: " + blockData.getAsString(true));
        }

        return true;
    }

    // ---------------------------------------------------------------------------------------------------------------//

    @Override public boolean parseConfigFile(@Nonnull File file, @Nonnull ConfigFile config) {
        Inscription.logger.info("Parsing TypeClass Configurations in: '" + file.getAbsolutePath() + "'");
        FileConfiguration root = config.getConfig();

        boolean parsedSomething = false;
        if (parseEntityDamageReward(root.getConfigurationSection(ENTITY_DAMAGE_SECTION_KEY))) {
            Inscription.logger.info(String.format(" - Registered: '%s'", ENTITY_DAMAGE_SECTION_KEY));
            parsedSomething = true;
        }
        if (parseEntityKillReward(root.getConfigurationSection(ENTITY_KILL_SECTION_KEY))) {
            Inscription.logger.info(String.format(" - Registered: '%s'", ENTITY_KILL_SECTION_KEY));
            parsedSomething = true;
        }
        if (parseMaterialBreakReward(root.getConfigurationSection(MATERIAL_BREAK_SECTION_KEY))) {
            Inscription.logger.info(String.format(" - Registered: '%s'", MATERIAL_BREAK_SECTION_KEY));
            parsedSomething = true;
        }
        if (parseMaterialCraft(root.getConfigurationSection(MATERIAL_CRAFT_SECTION_KEY))) {
            Inscription.logger.info(String.format(" - Registered: '%s'", MATERIAL_CRAFT_SECTION_KEY));
            parsedSomething = true;
        }
        if (parseMaterialIngredient(root.getConfigurationSection(MATERIAL_INGREDIENT_SECTION_KEY))) {
            Inscription.logger.info(String.format(" - Registered: '%s'", MATERIAL_INGREDIENT_SECTION_KEY));
            parsedSomething = true;
        }
        if (parseMaterialPlace(root.getConfigurationSection(MATERIAL_PLACE_SECTION_KEY))) {
            Inscription.logger.info(String.format(" - Registered: '%s'", MATERIAL_PLACE_SECTION_KEY));
            parsedSomething = true;
        }

        if (!parsedSomething) {
            Inscription.logger.warning(String.format("Didn't find anything to parse in '%s'", file.getAbsolutePath()));
        }
        return parsedSomething;

    }

    // ---------------------------------------------------------------------------------------------------------------//

    public BlockTracker getTracker() {
        return this.tracker;
    }
    public boolean hasTracker() {
        return this.tracker != null;
    }

    /**
     * Will configure the block tracker in the ExperienceManager using the
     * provided fileconfiguration
     *
     * @param config The file configuration to use.
     */
    public void configureTracker(FileConfiguration config) {
        List<String> tracked_s = config.getStringList("placement-tracking");
        Set<Material> tracked_m = new HashSet<>();
        for (String s : tracked_s) {
            Material m = Material.getMaterial(s);
            if (m == null)
                continue;
            tracked_m.add(m);
        }
        if (this.tracker == null)
            this.tracker = new BlockTracker();
        this.tracker.clearTracked();
        for (Material m : tracked_m)
            this.tracker.addTracked(m);
        this.tracker.cleanPlaced();
    }
    public void saveTracker(File file) {
        BlockTracker.save(this.tracker, file);
    }
    public void loadTracker(File file) {
        this.tracker = BlockTracker.load(file);
    }
}
