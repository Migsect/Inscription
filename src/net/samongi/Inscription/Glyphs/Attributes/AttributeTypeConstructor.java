package net.samongi.Inscription.Glyphs.Attributes;

import net.samongi.SamongiLib.Exceptions.InvalidConfigurationException;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.Listener;

public abstract class AttributeTypeConstructor {

    public abstract AttributeType construct(ConfigurationSection section) throws InvalidConfigurationException;

    public abstract Listener getListener();

}
