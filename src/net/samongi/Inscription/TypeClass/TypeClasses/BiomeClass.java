package net.samongi.Inscription.TypeClass.TypeClasses;

import net.samongi.Inscription.Inscription;
import net.samongi.Inscription.TypeClass.TypeClass;
import net.samongi.Inscription.TypeClass.TypeClassHandler;
import org.bukkit.block.Biome;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class BiomeClass extends TypeClass {
    //----------------------------------------------------------------------------------------------------------------//
    private static final BiomeClass GLOBAL_CLASS = new BiomeClass("GLOBAL", true);
    public static final TypeClassHandler<BiomeClass> handler = new TypeClassHandler<>("biome-classes", BiomeClass::new, GLOBAL_CLASS);

    //----------------------------------------------------------------------------------------------------------------//

    private final Set<Biome> m_biomes = new HashSet<>();

    //----------------------------------------------------------------------------------------------------------------//
    private BiomeClass(String name, boolean isGlobal) {
        super(name, isGlobal);
    }
    private BiomeClass(ConfigurationSection section) throws InvalidConfigurationException {
        super(section);

        List<String> biomeStrings = section.getStringList("biomes");
        if (!biomeStrings.isEmpty()) {
            Inscription.logger.fine("Found Biomes:");
            for (String biomeString : biomeStrings) {
                boolean valid = addBiome(biomeString);
                if (!valid) {
                    Inscription.logger.warning("'" + biomeString + "' is not a valid type for BiomeClass '" + getName() + "'");
                    continue;
                }
                Inscription.logger.fine(" - '" + biomeString + "'");
            }
        }
    }

    //----------------------------------------------------------------------------------------------------------------//
    @Nonnull public Set<Biome> getBiomes() {
        return getClassMembers().stream().map((Object obj) -> (Biome) obj).collect(Collectors.toSet());
    }

    public boolean addBiome(@Nonnull String biomeString) {
        try {
            Biome biome = Biome.valueOf(biomeString);
            addBiome(biome);
        }
        catch (IllegalArgumentException exception) {
            return false;
        }
        return true;
    }

    public void addBiome(@Nonnull Biome biome) {
        Inscription.logger.finest("Biomes " + m_biomes + " Biome " + biome);
        m_biomes.add(biome);
    }

    //----------------------------------------------------------------------------------------------------------------//
    @Override protected TypeClassHandler<?> getTypeClassManager() {
        return BiomeClass.handler;
    }

    @Override public void addGlobalClassMembers() {
        for (Biome type : Biome.values()) {
            addBiome(type);
        }
    }

    @Override public Set<Object> getDirectClassMembers() {
        return new HashSet<>(m_biomes);
    }

    //----------------------------------------------------------------------------------------------------------------//
}
