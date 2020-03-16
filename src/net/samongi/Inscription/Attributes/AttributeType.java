package net.samongi.Inscription.Attributes;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import net.md_5.bungee.api.ChatColor;
import net.samongi.Inscription.Experience.ExperienceMap;
import net.samongi.Inscription.Glyphs.Glyph;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;

import javax.annotation.Nonnull;

public abstract class AttributeType {

    /**
     * Transforms the section into an int map of keys to integers
     *
     * @param section The section to map
     * @return
     */
    @Deprecated public static Map<String, Integer> getIntMap(ConfigurationSection section) {
        Map<String, Integer> map = new HashMap<>();
        if (section == null) {
            return map;
        }
        Set<String> base_key = section.getKeys(false);
        for (String k : base_key) {
            int amount = section.getInt(k);
            if (amount == 0) {
                continue;
            }
            map.put(k, amount);
        }
        return map;
    }
    //--------------------------------------------------------------------------------------------------------------------//
    private final String m_implementationType;
    private final String m_typeName;
    protected final String m_displayName;

    private double m_experienceRarityMultiplier;
    private double m_effectRarityMultiplier;
    private int m_modelIncrement;

    private ExperienceMap m_baseExperience;
    private ExperienceMap m_levelExperience;

    //--------------------------------------------------------------------------------------------------------------------//
    public AttributeType(@Nonnull ConfigurationSection section) throws InvalidConfigurationException {
        m_implementationType = section.getString("type");

        m_typeName = section.getString("name");
        if (m_typeName == null) {
            throw new InvalidConfigurationException("'name' is not defined");
        }

        m_displayName = section.getString("descriptor", section.getString("display-name"));
        if (m_displayName == null) {
            throw new InvalidConfigurationException("'descriptor' is not defined");
        }

        if (section.isConfigurationSection("rarity-multiplier")) {
            m_experienceRarityMultiplier = section.getDouble("rarity-multiplier.experience", 1);
            m_effectRarityMultiplier = section.getDouble("rarity-multiplier.effect", 1);
        } else {
            double rarityMultiplier = section.getDouble("rarity-multiplier", 1);
            m_experienceRarityMultiplier = rarityMultiplier;
            m_effectRarityMultiplier = rarityMultiplier;
        }

        ConfigurationSection baseExperienceSection = section.getConfigurationSection("base-experience");
        ConfigurationSection levelExperienceSection = section.getConfigurationSection("level-experience");
        if (baseExperienceSection == null) {
            throw new InvalidConfigurationException("'base-experience' is not defined");
        }
        if (levelExperienceSection == null) {
            throw new InvalidConfigurationException("'level-experience' is not defined");
        }
        m_baseExperience = new ExperienceMap(baseExperienceSection);
        m_levelExperience = new ExperienceMap(levelExperienceSection);

        m_modelIncrement = section.getInt("model-increment", 0);
    }
    //--------------------------------------------------------------------------------------------------------------------//
    /**
     * Generates an attribute for a glyph.
     * This attribute will have no current glyph set to it and will need to have a
     * glyph set to it
     *
     * @return An unattuned attribute
     */
    public abstract Attribute generate();

    /**
     * Generates and adds the attribute to the glyph.
     *
     * @param glyph A glyph to add this attribute to.
     * @return The attribute generated
     */
    public Attribute generate(Glyph glyph) {
        Attribute attr = this.generate();
        glyph.addAttribute(attr);
        return attr;
    }

    /**
     * Will parse the string and attempt to construct a Attribute of this type
     * based off the
     * line. Otherwise if it cannot parse it, it will return null.
     *
     * @param line A lore line from an item to be parsed
     * @return Attribute if line was successfully parsed.
     */
    public Attribute parse(String line) {
        String reduced = ChatColor.stripColor(line.toLowerCase().trim());
        if (reduced.startsWith(this.m_displayName.toLowerCase())) {
            return this.generate();
        } else {
            return null;
        }
    }
    //--------------------------------------------------------------------------------------------------------------------//

    public String getImplementationType() {
        return m_implementationType;
    }

    /**
     * Get the universal type name of the Attribute
     * This will be unique amoung all attributes of the same class.
     * TODO Probably make other code work off classes, however this will do for
     * naming reasons
     *
     * @return A type name string, this string should not be handled in a case
     * sensitive manner
     */
    public String getTypeName() {
        return this.m_typeName;
    }

    /**
     * Get a name descriptor the attribute to be used in the name of the item.
     * For example: "Dangerous ... Glyph" "Unyeilding ... Glyph"
     * This is not used for attribute identification
     *
     * @return A name descriptor, this should be returned all lowercase by
     * contract/
     */
    public String getDisplayName() {
        return this.m_displayName;
    }

    public String getLoreLine() {
        return "" + ChatColor.YELLOW + ChatColor.ITALIC + this.getDisplayName() + " - " + ChatColor.RESET;
    }

    //--------------------------------------------------------------------------------------------------------------------//
    @Deprecated public Map<String, Integer> getBaseExperience_LEGACY() {
        return m_baseExperience.get();
    }
    @Deprecated public Map<String, Integer> getLevelExperience_LEGACY() {
        return m_levelExperience.get();
    }
    public ExperienceMap getBaseExperience() {
        return m_levelExperience;
    }
    public ExperienceMap getLevelExperience() {
        return m_levelExperience;
    }

    //--------------------------------------------------------------------------------------------------------------------//
    @Deprecated public void setRarityMultiplier(double multiplier) {
        setEffectRarityMultiplier(multiplier);
        setEffectRarityMultiplier(multiplier);
    }

    public void setEffectRarityMultiplier(double multiplier) {
        m_effectRarityMultiplier = multiplier;
    }

    public void setExperienceRarityMultiplier(double multiplier) {
        m_experienceRarityMultiplier = multiplier;
    }

    /**
     * Returns the rarity multiplier ratio that the attribute type uses.
     *
     * @return an amount to mutliply by for rarity
     */
    public double getEffectRarityMultiplier() {
        return this.m_experienceRarityMultiplier;
    }
    public double getExperienceRarityMultiplier() {
        return this.m_experienceRarityMultiplier;
    }

    public double calculateEffectRarityMultiplier(Glyph glyph) {
        return 1 + getEffectRarityMultiplier() * glyph.getRarity().getRank();
    }

    public double calculateExperienceRarityMultiplier(Glyph glyph) {
        return 1 + getExperienceRarityMultiplier() * glyph.getRarity().getRank();
    }

    //--------------------------------------------------------------------------------------------------------------------//
    public void setModelIncrement(int m_modelIncrement) {
        this.m_modelIncrement = m_modelIncrement;
    }

    public int getModelIncrement() {
        return this.m_modelIncrement;
    }

    //--------------------------------------------------------------------------------------------------------------------//
}
