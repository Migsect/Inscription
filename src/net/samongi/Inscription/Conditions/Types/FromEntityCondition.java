package net.samongi.Inscription.Conditions.Types;

import net.samongi.Inscription.Conditions.TypeClassCondition;
import net.samongi.Inscription.TypeClass.TypeClasses.EntityClass;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;

public class FromEntityCondition extends TypeClassCondition<EntityClass> {
    // ---------------------------------------------------------------------------------------------------------------//
    public FromEntityCondition(EntityClass entities) {
        super(entities);
    }
    public FromEntityCondition(ConfigurationSection section) throws InvalidConfigurationException {
        super();

        String entityClassString = section.getString("entity-class");
        if (entityClassString == null) {
            throw new InvalidConfigurationException("'entity-class' is not defined");
        }

        EntityClass entityClass = EntityClass.handler.getTypeClass(entityClassString);
        if (entityClass == null) {
            throw new InvalidConfigurationException("'" + entityClassString + "' is not a valid entity class");
        }

        m_typeClass = entityClass;
    }
    // ---------------------------------------------------------------------------------------------------------------//
    @Override public String getDisplay() {
        return ChatColor.YELLOW + " from " + ChatColor.BLUE + getTypeClass().getName();
    }

    // ---------------------------------------------------------------------------------------------------------------//
    public boolean equals(TargetEntityCondition other) {
        return getTypeClass().equals(other.getTypeClass());
    }

    @Override public boolean equals(Object obj) {
        if (obj instanceof TargetEntityCondition) {
            return equals((TargetEntityCondition) obj);
        }
        return false;
    }

    // ---------------------------------------------------------------------------------------------------------------//
}
