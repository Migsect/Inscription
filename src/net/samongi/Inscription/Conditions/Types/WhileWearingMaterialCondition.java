package net.samongi.Inscription.Conditions.Types;

import net.samongi.Inscription.Conditions.TypeClassCondition;
import net.samongi.Inscription.TypeClass.TypeClasses.MaterialClass;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;

public class WhileWearingMaterialCondition extends TypeClassCondition<MaterialClass> {
    // ---------------------------------------------------------------------------------------------------------------//
    public WhileWearingMaterialCondition(MaterialClass materials) {
        super(materials);
    }
    public WhileWearingMaterialCondition(ConfigurationSection section) throws InvalidConfigurationException {
        super();

        String materialClassString = section.getString("material-class");
        if (materialClassString == null) {
            throw new InvalidConfigurationException("'material-class' is not defined");
        }

        MaterialClass materialClass = MaterialClass.handler.getTypeClass(materialClassString);
        if (materialClass == null) {
            throw new InvalidConfigurationException("'" + materialClassString + "' is not a valid material class");
        }

        m_typeClass = materialClass;
    }

    // ---------------------------------------------------------------------------------------------------------------//
    @Override public String getDisplay() {
        return ChatColor.YELLOW + " using " + ChatColor.BLUE + getTypeClass().getName();
    }

    // ---------------------------------------------------------------------------------------------------------------//
    public boolean equals(WhileWearingMaterialCondition other) {
        return getTypeClass().equals(other.getTypeClass());
    }

    @Override public boolean equals(Object obj) {
        if (obj instanceof WhileWearingMaterialCondition) {
            return equals((WhileWearingMaterialCondition) obj);
        }
        return false;
    }

    // ---------------------------------------------------------------------------------------------------------------//
}
