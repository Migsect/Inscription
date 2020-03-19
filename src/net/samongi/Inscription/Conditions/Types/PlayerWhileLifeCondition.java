package net.samongi.Inscription.Conditions.Types;

import net.samongi.Inscription.Conditions.ComparativeCondition;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;

import javax.annotation.Nonnull;

public class PlayerWhileLifeCondition extends ComparativeCondition {
    //----------------------------------------------------------------------------------------------------------------//
    public PlayerWhileLifeCondition(@Nonnull Double value, @Nonnull Mode mode) {
        super(value, mode);
    }
    public PlayerWhileLifeCondition(ConfigurationSection section) throws InvalidConfigurationException {
        super(section);
    }

    //----------------------------------------------------------------------------------------------------------------//
    @Override public String getDisplay() {
        return " while life " + getMode().getDisplay() + " " + ChatColor.BLUE + String.format("%d", (int)Math.floor(getValue()));
    }

    //----------------------------------------------------------------------------------------------------------------//
    public boolean equals(PlayerWhileLifeCondition other) {
        return super.equals((ComparativeCondition)other);
    }

    @Override public boolean equals(Object obj) {
        if (obj instanceof PlayerWhileLifeCondition) {
            return equals((PlayerWhileLifeCondition) obj);
        }
        return false;
    }

    //----------------------------------------------------------------------------------------------------------------//
}
