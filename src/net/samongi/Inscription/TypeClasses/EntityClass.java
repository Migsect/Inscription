package net.samongi.Inscription.TypeClasses;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.entity.EntityType;

/**Referred to by attributes and other plugin systems
 * to group together different creature types.
 *
 */
public class EntityClass
{
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
  public boolean containsEntity(EntityType type){return this.entities.contains(type);}
  /**Gets a set of the entity types within this class. This is a set that when
   * mutated does not mutate the EntityClass itself.
   * 
   * @return A set of entities contained within this class.
   */
  public Set<EntityType> getEntities(){return new HashSet<EntityType>(this.entities);}
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
  
  /**Returns true if the class is contructed through the global methods.
   * Global signifies optimizaitons in data storage.
   * 
   * @return True if the class is a global global.
   */
  public boolean isGlobal(){return this.is_global;}
}
