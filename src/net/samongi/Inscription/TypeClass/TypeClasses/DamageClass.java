package net.samongi.Inscription.TypeClass.TypeClasses;

import net.samongi.Inscription.Inscription;
import net.samongi.Inscription.TypeClass.TypeClass;
import net.samongi.Inscription.TypeClass.TypeClassHandler;
import net.samongi.Inscription.TypeClass.TypeClassManager;
import org.bukkit.block.Biome;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.event.entity.EntityDamageEvent;

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class DamageClass extends TypeClass {

    //----------------------------------------------------------------------------------------------------------------//
    public static final TypeClassHandler<DamageClass> handler = new TypeClassHandler<>("damage-classes", DamageClass::new, new DamageClass("GLOBAL", true));

    //----------------------------------------------------------------------------------------------------------------//
    private final Set<EntityDamageEvent.DamageCause> m_damageTypes = new HashSet<>();

    //----------------------------------------------------------------------------------------------------------------//

    private DamageClass(String name, boolean isGlobal) {
        super(name, isGlobal);
    }
    private DamageClass(ConfigurationSection section) throws InvalidConfigurationException {
        super(section);

        List<String> damageTypeStrings = section.getStringList("damage-types");
        if (damageTypeStrings != null) {
            Inscription.logger.fine("Found DaamgeTypes:");
            for (String damageTypeString : damageTypeStrings) {
                boolean valid = addDamageType(damageTypeString);
                if (!valid) {
                    Inscription.logger.warning("'" + damageTypeString + "' is not a valid type for DamageClass '" + getName() + "'");
                    continue;
                }
                Inscription.logger.fine(" - '" + damageTypeString + "'");
            }
        }
    }
    //----------------------------------------------------------------------------------------------------------------//
    @Nonnull public Set<EntityDamageEvent.DamageCause> getDamageTypes() {
        return getClassMembers().stream().map((Object obj) -> (EntityDamageEvent.DamageCause) obj).collect(Collectors.toSet());
    }

    public boolean addDamageType(@Nonnull String damageType) {
        try {
            EntityDamageEvent.DamageCause damageCause = EntityDamageEvent.DamageCause.valueOf(damageType);
            this.m_damageTypes.add(damageCause);
        }
        catch (IllegalArgumentException exception) {
            return false;
        }
        return true;
    }

    public void addDamageType(@Nonnull EntityDamageEvent.DamageCause damageType) {
        this.m_damageTypes.add(damageType);
    }

    public boolean containsDamageType(@Nonnull EntityDamageEvent.DamageCause type) {
        return this.m_damageTypes.contains(type);
    }

    //----------------------------------------------------------------------------------------------------------------//
    @Override protected TypeClassHandler<?> getTypeClassManager() {
        return handler;
    }

    @Override public void addGlobalClassMembers() {
        for (EntityDamageEvent.DamageCause type : EntityDamageEvent.DamageCause.values()) {
            addDamageType(type);
        }
    }

    @Override public Set<Object> getDirectClassMembers() {
        return new HashSet<>(m_damageTypes);
    }

    //----------------------------------------------------------------------------------------------------------------//

}
