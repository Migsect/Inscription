package net.samongi.Inscription.Conditions.Types;

import net.samongi.Inscription.Conditions.TypeClassCondition;
import net.samongi.Inscription.TypeClass.TypeClasses.BiomeClass;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;

public class ToBiomeCondition extends TypeClassCondition<BiomeClass> {
    // ---------------------------------------------------------------------------------------------------------------//
    public ToBiomeCondition(BiomeClass biomes) {
        super(biomes);
    }
    public ToBiomeCondition(ConfigurationSection section) throws InvalidConfigurationException {
        super();

        String biomeClassString = section.getString("biome-class");
        if (biomeClassString == null) {
            throw new InvalidConfigurationException("'biome-class' is not defined");
        }

        BiomeClass biomeClass = BiomeClass.handler.getTypeClass(biomeClassString);
        if (biomeClass == null) {
            throw new InvalidConfigurationException("'" + biomeClassString + "' is not a valid biome class");
        }

        m_typeClass = biomeClass;
    }

    // ---------------------------------------------------------------------------------------------------------------//
    @Override public String getDisplay() {
        return ChatColor.YELLOW + " to " + ChatColor.BLUE + getTypeClass().getName();
    }

    // ---------------------------------------------------------------------------------------------------------------//
    public boolean equals(ToBiomeCondition other) {
        return getTypeClass().equals(other.getTypeClass());
    }

    @Override public boolean equals(Object obj) {
        if (obj instanceof ToBiomeCondition) {
            return equals((ToBiomeCondition) obj);
        }
        return false;
    }

    // ---------------------------------------------------------------------------------------------------------------//
}
