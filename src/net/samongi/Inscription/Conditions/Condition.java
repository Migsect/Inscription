package net.samongi.Inscription.Conditions;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface Condition {

    //----------------------------------------------------------------------------------------------------------------//
    static interface Parser {
        @Nullable Condition parse(@Nonnull ConfigurationSection section) throws InvalidConfigurationException;
    }

    //----------------------------------------------------------------------------------------------------------------//
    String getDisplay();

    //----------------------------------------------------------------------------------------------------------------//
}
