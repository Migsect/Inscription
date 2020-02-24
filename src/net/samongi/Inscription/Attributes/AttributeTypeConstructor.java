package net.samongi.Inscription.Attributes;

import net.samongi.SamongiLib.Exceptions.InvalidConfigurationException;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.Listener;

import java.util.HashMap;
import java.util.Map;

public abstract class AttributeTypeConstructor {

    public abstract AttributeType construct(ConfigurationSection section);

    public abstract Listener getListener();

}
