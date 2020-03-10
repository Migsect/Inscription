package net.samongi.Inscription.TypeClass;

import java.io.File;
import java.util.*;

import net.samongi.Inscription.Inscription;
import net.samongi.SamongiLib.Configuration.ConfigFile;

import net.samongi.SamongiLib.Configuration.ConfigurationParsing;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import javax.annotation.Nonnull;

public class TypeClassManager implements ConfigurationParsing {

    // ---------------------------------------------------------------------------------------------------------------//
    private final Set<TypeClassHandler> m_typeClassHandlers = new HashSet<>();

    // ---------------------------------------------------------------------------------------------------------------//
    public void registerTypeClassHandler(TypeClassHandler<?> handler) {
        m_typeClassHandlers.add(handler);
    }

    public void updateAllInvertedClasses() {
        for (TypeClassHandler handler : m_typeClassHandlers) {
            handler.updateInvertedClasses();
        }
    }

    // ---------------------------------------------------------------------------------------------------------------//
    @Override public boolean parseConfigFile(@Nonnull File file, @Nonnull ConfigFile config) {
        Inscription.logger.info("Parsing TypeClass Configurations in: '" + file.getAbsolutePath() + "'");
        FileConfiguration root = config.getConfig();

        boolean parsedSomething = false;
        for (TypeClassHandler handler : m_typeClassHandlers) {
            ConfigurationSection section = root.getConfigurationSection(handler.getSectionPath());
            boolean result = handler.parse(section);
            parsedSomething = parsedSomething || result;
        }

        if (!parsedSomething) {
            Inscription.logger.warning(String.format("Didn't find anything to parse in '%s'", file.getAbsolutePath()));
        }
        return parsedSomething;
    }

    // ---------------------------------------------------------------------------------------------------------------//

}
