package net.samongi.Inscription.Glyphs.Attributes;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.samongi.Inscription.Inscription;
import net.samongi.SamongiLib.Configuration.ConfigFile;
import net.samongi.SamongiLib.Exceptions.InvalidConfigurationException;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.PluginManager;

public class AttributeManager
{

  private Map<String, AttributeType> attributes = new HashMap<>();
  private List<AttributeTypeConstructor> attribute_constructors = new ArrayList<>();

  /**
   * Registers the AttributeType with the attribute manager
   * 
   * @param attribute
   *          The attribute type to register
   */
  public void register(AttributeType attribute)
  {
    // Adding the attribute to the mapping
    this.attributes.put(attribute.getName().toUpperCase().replace(" ", "_"), attribute);

    // Displaying that registration was a success
    Inscription.logger.fine("Registered '" + attribute.getName().toUpperCase().replace(" ", "_") + "'");
  }
  /**
   * Returns the attribute type based on the string identifier
   * 
   * @param s
   *          A string used to identify the attribute type
   * @return The attribute type if found, otherwise null
   */
  public AttributeType getAttributeType(String s)
  {
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
  public void registerConstructor(AttributeTypeConstructor constructor)
  {
    this.attribute_constructors.add(constructor);
    // Also registering the listener that handles the attribute
    PluginManager manager = Inscription.getInstance().getServer().getPluginManager();
    manager.registerEvents(constructor.getListener(), Inscription.getInstance());
  }

  public Attribute parseLoreLine(String line)
  {
    for (String k : this.attributes.keySet())
    {
      AttributeType p = this.attributes.get(k);
      Attribute result = p.parse(line);
      if (result == null) continue;
      return result;
    }
    return null;
  }

  /**
   * Parsing all the files for attributes
   * 
   * @param dir
   */
  public void parse(File dir)
  {
    if (!dir.exists()) return; // TODO error message
    if (!dir.isDirectory()) return; // TODO error message

    Inscription.logger.fine("Parsing Attribute Configurations in: '" + dir.getAbsolutePath() + "'");

    File[] files = dir.listFiles();
    for (File f : files)
    {
      Inscription.logger.fine("  Parsing: '" + f.getAbsolutePath() + "'");
      ConfigFile config = new ConfigFile(f);
      ConfigurationSection root = config.getConfig().getConfigurationSection("attributes");
      if (root == null) continue;

      Set<String> root_keys = root.getKeys(false);
      for (String k : root_keys)
      {
        Inscription.logger.fine("  - Parsing Section: '" + k + "'");
        AttributeType type = this.parseSection(root.getConfigurationSection(k));
        if (type == null) continue;
        Inscription.logger.fine("Registering: '" + type.getName() + "'");
        this.register(type);
      }
    }
  }

  /**
   * Will parse the section creating an attribute type based off the section.
   * 
   * @param section
   *          The section of a configuration to parse
   * @return An attribute Type or null if one could not be made.
   */
  public AttributeType parseSection(ConfigurationSection section)
  {
    for (AttributeTypeConstructor c : attribute_constructors)
    {
      AttributeType type = null;

      try
      {
        type = c.construct(section);
      }
      catch (InvalidConfigurationException e)
      {
        Inscription.logger.warning(e.getMessage());
      }

      if (type == null) continue;
      return type;
    }
    Inscription.logger.fine("Section '" + section.getName() + "' returned null type from parsing.");
    return null;
  }

}
