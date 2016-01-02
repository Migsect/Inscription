package net.samongi.Inscription.Glyphs.Attributes;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.Listener;

public interface AttributeTypeConstructor
{
  public AttributeType construct(ConfigurationSection section);
  
  public Listener getListener();
}
