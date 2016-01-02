package net.samongi.Inscription.TypeClasses;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.samongi.Inscription.Inscription;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;

/**Referred to by attributes and other plugin systems
 * to group together different creature types.
 *
 */
public class EntityClass implements Serializable
{
  private static void log(String message){Inscription.log("[EntityClass] " + message);}
  private static void logDebug(String message){if(Inscription.debug()) EntityClass.log(Inscription.debug_tag + message);}
  @SuppressWarnings("unused")
  private static boolean debug(){return Inscription.debug();}
  
  private static final long serialVersionUID = 5229737283135685813L;

  /**Returns an entity class with all the entities within it.
   * 
   * @param name The name that the class will be called.
   * @return An entity class with all the entities in it.
   */
  public static EntityClass getGlobal(String name)
  {
    EntityClass e_class = new EntityClass(name);
    for(EntityType type : EntityType.values()) e_class.addEntityType(type);
    e_class.is_global = true;
    return e_class;
  }
  
  /**Returns an entity class with all living entities.
   * 
   * @param name The name that the class will be called.
   * @return An entity class with all living entities in it.
   */
  public static EntityClass getGlobalLiving(String name)
  {
    EntityClass e_class = new EntityClass(name);
    for(EntityType type : EntityType.values()) if(type.isAlive()) e_class.addEntityType(type);
    e_class.is_global = true;
    return e_class;
  }
  
  /** The name of the class
   */
  private final String name;
  /** The set of entities for the class
  */
  private final Set<EntityType> entities = new HashSet<>();
  
  private final Set<String> inherited = new HashSet<>();
  /**Determines if the class is global.
   */
  private boolean is_global = false;
  
  public EntityClass(String name)
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
   * @param type The type of the entity to check
   * @return True if the class contains the entity type
   */
  public boolean containsEntity(EntityType type)
  {
    return this.entities.contains(type);
  }
  /**Gets a set of the entity types within this class. This is a set that when
   * mutated does not mutate the EntityClass itself.
   * 
   * @return A set of entities contained within this class.
   */
  public Set<EntityType> getEntities()
  {
    TypeClassManager manager = Inscription.getInstance().getTypeClassManager();
    HashSet<EntityType> ret_set = new HashSet<>(this.entities);
    if(manager != null) for(String t : this.inherited)
    {
      EntityClass e_class = manager.getEntityClass(t);
      if(e_class == null) continue;
      ret_set.addAll(e_class.getEntities());
    }
    return ret_set;
  }
  /**Adds the entity type to this entity class.
   * 
   * @param type
   */
  public void addEntityType(EntityType type){this.entities.add(type);}
  /**Adds the entity type parsed from the string to this class
   * Will return false if it could not successfully parse the string for
   * an entity.
   * 
   * @param type
   * @return False if the entity type was not added
   */
  public boolean addEntityType(String type)
  {
    EntityType e_type = EntityType.valueOf(type);
    if(e_type == null) return false;
    this.addEntityType(e_type);
    return true;
  }
  /**Adds a class to be inherited by this class.
   * 
   * @param class_name
   */
  public void addInheritied(String class_name){this.inherited.add(class_name);}
  
  /**Returns true if the class is contructed through the global methods.
   * Global signifies optimizaitons in data storage.
   * 
   * @return True if the class is a global global.
   */
  public boolean isGlobal()
  {
    if(this.is_global) return true;
    
    TypeClassManager manager = Inscription.getInstance().getTypeClassManager();
    for(String i : this.inherited) if(manager.getEntityClass(i) != null && manager.getEntityClass(i).isGlobal()) return true;
    return false;
  }
  
  /**Will parse the configuration section for an entity class
   * returns an entity class based off the section passed in
   * 
   * @param section
   * @return
   */
  public static EntityClass parse(ConfigurationSection section)
  {
    String name = section.getString("name");
    if(name == null) return null; // TODO error message
    EntityClass.logDebug("Found name to be: '" + name + "'");
    
    EntityClass e_class = new EntityClass(name);
    List<String> entities = section.getStringList("entities");
    if(entities != null) EntityClass.logDebug("Found EntityTypes:");;
    if(entities != null) for(String t : entities)
    {
      EntityType type = EntityType.valueOf(t);
      if(type == null) continue;
      EntityClass.logDebug(" - '" + type + "'");;
      e_class.addEntityType(type);
    }
    
    List<String> inherited = section.getStringList("inherited");
    if(inherited != null) EntityClass.logDebug("Found Inherited:");
    if(inherited != null) for(String i : inherited)
    {
      EntityClass.logDebug(" - '" + i + "'");;
      e_class.addInheritied(i);
    }
    
    
    return e_class;
  }
}
