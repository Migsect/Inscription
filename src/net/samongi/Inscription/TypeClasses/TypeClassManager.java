package net.samongi.Inscription.TypeClasses;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import net.samongi.Inscription.Inscription;
import net.samongi.SamongiLib.Configuration.ConfigFile;

import org.bukkit.configuration.ConfigurationSection;

import javax.annotation.Nonnull;

public class TypeClassManager implements Serializable {

    private static final long serialVersionUID = 6093085594741035831L;

    private final Map<String, EntityClass> m_entityClasses = new HashMap<>();
    private final Map<String, MaterialClass> m_materialClasses = new HashMap<>();
    private final Map<String, DamageClass> m_damageClasses = new HashMap<>();

    public void registerEntityClass(EntityClass entityClass)
    {
        String typeName = entityClass.getTypeName();
        m_entityClasses.put(typeName, entityClass);
    }

    public void registerMaterialClass(MaterialClass materialClass)
    {
        String typeName = materialClass.getTypeName();
        m_materialClasses.put(typeName, materialClass);
    }

    public void registerDamageClass(DamageClass damageClass)
    {
        String typeName = damageClass.getTypeName();
        m_damageClasses.put(typeName, damageClass);
    }

    public EntityClass getEntityClass(@Nonnull String typeName)
    {
        return this.m_entityClasses.get(TypeClassManager.convertToTypeName(typeName));
    }

    public MaterialClass getMaterialClass(@Nonnull String typeName)
    {
        return this.m_materialClasses.get(TypeClassManager.convertToTypeName(typeName));
    }

    public DamageClass getDamageClass(@Nonnull String typeName)
    {
        return this.m_damageClasses.get(TypeClassManager.convertToTypeName(typeName));
    }

    private static String convertToTypeName(String string)
    {
        return string.replace(" ", "_").toUpperCase();
    }

    public void parseEntities(ConfigurationSection section)
    {
        Inscription.logger.fine("Found EntityClass Definitions:");
        Set<String> entityClassesKeys = section.getKeys(false);
        for (String key : entityClassesKeys) {
            Inscription.logger.fine("  Parsing Key: '" + key + "'");
            ConfigurationSection classSection = section.getConfigurationSection(key);
            EntityClass entityClass = EntityClass.parse(classSection);
            if (entityClass == null) {
                continue;
            }
            this.registerEntityClass(entityClass);
        }
    }
    public void parseMaterials(ConfigurationSection section)
    {
        Inscription.logger.fine("Found MaterialClass Definitions:");
        Set<String> materialClassesKeys = section.getKeys(false);
        for (String key : materialClassesKeys) {
            Inscription.logger.fine("  Parsing Key: '" + key + "'");
            ConfigurationSection classSection = section.getConfigurationSection(key);
            MaterialClass materialClass = MaterialClass.parse(classSection);
            if (materialClass == null) {
                continue;
            }
            this.registerMaterialClass(materialClass);
        }
    }

    public void parseDamageTypes(ConfigurationSection section)
    {
        Inscription.logger.fine("Found DamageClass Definitions:");
        Set<String> damageClassKeys = section.getKeys(false);
        for (String key : damageClassKeys) {
            Inscription.logger.fine("  Parsing Key: '" + key + "'");
            ConfigurationSection classSection = section.getConfigurationSection(key);
            DamageClass damageClass = DamageClass.parse(classSection);
            if (damageClass == null) {
                continue;
            }
            this.registerDamageClass(damageClass);
        }
    }
    /**
     * Parses the directory and all type class files within
     *
     * @param directory The directory that will contain configuration files for classes.
     */
    public void parse(File directory)
    {
        if (!directory.exists()) {
            return; // TODO error message}
        }
        if (!directory.isDirectory()) {
            return; // TODO error message}
        }

        File[] files = directory.listFiles();
        for (File file : files) {
            Inscription.logger.fine("Parsing File: '" + file.toString() + "'");
            ConfigFile config = new ConfigFile(file);

            ConfigurationSection entityClasses = config.getConfig().getConfigurationSection("entity-classes");
            if (entityClasses != null) {
                parseEntities(entityClasses);
            }

            ConfigurationSection materialClasses = config.getConfig().getConfigurationSection("material-classes");
            if (materialClasses != null) {
                this.parseMaterials(materialClasses);
            }

            ConfigurationSection damageClasses = config.getConfig().getConfigurationSection("damage-classes");
            if (damageClasses != null) {
                this.parseDamageTypes(damageClasses);
            }
        }
        // Done parsing
    }

}
