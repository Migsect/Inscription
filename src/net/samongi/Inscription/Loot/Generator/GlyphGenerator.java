package net.samongi.Inscription.Loot.Generator;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import net.samongi.Inscription.Glyphs.*;
import net.samongi.Inscription.Glyphs.Types.GlyphElement;
import net.samongi.Inscription.Glyphs.Types.GlyphRarity;
import net.samongi.Inscription.Inscription;
import net.samongi.Inscription.Attributes.Attribute;
import net.samongi.Inscription.Attributes.AttributeManager;
import net.samongi.Inscription.Attributes.AttributeType;
import net.samongi.SamongiLib.Configuration.ConfigFile;

import org.bukkit.configuration.ConfigurationSection;

public class GlyphGenerator {

    private String m_typeName = "DEFAULT";
    private String m_displayName = "Default";

    private WeightMap<AttributeType> m_attributeWeights = new WeightMap<>();
    private WeightMap<Integer> m_attributeCountsWeights = new WeightMap<>();
    private WeightMap<GlyphRarity> m_rarityWeights = new WeightMap<>();
    private WeightMap<GlyphElement> m_elementWeight = new WeightMap<>();

    private int m_minLevel = 1;
    private int m_maxLevel = 100;

    private int m_consumableModel = 0;

    /**
     * Constructs a GlyphGenerator with the following type name.
     *
     * @param typeName This will be upcased and underscored spaced.
     */
    public GlyphGenerator(String typeName) {
        this.m_typeName = typeName.toUpperCase().replace(" ", "_");
        this.m_displayName = m_typeName;
    }

    public GlyphGenerator(String typeName, String displayName) {
        this.m_typeName = typeName.toUpperCase().replace(" ", "_");
        this.m_displayName = displayName;
    }

    public String getTypeName() {
        return this.m_typeName;
    }
    public String getDisplayName() {
        return this.m_displayName;
    }
    public int getConsumableModel() {
        return this.m_consumableModel;
    }

    public Glyph getGlyph() {
        return this.getGlyph(m_minLevel, m_maxLevel);
    }

    public Glyph getGlyph(int min_level, int max_level) {
        Random rand = new Random();

        // Creating the new parser
        Glyph glyph = new Glyph();
        glyph.setElement(this.m_elementWeight.getRandom());
        glyph.setRarity(this.m_rarityWeights.getRandom());
        glyph.setLevel(min_level + rand.nextInt(max_level - min_level + 1));

        int attribute_count = this.m_attributeCountsWeights.getRandom();
        Set<String> current_attributes = new HashSet<>();
        for (int i = 0; i < attribute_count; i++) {
            AttributeType attribute_generator = this.m_attributeWeights.getRandom();
            if (attribute_generator == null) {
                Inscription.logger.warning("Attribute Generator return null - Attributes:");
                for (AttributeType key : this.m_attributeWeights.keySet()) {
                    Inscription.logger.warning(" - " + key.getName());
                }
                continue;
            }
            Attribute attribute = attribute_generator.generate();
            // Making sure there aren't duplicates of attributes.
            if (current_attributes.contains(attribute_generator.getName())) {
                i--;
                if (i < 0) {
                    break;
                } else {
                    continue;
                }
            }
            // Adding the attribute to the list of attributes that have already been
            // selected
            current_attributes.add(attribute_generator.getName());
            // Adding the attribute to the glyph.
            glyph.addAttribute(attribute);
        }

        return glyph;
    }

    public void setMinLevel(int level) {
        this.m_minLevel = level;
    }

    public void setMaxLevel(int level) {
        this.m_maxLevel = level;
    }

    public void addAttributeCount(int count, int weight) {
        if (weight <= 0) {
            Inscription.logger.fine("AttrbiuteCount '" + count + "' attempted to be registered with weight " + weight);
            return;
        }
        this.m_attributeCountsWeights.put(count, weight);
    }
    public void addAttributeType(AttributeType type, int weight) {
        if (weight <= 0) {
            Inscription.logger
                .fine("AttributeType '" + type.getName() + "' attempted to be registered with weight " + weight);
            return;
        }
        this.m_attributeWeights.put(type, weight);
    }
    public void addRarity(GlyphRarity rarity, int weight) {
        if (weight <= 0) {
            Inscription.logger.fine("Rarity '" + rarity + "' attempted to be registered with weight " + weight);
            return;
        }
        this.m_rarityWeights.put(rarity, weight);
    }
    public void addElement(GlyphElement element, int weight) {
        if (weight <= 0) {
            Inscription.logger.fine("GlyphElement '" + element + "' attempted to be registered with weight " + weight);
            return;
        }
        this.m_elementWeight.put(element, weight);
    }

