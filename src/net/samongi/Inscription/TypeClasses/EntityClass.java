package net.samongi.Inscription.TypeClasses;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.samongi.Inscription.Inscription;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;

/**
 * Referred to by attributes and other plugin systems
 * to group together different creature types.
 */
public class EntityClass {

    /**
     * Returns an entity class with all the entities within it.
     *
     * @param name The name that the class will be called.
     * @return An entity class with all the entities in it.
     */
    public static EntityClass getGlobal(String name)
    {
        EntityClass entityClass = new EntityClass(name);
        for (EntityType type : EntityType.values()) {
            entityClass.addEntityType(type);
        }
        entityClass.isGlobal = true;
        return entityClass;
    }

    /**
     * Returns an entity class with all living entities.
     *
     * @param name The name that the class will be called.
     * @return An entity class with all living entities in it.
     */
    public static EntityClass getGlobalLiving(String name)
    {
        EntityClass entityClass = new EntityClass(name);
        for (EntityType type : EntityType.values()) {
            if (type.isAlive()) {
                entityClass.addEntityType(type);
            }
        }
        entityClass.isGlobal = true;
        return entityClass;
    }

    /**
     * The name of the class
     */
    private final String name;
    /**
     * The set of entities for the class
     */
    private final Set<EntityType> entities = new HashSet<>();

    private final Set<String> inherited = new HashSet<>();
    /**
     * Determines if the class is global.
     */
    private boolean isGlobal = false;

    public EntityClass(String name)
    {
        this.name = name;
    }

    /**
     * Returns the name of this entity class.
     * This name will generally be reader-friendly
     *
     * @return The name of the class
     */
    public String getName()
    {
        return this.name;
    }
    /**
     * Returns the type name format of this class
     * This name is not user friendly but will make it instantly identifiable
     * as an entity class reference. Used for debugging as well as storing within
     * Maps.
     *
     * @return The type name of this class
     */
    public String getTypeName()
    {
        return this.name.replace(" ", "_").toUpperCase();
    }
    /**
     * Returns true if the passed in type is contained within this class
     * Otherwise it will return false
     *
     * @param type The type of the entity to check
     * @return True if the class contains the entity type
     */
    public boolean containsEntity(EntityType type)
    {
        return this.entities.contains(type);
    }
    /**
     * Gets a set of the entity types within this class. This is a set that when
     * mutated does not mutate the EntityClass itself.
     *
     * @return A set of entities contained within this class.
     */
    public Set<EntityType> getEntities()
    {
        TypeClassManager manager = Inscription.getInstance().getTypeClassManager();
        HashSet<EntityType> returnSet = new HashSet<>(this.entities);
        if (manager == null) {
            return returnSet;
        }
        for (String type : this.inherited) {
            EntityClass entityClass = manager.getEntityClass(type);
            if (entityClass == null) {
                continue;
            }
            returnSet.addAll(entityClass.getEntities());
        }
        return returnSet;
    }
    /**
     * Adds the entity type to this entity class.
     *
     * @param type
     */
    public void addEntityType(EntityType type)
    {
        this.entities.add(type);
    }

    /**
     * Adds the entity type parsed from the string to this class
     * Will return false if it could not successfully parse the string for
     * an entity.
     *
     * @param type
     * @return False if the entity type was not added
     */
    public boolean addEntityType(String type)
    {
        EntityType entityType;
        try {
            entityType = EntityType.valueOf(type);
        }
        catch (IllegalArgumentException error) {
            Inscription.logger.warning("Could not find entity type for: " + type);
            return false;
        }
        this.addEntityType(entityType);
        return true;
    }

    /**
     * Adds a class to be inherited by this class.
     *
     * @param class_name
     */
    public void addInherited(String class_name)
    {
        this.inherited.add(class_name);
    }

    /**
     * Returns true if the class is contructed through the global methods.
     * Global signifies optimizaitons in data storage.
     *
     * @return True if the class is a global global.
     */
    public boolean isGlobal()
    {
        if (this.isGlobal) return true;

        TypeClassManager manager = Inscription.getInstance().getTypeClassManager();
        for (String i : this.inherited)
            if (manager.getEntityClass(i) != null && manager.getEntityClass(i).isGlobal()) return true;
        return false;
    }

    /**
     * Will parse the configuration section for an entity class
     * returns an entity class based off the section passed in
     *
     * @param section
     * @return
     */
    public static EntityClass parse(ConfigurationSection section)
    {
        String name = section.getString("name");
        if (name == null) {
            Inscription.logger.warning("Material class does not have a name defined.");
            return null;
        }
        Inscription.logger.fine("Found name to be: '" + name + "'");

        EntityClass entityClass = new EntityClass(name);
        List<String> entities = section.getStringList("entities");
        if (entities != null) {
            Inscription.logger.fine("Found EntityTypes:");
            for (String key : entities) {
                EntityType type;
                try {
                    type = EntityType.valueOf(key);
                }
                catch (IllegalArgumentException error) {
                    Inscription.logger.warning(" - Could not find entity type for: " + key);
                    continue;
                }
                Inscription.logger.fine(" - '" + type + "'");

                entityClass.addEntityType(type);
            }
        }

        List<String> inherited = section.getStringList("inherited");
        if (inherited != null) {
            Inscription.logger.fine("Found Inherited:");
            for (String inheritedKey : inherited) {
                Inscription.logger.fine(" - '" + inheritedKey + "'");
                entityClass.addInherited(inheritedKey);
            }
        }

        return entityClass;
    }
}
