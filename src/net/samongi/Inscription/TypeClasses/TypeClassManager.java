package net.samongi.Inscription.TypeClasses;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import net.samongi.Inscription.Inscription;
import net.samongi.SamongiLib.Configuration.ConfigFile;

import org.bukkit.configuration.ConfigurationSection;

public class TypeClassManager implements Serializable
{

  private static final long serialVersionUID = 6093085594741035831L;

  private final Map<String, EntityClass> entity_classes = new HashMap<>();
  private final Map<String, MaterialClass> material_classes = new HashMap<>();

  public void registerEntityClass(EntityClass e_class)
  {
    String type_name = e_class.getTypeName();
    entity_classes.put(type_name, e_class);
  }
  public void registerMaterialClass(MaterialClass m_class)
  {
    String type_name = m_class.getTypeName();
    material_classes.put(type_name, m_class);
  }
  public EntityClass getEntityClass(String type_name)
  {
    return this.entity_classes.get(TypeClassManager.convertToTypeName(type_name));
  }
  public MaterialClass getMaterialClass(String type_name)
  {
    return this.material_classes.get(TypeClassManager.convertToTypeName(type_name));
  }

  private static String convertToTypeName(String string)
  {
    return string.replace(" ", "_").toUpperCase();
  }

  /**
   * Parses the directory and all type class files within
   * 
   * @param dir
   */
  public void parse(File dir)
  {
    if (!dir.exists()) return; // TODO error message
    if (!dir.isDirectory()) return; // TODO error message

    File[] files = dir.listFiles();
    for (File f : files)
    {
      Inscription.logger.fine("Parsing File: '" + f.toString() + "'");
      ConfigFile config = new ConfigFile(f);

      ConfigurationSection entity_classes = config.getConfig().getConfigurationSection("entity-classes");
      if (entity_classes != null)
      {
        Inscription.logger.fine("Found Enity Class Definitions:");
        Set<String> entity_classes_keys = entity_classes.getKeys(false);
        for (String k : entity_classes_keys)
        {
          Inscription.logger.fine("  Parsing Key: '" + k + "'");
          ConfigurationSection section = entity_classes.getConfigurationSection(k);
          EntityClass e_class = EntityClass.parse(section);
          if (e_class == null) continue;
          this.registerEntityClass(e_class);
        }
      }

      ConfigurationSection material_classes = config.getConfig().getConfigurationSection("material-classes");
      if (material_classes != null)
      {
        Inscription.logger.fine("Found Material Class Definitions:");
        Set<String> material_classes_keys = material_classes.getKeys(false);
        for (String k : material_classes_keys)
        {
          Inscription.logger.fine("  Parsing Key: '" + k + "'");
          ConfigurationSection section = material_classes.getConfigurationSection(k);
          MaterialClass m_class = MaterialClass.parse(section);
          if (m_class == null) continue;
          this.registerMaterialClass(m_class);
        }
      }
    }
    // Done parsing
  }

}
