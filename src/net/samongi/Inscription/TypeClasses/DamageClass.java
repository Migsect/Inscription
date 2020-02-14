package net.samongi.Inscription.TypeClasses;

import net.samongi.Inscription.Inscription;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.entity.EntityDamageEvent;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DamageClass implements Serializable {

    private static final long serialVersionUID = 710801571402251141L;

    public static DamageClass getGlobal(String name)
    {
        DamageClass damageClass = new DamageClass(name);
        for (EntityDamageEvent.DamageCause type : EntityDamageEvent.DamageCause.values()) {
            damageClass.addDamageType(type);
        }
        damageClass.m_isGlobal = true;
        return damageClass;
    }

    //----------------------------------------------------------------------------------------------------------------//

    private final String m_name;

    private final Set<EntityDamageEvent.DamageCause> m_damageTypes = new HashSet<>();

    private final Set<String> m_inherited = new HashSet<>();

    private boolean m_isGlobal = false;

    public DamageClass(@Nonnull String name)
    {
        this.m_name = name;
    }

    @Nonnull
    public String getName() {
        return this.m_name;
    }

    @Nonnull
    public String getTypeName()
    {
        return this.m_name.replace(" ", "_").toUpperCase();
    }

    public boolean containsMaterial(@Nonnull EntityDamageEvent.DamageCause type)
    {
        return this.m_damageTypes.contains(type);
    }

    @Nonnull
    public Set<EntityDamageEvent.DamageCause> getDamageTypes()
    {
        TypeClassManager manager = Inscription.getInstance().getTypeClassManager();
        HashSet<EntityDamageEvent.DamageCause> returnSet = new HashSet(this.m_damageTypes);
        if (manager == null) {
            return returnSet;
        }

        for (String damageClassKey : this.m_inherited) {
            DamageClass damageClass = manager.getDamageClass(damageClassKey);
            if (damageClass == null) {
                continue;
            }
            returnSet.addAll(damageClass.getDamageTypes());
        }

        return returnSet;
    }

    public boolean addDamageType(@Nonnull String damageType)
    {
        try {
            EntityDamageEvent.DamageCause damageCause = EntityDamageEvent.DamageCause.valueOf(damageType);
            this.m_damageTypes.add(damageCause);
        }
        catch (IllegalArgumentException exception) {
            return false;
        }
        return true;
    }

    public void addDamageType(@Nonnull EntityDamageEvent.DamageCause damageType)
    {
        this.m_damageTypes.add(damageType);
    }

    public void addInherited(@Nonnull String className)
    {
        this.m_inherited.add(className);
    }

    public static DamageClass parse(@Nonnull ConfigurationSection section)
    {
        String name = section.getString("name");
        if (name == null) {
            Inscription.logger.warning("Material class does not have a name defined.");
            return null;
        }
        Inscription.logger.fine("Found name to be: '" + name + "'");

        DamageClass damageClass = new DamageClass(name);
        List<String> damageTypes = section.getStringList("damage-types");
        if (damageTypes != null) {
            Inscription.logger.fine("Found Materials:");
            for (String damageType : damageTypes) {
                boolean valid = damageClass.addDamageType(damageType);
                if (!valid) {
                    Inscription.logger.warning("'" + damageType + " is not a valid type for DamageClass: '" + name + "'");
                    continue;
                }
                Inscription.logger.fine(" - '" + damageType + "'");
            }
        }

        List<String> inherited = section.getStringList("inherited");
        if (inherited != null) {
            Inscription.logger.fine("Found Inherited:");
            for (String i : inherited) {
                Inscription.logger.fine(" - '" + i + "'");
                damageClass.addInherited(i);
            }

        }
        return damageClass;
    }
    public boolean isGlobal() {
        if (this.m_isGlobal) {
            return true;
        }

        TypeClassManager manager = Inscription.getInstance().getTypeClassManager();
        for (String inherited : this.m_inherited) {
            if (manager.getMaterialClass(inherited) != null && manager.getMaterialClass(inherited).isGlobal()) {
                return true;
            }
        }
        return false;
    }
}