    // ---------------------------------------------------------------------------------------------------------------//
    public static List<GlyphGenerator> parse(File file) {
        ConfigFile config = new ConfigFile(file);

        List<GlyphGenerator> generators = new ArrayList<GlyphGenerator>();
        ConfigurationSection root = config.getConfig().getConfigurationSection("generators");
        for (String s : root.getKeys(false)) {
            GlyphGenerator generator = GlyphGenerator.parse(root.getConfigurationSection(s));
            if (generator == null)
                continue;
            generators.add(generator);
        }

        return generators;
    }
    public static GlyphGenerator parse(ConfigurationSection section) {
        // Getting the generator's name
        String typeName = section.getString("type-name");
        if (typeName == null) {
            return null;
        }
        Inscription.logger.fine("Found typeName to be: '" + typeName + "'");

        String displayName = section.getString("display-name");
        Inscription.logger.fine("Found displayName to be: '" + displayName + "'");

        // Creating the generator
        GlyphGenerator generator = new GlyphGenerator(typeName, displayName == null ? typeName : displayName);

        generator.m_consumableModel = section.getInt("model", 1);

        // min and max level setting of the generator
        int minLevel = section.getInt("min-level", 1);
        int maxLevel = section.getInt("max-level", 100);
        generator.setMaxLevel(maxLevel);
        generator.setMinLevel(minLevel);
        Inscription.logger.fine("Found minLevel to be: '" + minLevel + "'");
        Inscription.logger.fine("Found maxLevel to be: '" + maxLevel + "'");

        Inscription.logger.fine("Parsing Elements:");
        ConfigurationSection elements = section.getConfigurationSection("elements");
        Set<String> elementKeys = elements.getKeys(false);
        for (String elementKey : elementKeys) {
            GlyphElement glyphElement = Inscription.getInstance().getGlyphTypesManager().getElement(elementKey);
            if (glyphElement == null) {
                Inscription.logger.warning("'" + elementKey + "' is not a valid glyph element.");
                continue;
            }

            int weight = elements.getInt(elementKey);
            generator.addElement(glyphElement, weight);

            Inscription.logger.fine("  Found glyphElement to be: '" + glyphElement.getType() + "'");
            Inscription.logger.fine("  Found weight to be: '" + weight + "'");
        }

        Inscription.logger.fine("Parsing Rarities:");
        ConfigurationSection rarities = section.getConfigurationSection("rarities");
        Set<String> rarityKeys = rarities.getKeys(false);
        for (String rarityKey : rarityKeys) {
            GlyphRarity rarity = Inscription.getInstance().getGlyphTypesManager().getRarity(rarityKey);
            if (rarity == null) {
                Inscription.logger.warning("'" + rarityKey + "' is not a valid glyph rarity.");
                continue;
            }

            int weight = rarities.getInt(rarityKey);
            generator.addRarity(rarity, weight);

            Inscription.logger.fine("  Found r to be: '" + rarity.getType() + "'");
            Inscription.logger.fine("  Found weight to be: '" + weight + "'");
        }

        Inscription.logger.fine("Parsing Attribute Counts:");
        ConfigurationSection attribute_counts = section.getConfigurationSection("attribute-counts");
        Set<String> attribute_count_keys = attribute_counts.getKeys(false);
        for (String k : attribute_count_keys) {
            int int_k = -1;
            try {
                int_k = Integer.parseInt(k);
            }
            catch (NumberFormatException e) {
            }
            if (int_k <= 0)
                continue; // TODO error message

            int weight = attribute_counts.getInt(k);
            generator.addAttributeCount(int_k, weight);

            Inscription.logger.finest("  Found int_k to be: '" + int_k + "'");
            Inscription.logger.finest("  Found weight to be: '" + weight + "'");
        }

        AttributeManager attribute_manager = Inscription.getInstance().getAttributeManager();

        Inscription.logger.fine("Parsing Attributes:");
        ConfigurationSection attributes = section.getConfigurationSection("attributes");
        Set<String> attribute_keys = attributes.getKeys(false);
        for (String k : attribute_keys) {
            AttributeType type = attribute_manager.getAttributeType(k);
            if (type == null) {
                Inscription.logger.fine("Could not find type for section: '" + k + "'");
                continue;
            }

            int weight = attributes.getInt(k);
            generator.addAttributeType(type, weight);

            Inscription.logger.fine("  Found type to be: '" + type.getName() + "'");
            Inscription.logger.fine("  Found weight to be: '" + weight + "'");
        }

        return generator;
    }
}
