package net.samongi.Inscription.Glyphs.Attributes;

import org.bukkit.configuration.ConfigurationSection;

public interface AttributeTypeConstructor
{
  public AttributeType construct(ConfigurationSection section);
}
