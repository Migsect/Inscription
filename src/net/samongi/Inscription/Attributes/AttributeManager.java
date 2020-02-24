package net.samongi.Inscription.Attributes;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.samongi.Inscription.Inscription;
import net.samongi.SamongiLib.Configuration.ConfigFile;
import net.samongi.SamongiLib.Configuration.ConfigurationParsing;
import net.samongi.SamongiLib.Exceptions.InvalidConfigurationException;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.PluginManager;

public class AttributeManager implements ConfigurationParsing {

    private Map<String, AttributeType> attributes = new HashMap<>();
    private List<AttributeTypeConstructor> attribute_constructors = new ArrayList<>();

    /**
     * Registers the AttributeType with the attribute manager
     *
     * @param attribute The attribute type to register
     */
    public void register(AttributeType attribute) {
        // Adding the attribute to the mapping
        this.attributes.put(attribute.getName().toUpperCase().replace(" ", "_"), attribute);

        // Displaying that registration was a success
        Inscription.logger.info("Registered '" + attribute.getName().toUpperCase().replace(" ", "_") + "'");
    }
    /**
     * Returns the attribute type based on the string identifier
     *
     * @param s A string used to identify the attribute type
     * @return The attribute type if found, otherwise null
     */
    public AttributeType getAttributeType(String s) {
        return this.attributes.get(s.toUpperCase().replace(" ", "_"));
    }

    /**
     * Registers a constructor with the attribute manager
     * This enables it to be able to parse configuration sections and be able to
     * generate
     * Attribute Types through configuration
     *
     * @param constructor
     */
    public void registerConstructor(AttributeTypeConstructor constructor) {
        this.attribute_constructors.add(constructor);
        // Also registering the listener that handles the attribute
        PluginManager pluginManager = Inscription.getInstance().getServer().getPluginManager();
        pluginManager.registerEvents(constructor.getListener(), Inscription.getInstance());
    }

    public Attribute parseLoreLine(String line) {
        for (String key : this.attributes.keySet()) {
            AttributeType attributeType = this.attributes.get(key);
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
        for (String key : rootKeys) {
            if (!root.isConfigurationSection(key)) {
                Inscription.logger.warning("'{0} is not a configuration section'");
                continue;

            }

            AttributeType type = this.parseSection(root.getConfigurationSection(key));
            if (type == null) {
                Inscription.logger.warning("Null section found: '" + key + "'");
                continue;
            }

            this.register(type);
        }

        return true;
    }

    /**
     * Will parse the section creating an attribute type based off the section.
     *
     * @param section The section of a configuration to parse
     * @return An attribute Type or null if one could not be made.
     */
    public AttributeType parseSection(ConfigurationSection section) {
        for (AttributeTypeConstructor constructor : attribute_constructors) {
            AttributeType type = constructor.construct(section);

            if (type == null) {
                continue;
            }
            return type;
        }
        return null;
    }

}
