package net.samongi.Inscription.TypeClass.TypeClasses;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import net.samongi.Inscription.Inscription;

import net.samongi.Inscription.TypeClass.TypeClass;
import net.samongi.Inscription.TypeClass.TypeClassHandler;
import net.samongi.Inscription.TypeClass.TypeClassManager;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.EntityType;

import javax.annotation.Nonnull;

/**
 * Referred to by attributes and other plugin systems
 * to group together different creature types.
 */
public class EntityClass extends TypeClass {

    //----------------------------------------------------------------------------------------------------------------//
    private static EntityClass getGlobalLivingEntityClass() {
        EntityClass entityClass = new EntityClass("GLOBAL_LIVING", false);
        for (EntityType type : EntityType.values()) {
            if (type.isAlive()) {
                entityClass.addEntityType(type);
            }
        }
        return entityClass;
    }

    public static final TypeClassHandler<EntityClass> handler = new TypeClassHandler<>("entity-classes", EntityClass::new, new EntityClass("GLOBAL", true),
        getGlobalLivingEntityClass());

    //----------------------------------------------------------------------------------------------------------------//
    private final Set<EntityType> m_entities = new HashSet<>();

    //----------------------------------------------------------------------------------------------------------------//
    private EntityClass(String name, boolean isGlobal) {
        super(name, isGlobal);
    }
    private EntityClass(ConfigurationSection section) throws InvalidConfigurationException {
        super(section);
    }

    //----------------------------------------------------------------------------------------------------------------//
    public @Nonnull Set<EntityType> getEntities() {
        return getClassMembers().stream().map((Object obj) -> (EntityType) obj).collect(Collectors.toSet());
    }

    public boolean containsEntity(EntityType type) {
        return m_entities.contains(type);
    }

    public void addEntityType(EntityType type) {
        m_entities.add(type);
    }

    public boolean addEntityType(String type) {
        try {
            EntityType entityType = EntityType.valueOf(type);
            addEntityType(entityType);
        }
        catch (IllegalArgumentException error) {
            return false;
        }
        return true;
    }

    //----------------------------------------------------------------------------------------------------------------//
    @Override protected TypeClassHandler<?> getTypeClassManager() {
        return handler;
    }

    @Override protected void addGlobalClassMembers() {
        for (EntityType type : EntityType.values()) {
            addEntityType(type);
        }
    }

    @Override public Set<Object> getDirectClassMembers() {
        return new HashSet<>(m_entities);
    }

    @Override protected void parse(ConfigurationSection section) {

        List<String> entityStrings = section.getStringList("entities");
        if (entityStrings != null) {
            Inscription.logger.fine("Found EntityTypes:");
            for (String entityString : entityStrings) {
                boolean valid = addEntityType(entityString);

                if (!valid) {
                    Inscription.logger.warning("'" + entityString + "' is not a valid type for EntityClass '" + getName() + "'");
                    continue;
                }
                Inscription.logger.fine(" - '" + entityString + "'");
            }
        }
    }

    //----------------------------------------------------------------------------------------------------------------//
}
