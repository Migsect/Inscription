package net.samongi.Inscription.Conditions;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface Condition {

    //----------------------------------------------------------------------------------------------------------------//
    static interface Parser {
        @Nullable Condition parse(@Nonnull ConfigurationSection section) throws InvalidConfigurationException;
    }

    public static String concatConditionDisplays(Collection<Condition> conditions)
    {
        StringBuilder aggregate = new StringBuilder();
        for (Condition condition : conditions) {
            aggregate.append(condition.getDisplay());
        }
        return aggregate.toString();
    }

    //----------------------------------------------------------------------------------------------------------------//
    String getDisplay();

    //----------------------------------------------------------------------------------------------------------------//
    @Override public String toString();

    //----------------------------------------------------------------------------------------------------------------//
}
