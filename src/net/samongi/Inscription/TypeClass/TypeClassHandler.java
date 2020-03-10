package net.samongi.Inscription.TypeClass;

import net.samongi.Inscription.Inscription;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.craftbukkit.libs.jline.internal.Nullable;

import javax.annotation.Nonnull;
import java.util.*;

public class TypeClassHandler<TClass extends TypeClass> {

    //----------------------------------------------------------------------------------------------------------------//
    private static @Nonnull String convertToTypeName(@Nonnull String string) {
        return string.replace(" ", "_").toUpperCase();
    }

    //----------------------------------------------------------------------------------------------------------------//
    private Map<String, TClass> m_classes = new HashMap<>();
    private Map<Object, Set<TClass>> m_invertedClasses = new HashMap<>();

    private final String m_sectionPath;
    private final TypeClass.TypeClassParser<TClass> m_typeClassConstructor;

    //----------------------------------------------------------------------------------------------------------------//
    public TypeClassHandler(@Nonnull String sectionPath, @Nonnull TypeClass.TypeClassParser<TClass> typeClassConstructor) {
        m_sectionPath = sectionPath;
        m_typeClassConstructor = typeClassConstructor;
    }
    public TypeClassHandler(@Nonnull String sectionPath, @Nonnull TypeClass.TypeClassParser<TClass> typeClassConstructor, @Nonnull TClass globalClass) {
        m_sectionPath = sectionPath;
        m_typeClassConstructor = typeClassConstructor;
        register(globalClass);
    }
    public TypeClassHandler(@Nonnull String sectionPath, @Nonnull TypeClass.TypeClassParser<TClass> typeClassConstructor,
        TClass... globalClasses) {
        m_sectionPath = sectionPath;
        m_typeClassConstructor = typeClassConstructor;
        for (TClass globalClass : globalClasses) {
            register(globalClass);
        }
    }

    //----------------------------------------------------------------------------------------------------------------//
    public String getSectionPath() {
        return m_sectionPath;
    }

    //----------------------------------------------------------------------------------------------------------------//
    public void register(@Nonnull TClass typeClass) {
        String typeName = convertToTypeName(typeClass.getName());
        m_classes.put(typeName, typeClass);
    }

    public @Nullable TClass getTypeClass(@Nonnull String typeName) {
        return m_classes.get(convertToTypeName(typeName));
    }

    /**
     * Gets all the type classes that the object is involved in.
     * Returns null if it is involved in no material classes.
     *
     * @param object The object to check for involvement.
     * @return A set of type classes this object is involved in.
     */
    public @Nonnull Set<TClass> getInvolved(@Nonnull Object object) {
        return m_invertedClasses.getOrDefault(object, new HashSet<>());
    }

    public @Nonnull Set<TClass> getContaining(@Nonnull Object classMember, @Nonnull Set<TClass> typeClasses) {
        Set<TClass> involved = getInvolved(classMember);
        Set<TClass> containing = new HashSet<>(typeClasses);
        containing.retainAll(involved);
        return containing;
    }

    /**
     * Should be called after all registers occur. This will populate the inverted class mapping.
     */
    public void updateInvertedClasses() {
        for (TClass typeClass : m_classes.values()) {
            for (Object classMember : typeClass.getClassMembers()) {
                Set<TClass> typeClassSet = m_invertedClasses.getOrDefault(classMember, new HashSet<>());
                typeClassSet.add(typeClass);
                m_invertedClasses.put(classMember, typeClassSet);
            }
        }
    }

    //----------------------------------------------------------------------------------------------------------------//
    public boolean parse(@Nonnull ConfigurationSection section) {
        if (section == null) {
            return false;
        }

        Inscription.logger.fine("Registering '" + m_sectionPath + "' definitions:");
        Set<String> typeClassKeys = section.getKeys(false);
        for (String key : typeClassKeys) {
            Inscription.logger.finer("  Parsing Key: '" + key + "'...");
            ConfigurationSection classSection = section.getConfigurationSection(key);
            try {
                TClass typeClass = m_typeClassConstructor.parse(classSection);
                this.register(typeClass);
            }
            catch (InvalidConfigurationException exception) {
                Inscription.logger.warning(exception.getMessage());
                continue;
            }

        }
        Inscription.logger.info("Registered '" + m_sectionPath + "'");
        return true;
    }

    //----------------------------------------------------------------------------------------------------------------//

}
