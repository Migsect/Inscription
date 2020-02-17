package net.samongi.Inscription.TypeClasses;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import net.samongi.Inscription.Inscription;
import net.samongi.SamongiLib.Configuration.ConfigFile;

import net.samongi.SamongiLib.Configuration.ConfigurationParsing;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import javax.annotation.Nonnull;

public class TypeClassManager implements ConfigurationParsing {

    private static final String ENTITY_SECTION_KEY = "entity-classes";
    private static final String MATERIAL_SECTION_KEY = "material-classes";
    private static final String DAMAGE_SECTION_KEY = "damage-classes";

    private final Map<String, EntityClass> m_entityClasses = new HashMap<>();
    private final Map<String, MaterialClass> m_materialClasses = new HashMap<>();
    private final Map<String, DamageClass> m_damageClasses = new HashMap<>();

    public void registerEntityClass(EntityClass entityClass) {
        String typeName = entityClass.getTypeName();
        m_entityClasses.put(typeName, entityClass);
    }

    public void registerMaterialClass(MaterialClass materialClass) {
        String typeName = materialClass.getTypeName();
        m_materialClasses.put(typeName, materialClass);
    }

    public void registerDamageClass(DamageClass damageClass) {
        String typeName = damageClass.getTypeName();
        m_damageClasses.put(typeName, damageClass);
    }

    public EntityClass getEntityClass(@Nonnull String typeName) {
        return this.m_entityClasses.get(TypeClassManager.convertToTypeName(typeName));
    }

    public MaterialClass getMaterialClass(@Nonnull String typeName) {

        return this.m_materialClasses.get(TypeClassManager.convertToTypeName(typeName));
    }

    public DamageClass getDamageClass(@Nonnull String typeName) {

        return this.m_damageClasses.get(TypeClassManager.convertToTypeName(typeName));
    }

    private static String convertToTypeName(String string) {

        return string.replace(" ", "_").toUpperCase();
    }

    public boolean parseEntities(ConfigurationSection section) {
        if (section == null) {
            return false;
        }

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
        return true;
    }
    public boolean parseMaterials(ConfigurationSection section) {
        if (section == null) {
            return false;
        }

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
        return true;
    }

    public boolean parseDamageTypes(ConfigurationSection section) {
        if (section == null) {
            return false;
        }

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
        return true;
    }

    @Override public boolean parseConfigFile(@Nonnull File file, @Nonnull ConfigFile config) {
        Inscription.logger.info("Parsing TypeClass Configurations in: '" + file.getAbsolutePath() + "'");
        FileConfiguration root = config.getConfig();

        boolean parsedSomething = false;
        if (parseEntities(root.getConfigurationSection(ENTITY_SECTION_KEY))) {
            Inscription.logger.info(String.format(" - Registered: '%s'", ENTITY_SECTION_KEY));
            parsedSomething = true;
        }
        if (parseMaterials(root.getConfigurationSection(MATERIAL_SECTION_KEY))) {
            Inscription.logger.info(String.format(" - Registered: '%s'", MATERIAL_SECTION_KEY));
            parsedSomething = true;
        }
        if (parseDamageTypes(root.getConfigurationSection(DAMAGE_SECTION_KEY))) {
            Inscription.logger.info(String.format(" - Registered: '%s'", DAMAGE_SECTION_KEY));
            parsedSomething = true;
        }

        if (!parsedSomething) {
            Inscription.logger.warning(String.format("Didn't find anything to parse in '%s'", file.getAbsolutePath()));
        }
        return parsedSomething;
    }

    //    /**
    //     * Parses the directory and all type class files within
    //     *
    //     * @param directory The directory that will contain configuration files for classes.
    //     */
    //    public void parse(File directory) {
    //        if (!directory.exists()) {
    //            return; // TODO error message}
    //        }
    //        if (!directory.isDirectory()) {
    //            return; // TODO error message}
    //        }
    //
    //        File[] files = directory.listFiles();
    //        for (File file : files) {
    //            Inscription.logger.fine("Parsing File: '" + file.toString() + "'");
    //            ConfigFile config = new ConfigFile(file);
    //
    //            ConfigurationSection entityClasses = config.getConfig().getConfigurationSection("entity-classes");
    //            if (entityClasses != null) {
    //                parseEntities(entityClasses);
    //            }
    //
    //            ConfigurationSection materialClasses = config.getConfig().getConfigurationSection("material-classes");
    //            if (materialClasses != null) {
    //                this.parseMaterials(materialClasses);
    //            }
    //
    //            ConfigurationSection damageClasses = config.getConfig().getConfigurationSection("damage-classes");
    //            if (damageClasses != null) {
    //                this.parseDamageTypes(damageClasses);
    //            }
    //        }
    //        // Done parsing
    //    }

}
