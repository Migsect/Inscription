package net.samongi.Inscription.Glyphs.Types;

import net.samongi.Inscription.Inscription;
import net.samongi.SamongiLib.Configuration.ConfigFile;
import net.samongi.SamongiLib.Configuration.ConfigurationParsing;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class GlyphTypesManager implements ConfigurationParsing {

    private static final String ELEMENTS_SECTION_KEY = "elements";
    private static final String RARITIES_SECTION_KEY = "rarities";

    // ---------------------------------------------------------------------------------------------------------------//
    private Map<String, GlyphElement> m_glyphElements = new HashMap<>();
    private Map<String, GlyphElement> m_glyphElementsByDisplay = new HashMap<>();

    private Map<String, GlyphRarity> m_glyphRarities = new HashMap<>();
    private Map<String, GlyphRarity> m_glyphRaritiesByDisplay = new HashMap<>();

    // ---------------------------------------------------------------------------------------------------------------//
    public GlyphElement getElement(String typeName) {
        return m_glyphElements.get(typeName);
    }

    public GlyphElement getElementByDisplay(String displayName) {
        return m_glyphElementsByDisplay.get(displayName);
    }

    public GlyphRarity getRarity(String typeName) {
        return m_glyphRarities.get(typeName);
    }

    public GlyphRarity getRarityByDisplay(String displayName) {
        return m_glyphRaritiesByDisplay.get(displayName);
    }

    // ---------------------------------------------------------------------------------------------------------------//
    public boolean parseElements(@Nullable ConfigurationSection section) {
        if (section == null) {
            return false;
        }

        Set<String> keySet = section.getKeys(false);
        for (String key : keySet) {
            if (!section.isConfigurationSection(key)) {
                Inscription.logger.warning("Element configuration for '" + key + "' is not a section.");
                continue;
            }
            ConfigurationSection subSection = section.getConfigurationSection(key);
            GlyphElement element = GlyphElement.parse(key, subSection);
            if (element == null) {
                Inscription.logger.warning("Element configuration for '" + key + "' could not be parsed.");
                continue;
            }
            m_glyphElements.put(element.getType(), element);
            m_glyphElementsByDisplay.put(element.getDisplay(), element);
            Inscription.logger
                .info(String.format(" - Added Element: '%s(%s)'", element.getType(), element.getDisplay()));
        }

        return true;
    }

    // ---------------------------------------------------------------------------------------------------------------//
    public boolean parseRarities(@Nullable ConfigurationSection section) {
        if (section == null) {
            return false;
        }

        Set<String> keySet = section.getKeys(false);
        for (String key : keySet) {
            if (!section.isConfigurationSection(key)) {
                Inscription.logger.warning("Rarity configuration for '" + key + "' is not a section.");
                continue;
            }
            ConfigurationSection subSection = section.getConfigurationSection(key);
            GlyphRarity rarity = GlyphRarity.parse(key, subSection);
            if (rarity == null) {
                Inscription.logger.warning("Rarity configuration for '" + key + "' could not be parsed.");
                continue;
            }
            m_glyphRarities.put(rarity.getType(), rarity);
            m_glyphRaritiesByDisplay.put(rarity.getDisplay(), rarity);
            Inscription.logger.info(String.format(" - Added Rarity: '%s(%s)'", rarity.getType(), rarity.getDisplay()));
        }

        return true;
    }

    // ---------------------------------------------------------------------------------------------------------------//
    @Override public boolean parseConfigFile(@Nonnull File file, @Nonnull ConfigFile config) {
        Inscription.logger.info("Parsing TypeClass Configurations in: '" + file.getAbsolutePath() + "'");
        FileConfiguration root = config.getConfig();
        boolean parsedSomething = false;
        if (parseElements(root.getConfigurationSection(ELEMENTS_SECTION_KEY))) {
            Inscription.logger.info(String.format(" - Registered: '%s'", ELEMENTS_SECTION_KEY));
            parsedSomething = true;
        }
        if (parseRarities(root.getConfigurationSection(RARITIES_SECTION_KEY))) {
            Inscription.logger.info(String.format(" - Registered: '%s'", RARITIES_SECTION_KEY));
            parsedSomething = true;
        }
        if (!parsedSomething) {
            Inscription.logger.warning(String.format("Didn't find anything to parse in '%s'", file.getAbsolutePath()));
        }
        return parsedSomething;
    }
}