package net.samongi.Inscription.Conditions.Types;

import net.samongi.Inscription.Conditions.TypeClassCondition;
import net.samongi.Inscription.TypeClass.TypeClasses.BiomeClass;
import net.samongi.Inscription.TypeClass.TypeClasses.EntityClass;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;

public class InBiomeCondition extends TypeClassCondition<BiomeClass> {

    // ---------------------------------------------------------------------------------------------------------------//
    public InBiomeCondition(BiomeClass biomes) {
        super(biomes);
    }
    public InBiomeCondition(ConfigurationSection section) throws InvalidConfigurationException {
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
        return ChatColor.YELLOW + " in " + ChatColor.BLUE + getTypeClass().getName();
    }

    // ---------------------------------------------------------------------------------------------------------------//
    public boolean equals(InBiomeCondition other) {
        return getTypeClass().equals(other.getTypeClass());
    }

    @Override public boolean equals(Object obj) {
        if (obj instanceof InBiomeCondition) {
            return equals((InBiomeCondition) obj);
        }
        return false;
    }

    // ---------------------------------------------------------------------------------------------------------------//
}
