package net.samongi.Inscription.Attributes;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.craftbukkit.libs.jline.internal.Nullable;
import org.bukkit.event.Listener;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

public abstract class AttributeTypeFactory {

    //----------------------------------------------------------------------------------------------------------------//
    public final @Nullable AttributeType checkTypeAndConstruct(@Nonnull ConfigurationSection section)
        throws InvalidConfigurationException {
        String type = section.getString("type");
        if (type == null) {
            throw new InvalidConfigurationException("'type' is not defined");
        }
        if (!type.equals(getAttributeTypeId())) {
            return null;
        }
        return construct(section);
    }

    public abstract @Nonnull AttributeType construct(@Nonnull ConfigurationSection section) throws InvalidConfigurationException;

    //----------------------------------------------------------------------------------------------------------------//
    public abstract Listener getListener();

    //----------------------------------------------------------------------------------------------------------------//
    public abstract @Nonnull String getAttributeTypeId();

    //----------------------------------------------------------------------------------------------------------------//
}
