package net.samongi.Inscription.TypeClasses;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

/**Referred to by attributes and othe rplugin systems
 * to group together different material types.
 *
 */
public class MaterialClass
{
  /**Returns a material class with all the materials within it.
   * 
   * @param name The name of the class to be set
   * @return A material class with all the materials in it.
   */
  public static MaterialClass getGlobal(String name)
  {
    MaterialClass m_class = new MaterialClass(name);
    for(Material m : Material.values()) m_class.addMaterial(m);
    m_class.is_global = true;
    return m_class;
  }
  
  /** The name of the class
   */
  private final String name;
  /** The set of entities for the class
   */
  private final Set<Material> materials = new HashSet<>();
  /**Determines if the class is global.
   */
  private boolean is_global = false;
  
  public MaterialClass(String name)
  {
    this.name = name;
  }
  
  /**Returns the name of this entity class.
   * This name will generally be reader-friendly
   * 
   * @return The name of the class
   */
  public String getName(){return this.name;}
  /**Returns the type name format of this class
   * This name is not user friendly but will make it instantly identifiable
   * as an entity class reference.  Used for debugging as well as storing within
   * Maps.
   * 
   * @return The type name of this class
   */
  public String getTypeName(){return this.name.replace(" ", "_").toUpperCase();}
  /**Returns true if the passed in type is contained within this class
   * Otherwise it will return false
   * 
   * @param type The type of the material to check
   * @return True if the class contains the material type
   */
  public boolean containsMaterial(Material type){return this.materials.contains(type);}
  /**Gets a set of the material types within this class. This is a set that when mutated does not
   * mutate the class itself.
   * 
   * @return A set of materials contained within this class.
   */
  public Set<Material> getMaterials(){return new HashSet<>(this.materials);}
  /**Adds the material type to the material class.
   * 
   * @param type
   */
  public void addMaterial(Material type){this.materials.add(type);}
  /**Adds the material type parsed from the string to this class
   * Will return false if it could not successfully parse the string
   * for a material
   * 
   * @param type
   * @return False if the material was not added.
   */
  public boolean addMaterial(String type)
  {
    Material e_type = Material.valueOf(type);
    if(e_type == null) return false;
    this.addMaterial(e_type);
    return true;
  }
  
  /**Returns true if the class is contructed through the global methods.
   * Global signifies optimizaitons in data storage.
   * 
   * @return True if the class is a global global.
   */
  public boolean isGlobal(){return this.is_global;}

  /**Will parse the configuration section for an material class
   * returns an material class based off the section passed in
   * 
   * @param section
   * @return
   */
  public static MaterialClass parse(ConfigurationSection section)
  {
    return null;
  }
}
