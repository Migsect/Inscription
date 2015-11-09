package net.samongi.Inscription.Glyphs.Attributes;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.samongi.Inscription.Inscription;
import net.samongi.SamongiLib.Configuration.ConfigFile;

import org.bukkit.configuration.ConfigurationSection;

public class AttributeManager
{
  public static void log(String message){Inscription.log("[AttributeManager] " + message);}
  public static void logDebug(String message){if(Inscription.debug()) AttributeManager.log(Inscription.debug_tag + message);}
  public static boolean debug(){return Inscription.debug();}
  
  private Map<String, AttributeType> attributes = new HashMap<>();
  private List<AttributeTypeConstructor> attribute_constructors = new ArrayList<>();
  
  public void register(AttributeType attribute)
  {
    this.attributes.put(attribute.getName().toUpperCase().replace(" ", "_"), attribute);
    AttributeManager.logDebug("Registered '" + attribute.getName().toUpperCase().replace(" ", "_") + "'");
  }
  public AttributeType getAttributeType(String s){return this.attributes.get(s.toUpperCase().replace(" ", "_"));}
  
  /**Registers a constructor with the attribute manager
   * This enables it to be able to parse configuration sections and be able to generate
   * Attribute Types through configuration
   * 
   * @param constructor
   */
  public void registerConstructor(AttributeTypeConstructor constructor){this.attribute_constructors.add(constructor);}
  
  public Attribute parseLoreLine(String line)
  {
    for(String k : this.attributes.keySet())
    {
      AttributeType p = this.attributes.get(k);
      Attribute result = p.parse(line);
      if(result == null) continue;
      return result;
    }
    return null;
  }
  
  /**Parsing all the files for attributes
   * 
   * @param dir
   */
  public void parse(File dir)
  {
    if(!dir.exists()) return; // TODO error message
    if(!dir.isDirectory()) return; // TODO error message
    
    AttributeManager.logDebug("Parsing Attribute Configurations in: '" + dir.getAbsolutePath() + "'");
    
    File[] files = dir.listFiles();
    for(File f : files)
    {
      AttributeManager.logDebug("  Parsing: '" + f.getAbsolutePath() + "'");
      ConfigFile config = new ConfigFile(f);
      ConfigurationSection root = config.getConfig().getConfigurationSection("attributes");
      if(root == null) continue;
      
      Set<String> root_keys = root.getKeys(false);
      for(String k : root_keys)
      {
        AttributeManager.logDebug("  - Parsing Section: '" + k + "'");
        AttributeType type = this.parseSection(root.getConfigurationSection(k));
        if(type == null) continue;
        AttributeManager.logDebug("Registering: '" + type.getName() + "'");
        this.register(type);
      }
    }
  }
  
  /**Will parse the section creating an attribute type based off the section.
   * 
   * @param section The section of a configuration to parse
   * @return An attribute Type or null if one could not be made.
   */
  public AttributeType parseSection(ConfigurationSection section)
  {
    for(AttributeTypeConstructor c : attribute_constructors)
    {
      AttributeType type = c.construct(section);
      if(type == null) continue;
      return type;
    }
    AttributeManager.logDebug("Section '" + section.getName() + "' returned null type from parsing.");
    return null;
  }
  
}
