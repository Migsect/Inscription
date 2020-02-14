package net.samongi.Inscription.Experience;

import java.io.File;
import java.util.*;

import net.samongi.Inscription.Inscription;
import net.samongi.Inscription.Player.PlayerData;
import net.samongi.SamongiLib.Configuration.ConfigFile;
import net.samongi.SamongiLib.Items.Comparators.BlockDataAgeableComparator;
import net.samongi.SamongiLib.Items.Comparators.BlockDataComparator;
import net.samongi.SamongiLib.Items.Comparators.BlockDataGroupComparator;
import net.samongi.SamongiLib.Items.Comparators.BlockDataMaterialComparator;
import net.samongi.SamongiLib.Items.ItemUtil;

import net.samongi.SamongiLib.Items.MaskedBlockData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.material.MaterialData;
import org.bukkit.scheduler.BukkitRunnable;

import javax.annotation.Nonnull;

public class ExperienceManager {

    // Experience mapping for entity related events
    private Map<EntityType, Map<String, Integer>> experiencePerKill = new HashMap<>();
    private Map<EntityType, Map<String, Integer>> experiencePerDamage = new HashMap<>();

    //    BlockDataComparator m_comparator = new BlockDataGroupComparator(new BlockDataComparator[]{
    //        new BlockDataMaterialComparator(),
    //        new BlockDataAgeableComparator()
    //    });
    // Experience mapping for material related events
    private final static MaskedBlockData.Mask[] BLOCKDATA_MASKS = new MaskedBlockData.Mask[]{
        MaskedBlockData.Mask.MATERIAL,
        MaskedBlockData.Mask.AGEABLE
    };
    private Map<MaskedBlockData, Map<String, Integer>> experiencePerBreak = new HashMap<>();
    private Map<MaskedBlockData, Map<String, Integer>> experiencePerPlace = new HashMap<>();
    private Map<Material, Map<String, Integer>> experiencePerCraft = new HashMap<>();

    // Tracks blocks that wouldn't be valid for experience because they may have been used to cheat.
    private BlockTracker tracker;

    // -----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------//
    /**
     * Method that encompasses logic to be handled by the experience manager
     * when another entity damages another. Generally for player experience
     * rewards.
     *
     * @param event
     */
    public void onEntityDamageEntity(EntityDamageByEntityEvent event)
    {
        Entity damaged = event.getEntity();
        Entity damager = event.getDamager();
        if (!(damager instanceof Player)) {
            return;
        }

        PlayerData data = Inscription.getInstance().getPlayerManager().getData((Player) damager);
        if (data == null) {
            Inscription.logger.severe("Player data return null on call for: " + damager.getName() + ":"
                + damager.getUniqueId());
            return;
        }

        EntityType damagedType = damaged.getType(); // Getting the type of the damage entity.
        double damageDealt = event.getFinalDamage();

        Map<String, Integer> experienceMapping = this.getExpPerDamage(damagedType);
        if (experienceMapping == null) return;

        for (String key : experienceMapping.keySet()) {
            int experience = (int) Math.ceil(experienceMapping.get(key) * damageDealt);
            data.getGlyphInventory().distributeExperience(key, experience);
        }
    }
    public void onEntityDeath(EntityDeathEvent event)
    {
        LivingEntity killed = event.getEntity();
        EntityDamageEvent damageEvent = killed.getLastDamageCause();
        if (!(damageEvent instanceof EntityDamageByEntityEvent)) {
            return;
        }
        EntityDamageByEntityEvent entityDamageEvent = (EntityDamageByEntityEvent) damageEvent;

        Entity damaged = entityDamageEvent.getEntity();
        Entity damager = entityDamageEvent.getDamager();
        if (!(damager instanceof Player)) {
            return;
        }

        PlayerData data = Inscription.getInstance().getPlayerManager().getData((Player) damager);
        if (data == null) {
            Inscription.logger.severe("Player data return null on call for: " + damager.getName() + ":" + damager.getUniqueId());
            return;
        }

        EntityType damaged_t = damaged.getType();

        Map<String, Integer> experienceMapping = this.getExpPerKill(damaged_t);
        if (experienceMapping == null) {
            return;
        }

        for (String key : experienceMapping.keySet()) {
            int experience = experienceMapping.get(key);
            data.getGlyphInventory().distributeExperience(key, experience);
        }
    }

