package net.samongi.Inscription.Conditions.Types;

import net.samongi.Inscription.Conditions.ComparativeCondition;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;

import javax.annotation.Nonnull;

public class PlayerWhileLevelCondition extends ComparativeCondition {

    //----------------------------------------------------------------------------------------------------------------//
    public PlayerWhileLevelCondition(@Nonnull Double value, @Nonnull Mode mode) {
        super(value, mode);
    }
    public PlayerWhileLevelCondition(ConfigurationSection section) throws InvalidConfigurationException {
        super(section);
    }

    //----------------------------------------------------------------------------------------------------------------//
    @Override public String getDisplay() {
        // " while your life {is at least} X"
        return " while your life " + getMode().getDisplay() + " " + ChatColor.BLUE + String.format("%d", (int)Math.floor(getValue()));
    }

    //----------------------------------------------------------------------------------------------------------------//
    public boolean equals(PlayerWhileLevelCondition other) {
        return super.equals((ComparativeCondition)other);
    }

    @Override public boolean equals(Object obj) {
        if (obj instanceof PlayerWhileLevelCondition) {
            return equals((PlayerWhileLevelCondition) obj);
        }
        return false;
    }

    //----------------------------------------------------------------------------------------------------------------//
}
