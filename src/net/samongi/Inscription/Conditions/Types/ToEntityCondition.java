package net.samongi.Inscription.Conditions.Types;

import net.samongi.Inscription.Conditions.TypeClassCondition;
import net.samongi.Inscription.TypeClass.TypeClass;
import net.samongi.Inscription.TypeClass.TypeClasses.BlockClass;
import net.samongi.Inscription.TypeClass.TypeClasses.EntityClass;
import net.samongi.Inscription.TypeClass.TypeClasses.MaterialClass;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;

public class ToEntityCondition extends TypeClassCondition<EntityClass> {

    // ---------------------------------------------------------------------------------------------------------------//
    public ToEntityCondition(EntityClass entities) {
        super(entities);
    }
    public ToEntityCondition(ConfigurationSection section) throws InvalidConfigurationException {
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
        return ChatColor.YELLOW + " to " + ChatColor.BLUE + getTypeClass().getName();
    }

    // ---------------------------------------------------------------------------------------------------------------//
    public boolean equals(ToEntityCondition other) {
        return getTypeClass().equals(other.getTypeClass());
    }

    @Override public boolean equals(Object obj) {
        if (obj instanceof ToEntityCondition) {
            return equals((ToEntityCondition) obj);
        }
        return false;
    }

    // ---------------------------------------------------------------------------------------------------------------//
}
