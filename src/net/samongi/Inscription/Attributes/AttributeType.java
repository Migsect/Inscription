package net.samongi.Inscription.Attributes;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import net.md_5.bungee.api.ChatColor;
import net.samongi.Inscription.Glyphs.Glyph;

import org.bukkit.configuration.ConfigurationSection;

import javax.annotation.Nonnull;

public abstract class AttributeType implements Serializable {

    private static final long serialVersionUID = 8046834107643613558L;

    /**
     * Transforms the section into an int map of keys to integers
     *
     * @param section The section to map
     * @return
     */
    public static Map<String, Integer> getIntMap(ConfigurationSection section) {
        Map<String, Integer> map = new HashMap<>();
        if (section == null)
            return map;
        Set<String> base_key = section.getKeys(false);
        for (String k : base_key) {
            int amount = section.getInt(k);
            if (amount == 0)
                continue;
            map.put(k, amount);
        }
        return map;
    }

    protected final String m_typeName;
    protected final String m_typeDescription;

    protected double m_rarityMultiplier;
    protected int m_modelIncrement;

    protected Map<String, Integer> m_baseExperience;
    protected Map<String, Integer> m_levelExperience;

    public AttributeType(@Nonnull String typeName, String description) {
        m_typeName = typeName;
        m_typeDescription = description;
    }

    public AttributeType(GeneralAttributeParser parser) {
        m_typeName = parser.getName();
        m_typeDescription = parser.getDescriptor();
        m_rarityMultiplier = parser.getRarityMultiplier();
        m_modelIncrement = parser.getModelIncrement();
        m_baseExperience = parser.getBaseExperience();
        m_levelExperience = parser.getLevelExperience();
    }

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
        if (reduced.startsWith(this.m_typeDescription.toLowerCase())) {
            return this.generate();
        } else {
            return null;
        }
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
    public String getName() {
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
    public String getNameDescriptor() {
        return this.m_typeDescription;
    }

    public String getDescriptionLoreLine() {
        return "" + ChatColor.YELLOW + ChatColor.ITALIC + this.getNameDescriptor() + " - " + ChatColor.RESET;
    }

    /**
     * Gets the base experience required for this attribute type
     *
     * @return The mapping of experience
     */
    public Map<String, Integer> getBaseExperience() {
        return this.m_baseExperience;
    }
    /**
     * Gets the per level experience required for this attribute type
     *
     * @return The mapping of experience
     */
    public Map<String, Integer> getLevelExperience() {
        return this.m_levelExperience;
    }

    /**
     * Sets the rarity multiplier ratio that the attribute type uses for each
     * rarity level.
     *
     * @param multiplier The multiplier value.
     */
    public void setRarityMultiplier(double multiplier) {
        this.m_rarityMultiplier = multiplier;
    }
    /**
     * Returns the rarity multiplier ratio that the attribute type uses.
     *
     * @return an amount to mutliply by for rarity
     */
    public double getRarityMultiplier() {
        return this.m_rarityMultiplier;
    }

    public void setModelIncrement(int m_modelIncrement) {
        this.m_modelIncrement = m_modelIncrement;
    }

    public int getModelIncrement() {
        return this.m_modelIncrement;
    }
}
