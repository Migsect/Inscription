package net.samongi.Inscription.Attributes;

import java.io.File;
import java.util.*;

import net.samongi.Inscription.Conditions.Condition;
import net.samongi.Inscription.Inscription;
import net.samongi.SamongiLib.Configuration.ConfigFile;
import net.samongi.SamongiLib.Configuration.ConfigurationParsing;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.plugin.PluginManager;

import javax.annotation.Nonnull;

public class AttributeManager implements ConfigurationParsing {

    //----------------------------------------------------------------------------------------------------------------//
    private Map<String, AttributeType> m_attributes = new HashMap<>();
    private Map<String, List<AttributeType>> m_attributeInstances = new HashMap<>();
    private List<AttributeTypeFactory> m_attributeConstructors = new ArrayList<>();

    private Map<String, Condition.Parser> m_conditionParsers = new HashMap<>();

    //----------------------------------------------------------------------------------------------------------------//

    /**
     * Registers the AttributeType with the attribute manager
     *
     * @param attribute The attribute type to register
     */
    public void register(AttributeType attribute) {
        // Adding the attribute to the mapping
        m_attributes.put(attribute.getTypeName().toUpperCase().replace(" ", "_"), attribute);
        addAttributeTypeToImplementionType(attribute.getImplementationType(), attribute);

        // Displaying that registration was a success
        Inscription.logger.info("Registered '" + attribute.getTypeName().toUpperCase().replace(" ", "_") + "'");
    }
    /**
     * Returns the attribute type based on the string identifier
     *
     * @param s A string used to identify the attribute type
     * @return The attribute type if found, otherwise null
     */
    public AttributeType getAttributeType(String s) {
        return this.m_attributes.get(s.toUpperCase().replace(" ", "_"));
    }

    public List<AttributeType> getAttributeTypesOfImplementationType(String implementationType) {
        if (m_attributeInstances.containsKey(implementationType)) {
            return m_attributeInstances.get(implementationType);

        }
        List<AttributeType> newList = new ArrayList<>();
        m_attributeInstances.put(implementationType, newList);
        return newList;
    }

    public void addAttributeTypeToImplementionType(String implementionType, AttributeType attributeType) {
        getAttributeTypesOfImplementationType(implementionType).add(attributeType);
    }

    /**
     * Registers a constructor with the attribute manager
     * This enables it to be able to parse configuration sections and be able to
     * generate
     * Attribute Types through configuration
     *
     * @param constructor
     */
    public void registerConstructor(AttributeTypeFactory constructor) {
        this.m_attributeConstructors.add(constructor);
        // Also registering the listener that handles the attribute
        PluginManager pluginManager = Inscription.getInstance().getServer().getPluginManager();
        pluginManager.registerEvents(constructor.getListener(), Inscription.getInstance());
    }

    public Attribute parseLoreLine(String line) {
        for (String key : this.m_attributes.keySet()) {
            AttributeType attributeType = this.m_attributes.get(key);
            Attribute result = attributeType.parse(line);
            if (result == null) {
                continue;
            }
            return result;
        }
        return null;
    }

    @Override public boolean parseConfigFile(File file, ConfigFile config) {
        Inscription.logger.info("Parsing Attribute Configurations in: '" + file.getAbsolutePath() + "'");
        ConfigurationSection root = config.getConfig();// .getConfigurationSection("attributes");

        Set<String> rootKeys = root.getKeys(false);
        for (String configKey : rootKeys) {
            if (!root.isConfigurationSection(configKey)) {
                Inscription.logger.warning("'" + configKey + "' is not a valid attribute section");
                continue;

            }

            AttributeType type = parseSection(root.getConfigurationSection(configKey), configKey);
            if (type == null) {
                Inscription.logger.warning("Could not parse attribute section '" + configKey + "'");
                continue;
            }

            register(type);
        }

        return true;
    }

    /**
     * Will parse the section creating an attribute type based off the section.
     *
     * @param section The section of a configuration to parse
     * @return An attribute Type or null if one could not be made.
     */
    public AttributeType parseSection(ConfigurationSection section, String configKey) {
        for (AttributeTypeFactory constructor : m_attributeConstructors) {
            AttributeType type = null;
            try {
                type = constructor.checkTypeAndConstruct(section);
            }
            catch (InvalidConfigurationException exception) {
                Inscription.logger.warning("Attribute condifiguration error for key '" + configKey + "': " + exception.getMessage());
                continue;
            }
            if (type == null) {
                continue;
            }
            return type;
        }
        return null;
    }
    //----------------------------------------------------------------------------------------------------------------//
    public void registerConditionParser(@Nonnull String conditionId, @Nonnull Condition.Parser parser) {
        m_conditionParsers.put(conditionId, parser);
    }

    public Set<Condition> parseConditions(@Nonnull ConfigurationSection section) throws InvalidConfigurationException {
        Set<Condition> conditions = new HashSet<>();

        Set<String> keys = section.getKeys(false);
        for (String key : keys) {
            Condition.Parser parser = m_conditionParsers.get(key);
            if (parser == null) {
                throw new InvalidConfigurationException("Condition '" + key + "' does not have a parser defined");
            }

            ConfigurationSection conditionSection = section.getConfigurationSection(key);
            if (conditionSection == null) {
                throw new InvalidConfigurationException("Condition '" + key + "' does not have a valid section");
            }

            conditions.add(parser.parse(conditionSection));
        }
        return conditions;
    }
    //----------------------------------------------------------------------------------------------------------------//
}