    public void onBlockBreak(BlockBreakEvent event)
    {
        Location location = event.getBlock().getLocation();
        Material material = event.getBlock().getType();
        if (this.tracker.isTracked(material) && this.tracker.isPlaced(location)) {
            // This block break cannot trigger experience.
            return;
        }

        BlockData blockData = event.getBlock().getBlockData();
        Player player = event.getPlayer();

        PlayerData data = Inscription.getInstance().getPlayerManager().getData(player);
        if (data == null) {
            Inscription.logger.severe("Player Data return null on call for: " + player.getName() + ":" + player.getUniqueId());
            return;
        }
        Map<String, Integer> experienceMapping = this.getExpPerBreak(blockData);
        if (experienceMapping == null) {
            Inscription.logger.finest("Found not experience for " + blockData.getAsString(true));
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
    public void onBlockPlace(BlockPlaceEvent event)
    {
        BlockData blockData = event.getBlock().getBlockData();
        Player player = event.getPlayer();

        PlayerData data = Inscription.getInstance().getPlayerManager().getData(player);
        if (data == null) {
            Inscription.logger.severe("Player data return null on call for: " + player.getName() + ":" + player.getUniqueId());
            return;
        }

        Map<String, Integer> experienceMapping = this.getExpPerPlace(blockData);
        if (experienceMapping == null) {
            return;
        }

        for (String key : experienceMapping.keySet()) {
            int exp = experienceMapping.get(key);
            data.getGlyphInventory().distributeExperience(key, exp);
        }
    }

    public void onCraftItem(CraftItemEvent event)
    {
        if (event.isCancelled()) return;

        Recipe recipe = event.getRecipe(); // getting the recipe
        ItemStack result = recipe.getResult(); // getting the result of the craft event

        Inscription.logger.fine("onCraftItem: Item Crafted Type: " + result.getType());
        Material resultMaterial = recipe.getResult().getType();

        // Grabbing the experience mapping that is the reward if there is none then we will just stop the event handling now.
        Map<String, Integer> experienceMap = Inscription.getInstance().getExperienceManager().getExpPerCraft(resultMaterial);
        if (experienceMap == null) {
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

                @Override
                public void run()
                {
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

                    for (String s : experienceMap.keySet()) {
                        int exp = experienceMap.get(s);
                        for (int i = 0; i < totalCrafts; i++) {
                            data.getGlyphInventory().distributeExperience(s, exp);
                        }
                    }
                }
            };
            task.runTask(Inscription.getInstance()); // Running the task
        } else {
            PlayerData data = Inscription.getInstance().getPlayerManager().getData(player);
            if (data == null) {
                Inscription.logger.severe("Player data return null on call for: " + player.getName() + ":"
                    + player.getUniqueId());
                return;
            }

            for (String key : experienceMap.keySet()) {
                int experience = experienceMap.get(key);
                data.getGlyphInventory().distributeExperience(key, experience);
            }
        }
    }

    public void onEnchantItem(EnchantItemEvent event)
    {
        // TODO
    }
    public void onAnvilRepair()
    {
    } // TODO Anvil events are most likely an inventory interact event

    // -----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------//
    public void setExpPerKill(EntityType type, String experienceType, int amount)
    {
        if (!this.experiencePerKill.containsKey(type)) {
            this.experiencePerKill.put(type, new HashMap<String, Integer>());
        }
        this.experiencePerKill.get(type).put(experienceType, amount);
    }
    public Map<String, Integer> getExpPerKill(EntityType type)
    {
        if (!this.experiencePerKill.containsKey(type)) {
            return new HashMap<>();
        }
        return this.experiencePerKill.get(type);
    }

    // -----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------//
    public void setExpPerDamage(EntityType type, String experienceType, int amount)
    {
        if (!this.experiencePerDamage.containsKey(type)) this.experiencePerDamage.put(type, new HashMap<String, Integer>());
        this.experiencePerDamage.get(type).put(experienceType, amount);
    }
    public Map<String, Integer> getExpPerDamage(EntityType type)
    {
        if (!this.experiencePerDamage.containsKey(type)) return new HashMap<>();
        return this.experiencePerDamage.get(type);
    }
    // -----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------//

    public void setExpPerBreak(BlockData data, String experienceType, int amount)
    {
        MaskedBlockData key = new MaskedBlockData(data, BLOCKDATA_MASKS);
        if (!this.experiencePerBreak.containsKey(key)) {
            this.experiencePerBreak.put(key, new HashMap<String, Integer>());
        }
        this.experiencePerBreak.get(key).put(experienceType, amount);
    }
    public Map<String, Integer> getExpPerBreak(BlockData data)
    {
        MaskedBlockData key = new MaskedBlockData(data, BLOCKDATA_MASKS);
        if (!this.experiencePerBreak.containsKey(key)) {
            return new HashMap<>();
        }
        return this.experiencePerBreak.get(key);
    }

    // -----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------//

    public void setExpPerPlace(BlockData data, String experienceType, int amount)
    {
        MaskedBlockData key = new MaskedBlockData(data, BLOCKDATA_MASKS);
        if (!this.experiencePerPlace.containsKey(key)) {
            this.experiencePerPlace.put(key, new HashMap<String, Integer>());
        }
        this.experiencePerPlace.get(key).put(experienceType, amount);
    }
    public Map<String, Integer> getExpPerPlace(BlockData data)
    {
        MaskedBlockData key = new MaskedBlockData(data, BLOCKDATA_MASKS);
        if (!this.experiencePerPlace.containsKey(key)) {
            return new HashMap<>();
        }
        return this.experiencePerPlace.get(key);
    }

    // -----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------//

    public void setExpPerCraft(Material data, String experienceType, int amount)
    {
        if (!this.experiencePerCraft.containsKey(data)) this.experiencePerCraft.put(data, new HashMap<String, Integer>());
        this.experiencePerCraft.get(data).put(experienceType, amount);
    }
    public Map<String, Integer> getExpPerCraft(Material data)
    {
        if (!this.experiencePerCraft.containsKey(data)) return new HashMap<>();
        return this.experiencePerCraft.get(data);
    }
    // -----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------//

    public static BlockData parseBlockData(@Nonnull String parseable)
    {
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
    public static Material parseMaterialData(@Nonnull String parseable)
    {
        try {
            Material materialType = Material.valueOf(parseable);
            return materialType;
        }
        catch (IllegalArgumentException error) {
        }

        Inscription.logger.warning("Could not find type for: " + parseable);
        return null;
    }

    // -----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------//

    public void parseEntityDamageReward(@Nonnull ConfigurationSection section) {
        Inscription.logger.fine("Parsing entity damage experience rewards...");

        Set<String> entity_damage_keys = section.getKeys(false);
        for (String key : entity_damage_keys) {

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

            ConfigurationSection keySection = section.getConfigurationSection(key);
            if (keySection == null) {
                Inscription.logger.warning("  " + key + "'s configuration section is null.");
                continue;
            }

            Set<String> keySectionKeys = keySection.getKeys(false);
            for (String experience : keySectionKeys) {
                this.setExpPerDamage(entityType, experience, keySection.getInt(experience));
            }

            Inscription.logger.fine("  Parsed: " + key + " registered: " + entityType.toString());
        }
    }

    // -----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------//
    public void parseEntityKillReward(@Nonnull ConfigurationSection section) {
        Inscription.logger.fine("Parsing entity kill experience rewards...");

        Set<String> entity_damage_keys = section.getKeys(false);
        for (String key : entity_damage_keys) {

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

            ConfigurationSection key_section = section.getConfigurationSection(key);
            if (key_section == null) {
                Inscription.logger.warning("  " + key + "'s configuration section is null.");
                continue;
            }

            Set<String> experienceTypeKeys = key_section.getKeys(false);
            for (String exp : experienceTypeKeys) {
                this.setExpPerKill(entityType, exp, key_section.getInt(exp));
            }

            Inscription.logger.fine("  Parsed: " + key + " registered: " + entityType.toString());
        }
    }

    // -----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------//
    public void parseMaterialBreakReward(@Nonnull ConfigurationSection section)
    {
        Inscription.logger.fine("Parsing material break experience rewards");

        Set<String> blockDataKeys = section.getKeys(false);
        for (String key : blockDataKeys) {
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

            Set<String> experienceTypeSectionKeys = experienceTypeSection.getKeys(false);
            for (String experience : experienceTypeSectionKeys) {
                this.setExpPerBreak(blockData, experience, experienceTypeSection.getInt(experience));
            }

            Inscription.logger.fine("  Parsed: " + key + " registered: " + blockData.getAsString(true));
        }
    }

    // -----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------//
    public void parseMaterialCraft(@Nonnull ConfigurationSection section)
    {
        Inscription.logger.fine("Parsing material craft experience rewards");

        Set<String> blockDataKeys = section.getKeys(false);
        for (String key : blockDataKeys) {
            Material materialData = parseMaterialData(key);
            if (materialData == null) {
                Inscription.logger.warning("  " + key + " is not valid material data.");
                continue;
            }

            ConfigurationSection experienceTypeSection = section.getConfigurationSection(key);
            if (experienceTypeSection == null) {
                Inscription.logger.warning("  " + key + "'s configuration section is null.");
                continue;
            }

            Set<String> experienceTypeSectionKeys = experienceTypeSection.getKeys(false);
            for (String experience : experienceTypeSectionKeys) {
                this.setExpPerCraft(materialData, experience, experienceTypeSection.getInt(experience));
            }

            Inscription.logger.fine("  Parsed: " + key + " registered: " + materialData.toString());
        }
    }

    // -----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------//
    public void parseMaterialPlace(@Nonnull ConfigurationSection section)
    {
        Inscription.logger.fine("Parsing material place experience rewards");

        Set<String> blockDataKeys = section.getKeys(false);
        for (String key : blockDataKeys) {
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

            Set<String> experienceTypeSectionKeys = experienceTypeSection.getKeys(false);
            for (String experience : experienceTypeSectionKeys) {
                this.setExpPerPlace(blockData, experience, experienceTypeSection.getInt(experience));
            }

            Inscription.logger.fine("  Parsed: " + key + " registered: " + blockData.getAsString(true));
        }
    }

    // -----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------//

    /**
     * Parses a file with experience mappings.
     *
     * @param config A filling with experience mappings
     */
    public void parse(@Nonnull ConfigFile config) {
        FileConfiguration root = config.getConfig();

        ConfigurationSection entityDamage = root.getConfigurationSection("entity-damage");
        if (entityDamage != null) {
            parseEntityDamageReward(entityDamage);
        } else {
            Inscription.logger.warning("'entity-damage' was not defined.");
        }

        ConfigurationSection entityKill = root.getConfigurationSection("entity-kill");
        if (entityKill != null) {
            parseEntityKillReward(entityKill);
        } else {
            Inscription.logger.warning("'entity-kill' was not defined.");
        }

        ConfigurationSection materialBreak = root.getConfigurationSection("material-break");
        if (materialBreak != null) {
            parseMaterialBreakReward(materialBreak);
        } else {
            Inscription.logger.warning("'material-break' was not defined.");
        }

        ConfigurationSection materialCraft = root.getConfigurationSection("material-craft");
        if (materialCraft != null) {
            parseMaterialCraft(materialCraft);
        } else {
            Inscription.logger.warning("'material-craft' was not defined.");
        }

        ConfigurationSection materialPlace = root.getConfigurationSection("material-place");
        if (materialPlace != null) {
            parseMaterialPlace(materialPlace);
        } else {
            Inscription.logger.warning("'material-place' was not defined.");
        }
    }

    public BlockTracker getTracker()
    {
        return this.tracker;
    }
    public boolean hasTracker()
    {
        return this.tracker != null;
    }

    /**
     * Will configure the block tracker in the ExperienceManager using the
     * provided fileconfiguration
     *
     * @param config The file configuration to use.
     */
    public void configureTracker(FileConfiguration config)
    {
        List<String> tracked_s = config.getStringList("placement-tracking");
        Set<Material> tracked_m = new HashSet<>();
        for (String s : tracked_s) {
            Material m = Material.getMaterial(s);
            if (m == null) continue;
            tracked_m.add(m);
        }
        if (this.tracker == null) this.tracker = new BlockTracker();
        this.tracker.clearTracked();
        for (Material m : tracked_m)
            this.tracker.addTracked(m);
        this.tracker.cleanPlaced();
    }
    public void saveTracker(File file)
    {
        BlockTracker.save(this.tracker, file);
    }
    public void loadTracker(File file)
    {
        this.tracker = BlockTracker.load(file);
    }
}
