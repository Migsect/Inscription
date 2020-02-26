package net.samongi.Inscription.Loot;

import java.io.File;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import net.samongi.Inscription.Experience.BlockTracker;
import net.samongi.Inscription.Experience.PlayerExperienceOverflowEvent;
import net.samongi.Inscription.Inscription;
import net.samongi.Inscription.Glyphs.Glyph;
import net.samongi.Inscription.Loot.Generator.GlyphGenerator;
import net.samongi.Inscription.Player.PlayerData;
import net.samongi.SamongiLib.Configuration.ConfigFile;

import net.samongi.SamongiLib.Configuration.ConfigurationParsing;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class LootManager implements Listener, ConfigurationParsing {

    private static final String ENTITY_SECTION_KEY = "entity-drops";
    private static final String MATERIAL_SECTION_KEY = "material-drops";
    private static final String EXPERIENCE_OVERFLOW_SECTION_KEY = "experience-overflow-drops";

    //----------------------------------------------------------------------------------------------------------------//

    private Material m_consumableMaterial = Material.PAPER;

    private Map<EntityType, Double> m_entityDropChances = new HashMap<>();
    private Map<EntityType, GlyphGenerator> m_entityGenerators = new HashMap<>();

    private Map<Material, Double> m_blockDropChances = new HashMap<>();
    private Map<Material, GlyphGenerator> m_blockGenerators = new HashMap<>();

    private List<Map<String, Integer>> m_experienceOverflowThresholds = new ArrayList<>();
    private List<GlyphGenerator> m_experienceOverflowGenerators = new ArrayList<>();

    private boolean m_dropConsumables = false;

    //    // Mapping of all the GlyphGenerators (type name to generator)
    //    private Map<String, GlyphGenerator> m_glyphGenerators = new HashMap<>();
    //    private Map<String, GlyphGenerator> m_glyphGeneratorsDisplay = new HashMap<>();

    //----------------------------------------------------------------------------------------------------------------//

    public void setDropConsumables(boolean dropConsumables) {
        m_dropConsumables = dropConsumables;
    }

    //    public void registerGenerator(GlyphGenerator generator) {
    //        this.m_glyphGenerators.put(generator.getTypeName(), generator);
    //        this.m_glyphGeneratorsDisplay.put(generator.getDisplayName(), generator);
    //    }
    //
    //    /**
    //     * Retrieves the generator with the type name.
    //     *
    //     * @param type_name The name of the generator to retrieve
    //     * @return The GlyphGenerator
    //     */
    //    public GlyphGenerator getGenerator(String type_name) {
    //        return this.m_glyphGenerators.get(type_name);
    //    }
    //
    //    /**
    //     * Retrieves a list of all the generators.
    //     *
    //     * @return All the generators.
    //     */
    //    public List<GlyphGenerator> getGenerators() {
    //        return new ArrayList<GlyphGenerator>(this.m_glyphGenerators.values());
    //    }

    /**
     * Registers the generator to the entity type.
     *
     * @param type      The type of the entity to register the generator to.
     * @param generator The generator to register.
     * @param chance    The probability that the generator will be used.
     */
    public void registerGeneratorToEntity(EntityType type, GlyphGenerator generator, double chance) {

        this.m_entityDropChances.put(type, chance);
        this.m_entityGenerators.put(type, generator);
        Inscription.logger.fine("Registered drop for entity '" + type.toString() + "' with generator '" + generator.getTypeName() + "' with chance " + chance);
    }

    /**
     * Registers the generator to the material type.
     *
     * @param type      The type of the mateiral (block) to register the generator to.
     * @param generator The generator to register.
     * @param chance    The probability that the generator will be used.
     */
    public void registerGeneratorToMaterial(Material type, GlyphGenerator generator, double chance) {
        this.m_blockDropChances.put(type, chance);
        this.m_blockGenerators.put(type, generator);
        Inscription.logger
            .fine("Registered drop for material '" + type.toString() + "' with generator '" + generator.getTypeName() + "' with chance " + chance);

    }

    public void addGeneratorToExperienceOverflow(Map<String, Integer> experienceMap, GlyphGenerator generator) {
        this.m_experienceOverflowThresholds.add(experienceMap);
        this.m_experienceOverflowGenerators.add(generator);
        Inscription.logger.fine(String.format("Registered experience overflow with generator '%s'", generator.getTypeName()));
    }
    // ---------------------------------------------------------------------------------------------------------------//

    //    /**
    //     * Parses all configuration files that represent GlyphGenerators and will add
    //     * them all to the LootManager
    //     *
    //     * @param directory The directory of the config files that are generators
    //     */
    //    public void parseGenerators(File directory) {
    //        if (!directory.exists())
    //            return; // TODO error message
    //        if (!directory.isDirectory())
    //            return; // TODO error message
    //
    //        File[] files = directory.listFiles();
    //        for (File file : files) {
    //            if (!file.exists() || file.isDirectory()) {
    //                continue;
    //            }
    //
    //            List<GlyphGenerator> generators = GlyphGenerator.parse(file);
    //            for (GlyphGenerator generator : generators) {
    //                this.registerGenerator(generator);
    //            }
    //        }
    //    }

    private boolean parseEntities(@Nonnull ConfigurationSection section) {
        if (section == null) {
            return false;
        }

        Set<String> entityKeys = section.getKeys(false);
        for (String entityKey : entityKeys) {
            EntityType type = null;
            try {
                type = EntityType.valueOf(entityKey);
            }
            catch (InvalidParameterException exception) {
                Inscription.logger.warning(String.format("'%s' is not a valid entity type.", entityKey));
                continue;
            }
            ConfigurationSection entitySection = section.getConfigurationSection(entityKey);

            String generatorString = entitySection.getString("generator");
            if (generatorString == null) {
                Inscription.logger.warning(String.format("Section '%s' does not define a generator type.", entityKey));
                continue;
            }

            GlyphGenerator generator = Inscription.getInstance().getGeneratorManager().getGeneratorByType(generatorString);
            if (generator == null) {
                Inscription.logger.warning(String.format("'%s' is not a valid generator type.", generatorString));
                continue;
            }

            if (!entitySection.isDouble("rate")) {
                Inscription.logger.warning(String.format("'%s' does not have a valid rate.", entityKey));
                continue;
            }
            double rate = entitySection.getDouble("rate");

            this.registerGeneratorToEntity(type, generator, rate);
        }
        return true;

    }

    private boolean parseMaterials(@Nonnull ConfigurationSection section) {
        if (section == null) {
            return false;
        }

        Set<String> materialKeys = section.getKeys(false);
        for (String materialKey : materialKeys) {
            Material type = null;
            try {
                type = Material.valueOf(materialKey);
            }
            catch (InvalidParameterException exception) {
                Inscription.logger.warning(String.format("'%s' is not a valid material type.", materialKey));
                continue;
            }
            ConfigurationSection materialSection = section.getConfigurationSection(materialKey);

            String generatorString = materialSection.getString("generator");
            if (generatorString == null) {

                Inscription.logger.warning(String.format("Section '%s' does not define a generator type.", materialKey));
                continue;
            }
            GlyphGenerator generator = Inscription.getInstance().getGeneratorManager().getGeneratorByType(generatorString);
            if (generator == null) {
                Inscription.logger.warning(String.format("'%s' is not a valid generator type.", generatorString));
                continue;
            }

            if (!materialSection.isDouble("rate")) {
                Inscription.logger.warning(String.format("'%s' does not have a valid rate.", materialKey));
                continue;
            }
            double rate = materialSection.getDouble("rate");

            this.registerGeneratorToMaterial(type, generator, rate);
        }
        return true;
    }

    private boolean parseExperienceOverflow(@Nonnull ConfigurationSection section) {
        if (section == null) {
            return false;
        }

        Set<String> experienceSectionKeys = section.getKeys(false);
        for (String experienceSectionKey : experienceSectionKeys) {
            ConfigurationSection experienceSection = section.getConfigurationSection(experienceSectionKey);

            String generatorString = experienceSection.getString("generator");
            if (generatorString == null) {
                Inscription.logger.warning(String.format("Section '%s' does not define a generator type.", experienceSectionKey));
                continue;
            }

            GlyphGenerator generator = Inscription.getInstance().getGeneratorManager().getGeneratorByType(generatorString);
            if (generator == null) {
                Inscription.logger.warning(String.format("'%s' is not a valid generator type.", generatorString));
                continue;
            }

            ConfigurationSection experienceMappingSection = experienceSection.getConfigurationSection("experience");
            Map<String, Integer> experienceMapping = new HashMap<>();
            Set<String> experienceKeys = experienceMappingSection.getKeys(false);
            for (String experienceKey : experienceKeys) {
                if (!experienceMappingSection.isInt(experienceKey)) {
                    continue;
                }
                int experienceValue = experienceMappingSection.getInt(experienceKey);
                experienceMapping.put(experienceKey, experienceValue);
            }

            this.addGeneratorToExperienceOverflow(experienceMapping, generator);
        }
        return true;
    }

    @Override public boolean parseConfigFile(@Nonnull File file, @Nonnull ConfigFile config) {

        FileConfiguration root = config.getConfig();
        Inscription.logger.info("Parsing Loot Configurations in: '" + file.getAbsolutePath() + "'");
        if (root == null) {
            return false;
        }

        boolean parsedSomething = false;
        if (parseEntities(root.getConfigurationSection(ENTITY_SECTION_KEY))) {
            Inscription.logger.info(String.format(" - Registered: '%s'", ENTITY_SECTION_KEY));
            parsedSomething = true;
        }
        if (parseMaterials(root.getConfigurationSection(MATERIAL_SECTION_KEY))) {
            Inscription.logger.info(String.format(" - Registered: '%s'", MATERIAL_SECTION_KEY));
            parsedSomething = true;
        }
        if (parseExperienceOverflow(root.getConfigurationSection(EXPERIENCE_OVERFLOW_SECTION_KEY))) {
            Inscription.logger.info(String.format(" - Registered: '%s'", EXPERIENCE_OVERFLOW_SECTION_KEY));
            parsedSomething = true;
        }

        if (!parsedSomething) {
            Inscription.logger.warning(String.format("Didn't find anything to parse in '%s'", file.getAbsolutePath()));
        }
        return parsedSomething;
    }

    //    public void parseDrops(@Nonnull File file) {
    //        if (!file.exists())
    //            return; // TODO error message;
    //        if (file.isDirectory())
    //            return; // TODO error message;
    //
    //        ConfigFile config = new ConfigFile(file);
    //        ConfigurationSection root = config.getConfig().getConfigurationSection("drops");
    //        if (root == null) {
    //            return; // TODO error message
    //        }
    //
    //        parseEntities(root.getConfigurationSection("entities"));
    //        parseMaterials(root.getConfigurationSection("materials"));
    //        parseExperienceOverflow(root.getConfigurationSection("experience-overflow"));
    //    }

    // ---------------------------------------------------------------------------------------------------------------//
    @EventHandler public void onEntityDeath(EntityDeathEvent event) {
        EntityType type = event.getEntityType();
        if (!m_entityDropChances.containsKey(type)) {
            return;
        }
        if (!m_entityGenerators.containsKey(type)) {
            return;
        }
        double typeChance = m_entityDropChances.get(type);
        Inscription.logger.finest(String.format("Glyph Drop Chance %s", typeChance));

        // Checking to see if the mob will drop the glyph
        Random rand = new Random();
        if (rand.nextDouble() > typeChance) {
            return;
        }

        GlyphGenerator generator = m_entityGenerators.get(type);
        Location loc = event.getEntity().getLocation();

        this.dropGlyph(generator, loc);
    }

    @EventHandler public void onBlockBreak(BlockBreakEvent event) {
        if (event.isCancelled()) {
            return;
        }

        Location location = event.getBlock().getLocation();
        Material type = event.getBlock().getType();
        BlockTracker tracker = Inscription.getInstance().getBlockTracker();
        if (tracker.isTracked(type) && tracker.isPlaced(location)) {
            return;
        }

        if (!m_blockDropChances.containsKey(type) || !m_blockGenerators.containsKey(type)) {
            return;
        }
        double typeChance = m_blockDropChances.get(type);
        Inscription.logger.finest(String.format("Glyph Drop Chance %s", typeChance));

        // Checking to see if the mob will drop the glyph
        Random rand = new Random();
        if (rand.nextDouble() > typeChance) {
            return;
        }

        GlyphGenerator generator = m_blockGenerators.get(type);

        this.dropGlyph(generator, location);
    }

    @EventHandler public void onPlayerInteractEvent(PlayerInteractEvent event) {
        EquipmentSlot usedSlot = event.getHand();
        if (event.useItemInHand() == Event.Result.DENY || usedSlot == EquipmentSlot.OFF_HAND) {
            return;
        }
        Player player = event.getPlayer();
        ItemStack itemUsed = null;
        if (usedSlot == EquipmentSlot.OFF_HAND) {
            itemUsed = player.getInventory().getItemInOffHand();
        } else if (usedSlot == EquipmentSlot.HAND) {
            itemUsed = player.getInventory().getItemInMainHand();
        } else {
            return;
        }
        if (itemUsed == null || itemUsed.getType() != m_consumableMaterial) {
            return;
        }
        GlyphGenerator generator = getGeneratorFromItemStack(itemUsed);
        if (generator == null) {
            return;
        }

        int amount = itemUsed.getAmount();
        if (amount > 1) {
            itemUsed.setAmount(amount - 1);
        } else {
            if (usedSlot == EquipmentSlot.OFF_HAND) {
                player.getInventory().setItemInOffHand(new ItemStack(Material.AIR));
            }
            if (usedSlot == EquipmentSlot.HAND) {
                player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
            }
        }

        Location playerLocation = player.getLocation();
        Glyph glyph = generator.getGlyph();
        ItemStack glyphItem = glyph.getItemStack();
        playerLocation.getWorld().dropItem(playerLocation, glyphItem);
        event.setUseItemInHand(Event.Result.DENY);
        event.setUseInteractedBlock(Event.Result.DENY);
    }

    @EventHandler public void onPlayerExperienceOverflowEvent(PlayerExperienceOverflowEvent event) {
        Player player = event.getPlayer();
        int amount = event.getAmount();
        if (amount == 0) {
            return;
        }

        String experienceType = event.getExperienceType();
        Inscription.logger.finest("[PlayerExperienceOverflowEvent] '" + experienceType + "' : " + amount);
        PlayerData data = Inscription.getInstance().getPlayerManager().getData(player);

        Map<String, Integer> futureExperienceMapping = new HashMap<>(data.getExperience());
        // Updating the current experience to reflect what it may become.
        futureExperienceMapping.put(experienceType, futureExperienceMapping.getOrDefault(experienceType, 0) + amount);

        int index = 0;
        for (Map<String, Integer> mapping : m_experienceOverflowThresholds) {
            boolean thresholdMet = true;
            for (String key : mapping.keySet()) {
                int future = futureExperienceMapping.getOrDefault(key, 0);
                int compare = mapping.get(key);
                if (future < compare) {
                    thresholdMet = false;
                    break;
                }
            }
            if (thresholdMet) {
                GlyphGenerator generator = m_experienceOverflowGenerators.get(index);
                this.dropGlyph(generator, player.getLocation());

                // Removing the experience (this will put the character into negatives for the type that caused the
                // overflow for a glyph.
                for (String key : mapping.keySet()) {
                    data.addExperience(key, -mapping.get(key));
                }
                // The experience should be negative or 0 that's currently on the player.
                event.setAmount(amount + data.getExperience(experienceType));
                break;
            }
            index++;
        }
    }

    //----------------------------------------------------------------------------------------------------------------//
    @Nonnull public ItemStack createGeneratorConsumable(@Nonnull GlyphGenerator generator) {
        ItemStack itemStack = new ItemStack(m_consumableMaterial);
        ItemMeta itemMeta = itemStack.getItemMeta();

        itemMeta.setDisplayName("Undiscovered Glyph");

        List<String> itemLore = new ArrayList<String>();
        itemLore.add(ChatColor.MAGIC + "" + ChatColor.YELLOW + "Unknown " + ChatColor.BLUE + generator.getDisplayName() + ChatColor.YELLOW + " Glyph");
        itemLore.add(ChatColor.YELLOW + "Use to discover a random glyph.");
        itemMeta.setLore(itemLore);
        itemMeta.setCustomModelData(generator.getConsumableModel());

        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }

    @Nullable public GlyphGenerator getGeneratorFromItemStack(@Nonnull ItemStack itemStack) {
        ItemMeta itemMeta = itemStack.getItemMeta();
        List<String> itemLore = itemMeta.getLore();
        if (itemLore == null || itemLore.size() < 1) {
            return null;
        }

        String generatorLine = ChatColor.stripColor(itemLore.get(0));
        if (!generatorLine.startsWith("Unknown ") || !generatorLine.endsWith(" Glyph")) {
            return null;
        }
        String[] generatorLineSplit = generatorLine.split(" ");
        if (generatorLineSplit.length < 3) { // Invalid if we don't have three things.
            return null;
        }
        String generatorDisplay = "";
        for (int split = 1; split < generatorLineSplit.length - 1; split++) {
            generatorDisplay += generatorLineSplit[split];
        }

        return Inscription.getInstance().getGeneratorManager().getGeneratorByName(generatorDisplay);

    }

    private void dropGlyph(@Nonnull GlyphGenerator generator, @Nonnull Location location) {

        ItemStack item = null;
        if (m_dropConsumables) {
            item = createGeneratorConsumable(generator);
        } else {
            Glyph glyph = generator.getGlyph();
            item = glyph.getItemStack();
        }
        location.getWorld().dropItem(location, item);
    }

}
