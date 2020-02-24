package net.samongi.Inscription.Attributes;

import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.Map;

public class GeneralAttributeParser {

    //--------------------------------------------------------------------------------------------------------------------//
    private final ConfigurationSection m_section;
    private final String m_typeIdentifier;

    private String m_type;
    private String m_name;
    private String m_descriptor;
    private double m_rarityMultiplier;
    private int m_modelIncrement;

    private Map<String, Integer> m_baseExperience = new HashMap<>();
    private Map<String, Integer> m_levelExperience = new HashMap<>();

    //--------------------------------------------------------------------------------------------------------------------//
    public GeneralAttributeParser(ConfigurationSection section, String typeIdentifier) {
        m_section = section;
        m_typeIdentifier = typeIdentifier;
    }

    //--------------------------------------------------------------------------------------------------------------------//
    public boolean checkType() {
        m_type = m_section.getString("type");
        return m_type != null && m_type.toUpperCase().equals(m_typeIdentifier);
    }

    //--------------------------------------------------------------------------------------------------------------------//
    /**
     * Loads all the information needed for validation.
     * If anything fails to load then false will be returned.
     */
    public boolean loadInfo() {
        m_name = m_section.getString("name");
        if (m_name == null) {
            return false;
        }

        m_descriptor = m_section.getString("descriptor");
        if (m_descriptor == null) {
            return false;
        }

        m_rarityMultiplier = m_section.getDouble("rarity-multiplier", 1);
        m_modelIncrement = m_section.getInt("model-increment", 0);

        if (m_section.isConfigurationSection("base-experience")) {
            m_baseExperience = AttributeType.getIntMap(m_section.getConfigurationSection("base-experience"));
        }
        if (m_section.isConfigurationSection("level-experience")) {
            m_levelExperience = AttributeType.getIntMap(m_section.getConfigurationSection("level-experience"));
        }

        return true;
    }
    //--------------------------------------------------------------------------------------------------------------------//
    public String getType() {
        return m_typeIdentifier;
    }
    public String getName() {
        return m_name;
    }
    public String getDescriptor() {
        return m_descriptor;
    }
    public double getRarityMultiplier() {
        return m_rarityMultiplier;
    }
    public int getModelIncrement() {
        return m_modelIncrement;
    }
    public Map<String, Integer> getBaseExperience() {
        return m_baseExperience;
    }
    public Map<String, Integer> getLevelExperience() {
        return m_levelExperience;
    }

    //--------------------------------------------------------------------------------------------------------------------//
}
