package net.samongi.Inscription.TypeClass;

import net.samongi.Inscription.Inscription;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class TypeClass {

    public static interface TypeClassParser<TClass> {

        public @Nonnull TClass parse(ConfigurationSection section) throws InvalidConfigurationException;
    }

    //----------------------------------------------------------------------------------------------------------------//
    private final String m_name;
    private final boolean m_isGlobal;
    private final Set<String> m_inherited = new HashSet<>();

    //----------------------------------------------------------------------------------------------------------------//
    protected TypeClass(@Nonnull String name) {
        m_name = name;
        m_isGlobal = false;
    }

    protected TypeClass(@Nonnull String name, boolean isGlobal) {
        m_name = name;
        m_isGlobal = isGlobal;
    }

    protected TypeClass(@Nonnull ConfigurationSection section) throws InvalidConfigurationException {
        String name = section.getString("name");
        if (name == null) {
            throw new InvalidConfigurationException("Type class does not have a name defined.");
        }
        m_name = name;
        Inscription.logger.finer("Found type class name to be: '" + name + "'");

        boolean isGlobal = section.getBoolean("is-global", false);
        m_isGlobal = isGlobal;
        if (m_isGlobal) {
            addGlobalClassMembers();
        }

        List<String> inheritedClasses = section.getStringList("inherited");
        Inscription.logger.fine("Found Inherited:");
        for (String inheritedClass : inheritedClasses) {
            Inscription.logger.fine(" - '" + inheritedClass + "'");
            addInherited(inheritedClass);
        }

    }

    //----------------------------------------------------------------------------------------------------------------//
    public final @Nonnull String getName() {
        return m_name;
    }

    public final @Nonnull String getTypeName() {
        return this.m_name.replace(" ", "_").toUpperCase();
    }

    protected abstract TypeClassHandler<?> getTypeClassManager();

    public final void addInherited(@Nonnull String className) {
        this.m_inherited.add(className);
    }

    public final boolean isDirectedGlobal() {
        return m_isGlobal;
    }

    public final boolean isGlobal() {
        if (isDirectedGlobal()) {
            return true;
        }

        TypeClassHandler<?> typeClassMappings = getTypeClassManager();
        for (String inherited : this.m_inherited) {
            TypeClass typeClass = typeClassMappings.getTypeClass(inherited);
            if (typeClass != null && typeClass.isGlobal()) {
                return true;
            }
        }

        return false;
    }

    /**
     * Adds all class members that will make this class global.
     * This is called when the m)_
     */
    public abstract void addGlobalClassMembers();

    public final Set<Object> getClassMembers() {
        Set<Object> classMembers = getDirectClassMembers();
        TypeClassHandler<?> typeClassMappings = getTypeClassManager();
        for (String inherited : this.m_inherited) {
            TypeClass typeClass = typeClassMappings.getTypeClass(inherited);
            if (typeClass == null) {
                continue;
            }
            classMembers.addAll(typeClass.getClassMembers());
        }
        return classMembers;
    }

    public abstract Set<Object> getDirectClassMembers();

    //----------------------------------------------------------------------------------------------------------------//
    @Override public boolean equals(Object obj) {
        if (obj instanceof TypeClass) {
            TypeClass otherTypeClass = (TypeClass) obj;
            boolean globalEquals = m_isGlobal == otherTypeClass.m_isGlobal;
            boolean nameEquals = m_name.equals(otherTypeClass.m_name);
            boolean inheritedEquals = m_inherited.equals(otherTypeClass.m_inherited);
            return globalEquals && nameEquals && inheritedEquals;
        } return false;
    }

    @Override public int hashCode() {
        return Arrays.hashCode(new Object[]{m_inherited, m_isGlobal, m_name, getDirectClassMembers()});
    }

    @Override public String toString() {
        return "{" + getClass().getSimpleName() + "<" + m_name + ">}";
    }

    //----------------------------------------------------------------------------------------------------------------//

}
