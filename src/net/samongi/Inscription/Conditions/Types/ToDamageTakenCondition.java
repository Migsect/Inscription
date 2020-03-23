package net.samongi.Inscription.Conditions.Types;

import net.samongi.Inscription.Conditions.TypeClassCondition;
import net.samongi.Inscription.TypeClass.TypeClasses.DamageClass;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;

public class ToDamageTakenCondition extends TypeClassCondition<DamageClass> {

    // ---------------------------------------------------------------------------------------------------------------//
    public ToDamageTakenCondition(DamageClass damageTypes) {
        super(damageTypes);
    }
    public ToDamageTakenCondition(ConfigurationSection section) throws InvalidConfigurationException {
        super();

        String damageClassString = section.getString("damage-class");
        if (damageClassString == null) {
            throw new InvalidConfigurationException("'damage-class' is not defined");
        }

        DamageClass damageClass = DamageClass.handler.getTypeClass(damageClassString);
        if (damageClass == null) {
            throw new InvalidConfigurationException("'" + damageClassString + "' is not a valid damage class");
        }

        m_typeClass = damageClass;
    }

    // ---------------------------------------------------------------------------------------------------------------//
    @Override public String getDisplay() {
        return ChatColor.YELLOW + " to " + ChatColor.BLUE + getTypeClass().getName() + ChatColor.YELLOW;
    }

    // ---------------------------------------------------------------------------------------------------------------//
    public boolean equals(ToDamageTakenCondition other) {
        return getTypeClass().equals(other.getTypeClass());
    }

    @Override public boolean equals(Object obj) {
        if (obj instanceof ToDamageTakenCondition) {
            return equals((ToDamageTakenCondition) obj);
        }
        return false;
    }

    // ---------------------------------------------------------------------------------------------------------------//
}
