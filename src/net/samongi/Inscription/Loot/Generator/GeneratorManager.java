package net.samongi.Inscription.Loot.Generator;

import net.samongi.Inscription.Inscription;
import net.samongi.SamongiLib.Configuration.ConfigFile;
import net.samongi.SamongiLib.Configuration.ConfigurationParsing;
import org.bukkit.configuration.file.FileConfiguration;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.*;

public class GeneratorManager implements ConfigurationParsing {

    private static final String CONFIGURATION_SECTION_KEY = "generators";

    //----------------------------------------------------------------------------------------------------------------//

    private Map<String, GlyphGenerator> m_glyphGenerators = new HashMap<>();
    private Map<String, GlyphGenerator> m_glyphGeneratorsDisplay = new HashMap<>();

    //----------------------------------------------------------------------------------------------------------------//

    public void registerGenerator(GlyphGenerator generator) {
        this.m_glyphGenerators.put(generator.getTypeName(), generator);
        this.m_glyphGeneratorsDisplay.put(generator.getDisplayName(), generator);
    }

    /**
     * Retrieves the generator with the type name.
     *
     * @param type The name of the generator to retrieve
     * @return The GlyphGenerator
     */
    public GlyphGenerator getGeneratorByType(String type) {
        return this.m_glyphGenerators.get(type);
    }
    public GlyphGenerator getGeneratorByName(String name) {
        return this.m_glyphGeneratorsDisplay.get(name);
    }

    /**
     * Retrieves a list of all the generators.
     *
     * @return All the generators.
     */
    public List<GlyphGenerator> getGenerators() {
        return new ArrayList<GlyphGenerator>(this.m_glyphGenerators.values());
    }

    //----------------------------------------------------------------------------------------------------------------//
    @Override public boolean parseConfigFile(@Nonnull File file, @Nonnull ConfigFile config) {
        Inscription.logger.info("Parsing Generator Configurations in: '" + file.getAbsolutePath() + "'");
        FileConfiguration root = config.getConfig();

        Set<String> rootKeys = root.getKeys(false);
        for (String key : rootKeys) {

            GlyphGenerator generator = GlyphGenerator.parse(root.getConfigurationSection(key));
            if (generator == null) {
                Inscription.logger.warning(" - Null section found: '" + key + "'");
                continue;
            }

            this.registerGenerator(generator);
            Inscription.logger.info(" - Registered: '" + generator.getTypeName() + "'");
        }
        return true;
    }

    //----------------------------------------------------------------------------------------------------------------//
}
