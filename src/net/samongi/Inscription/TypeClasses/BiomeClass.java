package net.samongi.Inscription.TypeClasses;

import net.samongi.Inscription.Inscription;
import org.bukkit.block.Biome;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.entity.EntityDamageEvent;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BiomeClass {

    public static BiomeClass getGlobal(String name) {
        BiomeClass biomeClass = new BiomeClass(name);
        for (Biome type : Biome.values()) {
            biomeClass.addBiome(type);
        }
        biomeClass.m_isGlobal = true;
        return biomeClass;
    }

    public static List<BiomeClass> getContaining(Biome biome, List<BiomeClass> biomeClasses) {
        List<BiomeClass> containingClasses = new ArrayList<>();
        for (BiomeClass biomeClass : biomeClasses) {
            if (biomeClass.getBiomes().contains(biome)) {
                containingClasses.add(biomeClass);
            }
        }
        return containingClasses;
    }

    //----------------------------------------------------------------------------------------------------------------//

    private final String m_name;

    private final Set<Biome> m_biomes = new HashSet<>();

    private final Set<String> m_inherited = new HashSet<>();

    private boolean m_isGlobal = false;

    public BiomeClass(@Nonnull String name) {
        this.m_name = name;
    }

    @Nonnull public String getName() {
        return this.m_name;
    }

    @Nonnull public String getTypeName() {
        return this.m_name.replace(" ", "_").toUpperCase();
    }

    @Nonnull public Set<Biome> getBiomes() {
        TypeClassManager manager = Inscription.getInstance().getTypeClassManager();
        HashSet<Biome> returnSet = new HashSet<>(this.m_biomes);
        if (manager == null) {
            return returnSet;
        }

        for (String biomeClassKey : this.m_inherited) {
            BiomeClass biomeClass = manager.getBiomeClass(biomeClassKey);
            if (biomeClass == null) {
                continue;
            }
            returnSet.addAll(biomeClass.getBiomes());
        }

        return returnSet;
    }

    public boolean addBiome(@Nonnull String biomeString) {
        try {
            Biome biome = Biome.valueOf(biomeString);
            this.m_biomes.add(biome);
        }
        catch (IllegalArgumentException exception) {
            return false;
        }
        return true;
    }

    public void addBiome(@Nonnull Biome biome) {
        this.m_biomes.add(biome);
    }

    public void addInherited(@Nonnull String className) {
        this.m_inherited.add(className);
    }

    public boolean isGlobal() {
        if (this.m_isGlobal) {
            return true;
        }

        TypeClassManager manager = Inscription.getInstance().getTypeClassManager();
        for (String inherited : this.m_inherited) {
            if (manager.getBiomeClass(inherited) != null && manager.getBiomeClass(inherited).isGlobal()) {
                return true;
            }
        }
        return false;
    }

    public static BiomeClass parse(@Nonnull ConfigurationSection section) {
        String name = section.getString("name");
        if (name == null) {
            Inscription.logger.warning("Biome class does not have a name defined.");
            return null;
        }
        Inscription.logger.fine("Found name to be: '" + name + "'");

        BiomeClass biomeClass = new BiomeClass(name);
        List<String> biomes = section.getStringList("biomes");
        if (biomes != null) {
            Inscription.logger.fine("Found Biomes:");
            for (String biome : biomes) {
                boolean valid = biomeClass.addBiome(biome);
                if (!valid) {
                    Inscription.logger.warning("'" + biome + " is not a valid type for BiomeClass: '" + name + "'");
                    continue;
                }
                Inscription.logger.fine(" - '" + biome + "'");
            }
        }

        List<String> inherited = section.getStringList("inherited");
        if (inherited != null) {
            Inscription.logger.fine("Found Inherited:");
            for (String i : inherited) {
                Inscription.logger.fine(" - '" + i + "'");
                biomeClass.addInherited(i);
            }

        }
        return biomeClass;
    }
}
