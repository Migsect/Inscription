package net.samongi.Inscription.Experience.ExperienceSource;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.craftbukkit.libs.jline.internal.Nullable;
import org.bukkit.event.Listener;

public interface ExperienceSource extends Listener {
    public boolean parseRewards(@Nullable ConfigurationSection section);
}