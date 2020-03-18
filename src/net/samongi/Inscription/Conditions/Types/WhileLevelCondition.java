package net.samongi.Inscription.Conditions.Types;

import net.samongi.Inscription.Conditions.ComparativeCondition;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;

import javax.annotation.Nonnull;

public class WhileLevelCondition extends ComparativeCondition {

    //----------------------------------------------------------------------------------------------------------------//
    public WhileLevelCondition(@Nonnull Double value, @Nonnull Mode mode) {
        super(value, mode);
    }
    public WhileLevelCondition(ConfigurationSection section) throws InvalidConfigurationException {
        super(section);
    }

    //----------------------------------------------------------------------------------------------------------------//
    @Override public String getDisplay() {
        return " while level " + getMode().getDisplay() + " " + ChatColor.BLUE + String.format("%d", (int)Math.floor(getValue()));
    }

    //----------------------------------------------------------------------------------------------------------------//
    public boolean equals(WhileLevelCondition other) {
        return super.equals((ComparativeCondition)other);
    }

    @Override public boolean equals(Object obj) {
        if (obj instanceof WhileLevelCondition) {
            return equals((WhileLevelCondition) obj);
        }
        return false;
    }

    //----------------------------------------------------------------------------------------------------------------//
}
