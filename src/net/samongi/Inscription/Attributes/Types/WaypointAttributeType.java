package net.samongi.Inscription.Attributes.Types;

import net.md_5.bungee.api.ChatColor;
import net.samongi.Inscription.Attributes.Attribute;
import net.samongi.Inscription.Attributes.AttributeType;
import net.samongi.Inscription.Attributes.AttributeTypeFactory;
import net.samongi.Inscription.Attributes.Base.AmountAttributeType;
import net.samongi.Inscription.Attributes.GeneralAttributeParser;
import net.samongi.Inscription.Inscription;
import net.samongi.Inscription.Player.CacheData;
import net.samongi.Inscription.Player.PlayerData;
import net.samongi.Inscription.TypeClass.TypeClasses.BiomeClass;
import net.samongi.SamongiLib.Tuple.Tuple;
import org.bukkit.block.Biome;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.event.Listener;

import javax.annotation.Nonnull;
import java.util.*;

public class WaypointAttributeType extends AmountAttributeType {

    //--------------------------------------------------------------------------------------------------------------------//
    public static final String TYPE_IDENTIFIER = "WAYPOINT";

    //--------------------------------------------------------------------------------------------------------------------//
    private BiomeClass m_fromBiome = null;
    private BiomeClass m_toBiome = null;

    //--------------------------------------------------------------------------------------------------------------------//
    protected WaypointAttributeType(@Nonnull ConfigurationSection section) throws InvalidConfigurationException {
        super(section);

        String fromBiomeString = section.getString("target-materials");
        if (fromBiomeString == null) {
            throw new InvalidConfigurationException("'target-materials' is not defined");
        }

        String toBiomeString = section.getString("target-blocks");
        if (toBiomeString == null) {
            throw new InvalidConfigurationException("'target-blocks' is not defined");
        }

        m_fromBiome = BiomeClass.handler.getTypeClass(fromBiomeString);
        if (m_fromBiome == null) {
            throw new InvalidConfigurationException("'" + fromBiomeString + "' is not a valid biome class.");
        }

        m_toBiome = BiomeClass.handler.getTypeClass(toBiomeString);
        if (m_toBiome == null) {
            throw new InvalidConfigurationException("'" + toBiomeString + "' is not a valid biome class.");
        }
    }

    //--------------------------------------------------------------------------------------------------------------------//
    @Override public Attribute generate() {
        return new Attribute(this) {

            @Override public void cache(PlayerData playerData) {
                CacheData cachedData = playerData.getData(TYPE_IDENTIFIER);
                if (cachedData == null) {
                    cachedData = new WaypointAttributeType.Data();
                }
                if (!(cachedData instanceof WaypointAttributeType.Data)) {
                    return;
                }

                Inscription.logger.finer("  Caching attribute for " + m_displayName);
                Inscription.logger.finer("    'fromBiome' is global?: " + m_fromBiome.isGlobal());
                Inscription.logger.finer("    'toBiome' is global?: " + m_toBiome.isGlobal());

                Data amountData = (Data) cachedData;

                int addedAmount = getAmount(this.getGlyph());
                if (m_fromBiome.isGlobal() && m_toBiome.isGlobal()) {
                    int currentAmount = amountData.get();
                    amountData.set(currentAmount + addedAmount);
                    Inscription.logger.finer("  +C Added '" + addedAmount + "' bonus");

                } else if (m_fromBiome.isGlobal()) {
                    for (Biome toBiome : m_toBiome.getBiomes()) {
                        int currentAmount = amountData.get(null, toBiome);
                        amountData.set(null, toBiome, currentAmount + addedAmount);
                        Inscription.logger.finer("  +C Added '" + addedAmount + "' bonus to toBiome:'" + toBiome.toString() + "'");
                    }

                } else if (m_toBiome.isGlobal()) {
                    for (Biome fromBiome : m_fromBiome.getBiomes()) {
                        int currentAmount = amountData.get(fromBiome, null);
                        amountData.set(fromBiome, null, currentAmount + addedAmount);
                        Inscription.logger.finer("  +C Added '" + addedAmount + "' bonus to fromBiome:'" + fromBiome.toString() + "'");
                    }
                } else {
                    for (Biome toBiome : m_toBiome.getBiomes())
                        for (Biome fromBiome : m_fromBiome.getBiomes()) {
                            int currentAmount = amountData.get(fromBiome, toBiome);
                            amountData.set(fromBiome, toBiome, currentAmount + addedAmount);
                            Inscription.logger.finer(
                                "  +C Added '" + addedAmount + "' bonus to fromBiome:'" + fromBiome.toString() + "'|toBiome:'" + fromBiome.toString() + "'");
                        }

                }

                Inscription.logger.finer("  Finished caching for " + m_displayName);
                playerData.setData(amountData);
            }

            @Override public String getLoreLine() {
                String amountString = ((WaypointAttributeType) this.getType()).getDisplayString(this.getGlyph(), "+", "m");
                String fromClass = m_fromBiome.getName();
                String toClass = m_toBiome.getName();

                String infoLine =
                    amountString + ChatColor.YELLOW + " distance for warping from " + ChatColor.BLUE + fromClass + ChatColor.YELLOW + " biomes to "
                        + ChatColor.BLUE + toClass + ChatColor.YELLOW + " biomes";
                return "" + ChatColor.YELLOW + ChatColor.ITALIC + this.getType().getDisplayName() + " - " + ChatColor.RESET + infoLine;
            }
        };
    }

    //--------------------------------------------------------------------------------------------------------------------//
    public static class Data implements CacheData {

        /* Data members of the the data */
        private int m_globalWaypointAmount = 0;
        private Map<Tuple, Integer> m_speciedWaypointAmounts = new HashMap<>();

        // Setters
        public void set(int amount) {
            this.m_globalWaypointAmount = amount;
        }
        public void set(Biome fromBiome, Biome toBiome, int amount) {
            Tuple key = new Tuple(fromBiome, toBiome);
            m_speciedWaypointAmounts.put(key, amount);
        }

        // Getters
        public int get() {
            return this.m_globalWaypointAmount;
        }
        public int get(Biome fromBiome, Biome toBiome) {
            Tuple key = new Tuple(fromBiome, toBiome);
            int value = m_speciedWaypointAmounts.getOrDefault(key, 0);
            return value;
        }

        @Override public void clear() {
            m_globalWaypointAmount = 0;
            m_speciedWaypointAmounts.clear();
        }

        @Override public String getType() {
            return TYPE_IDENTIFIER;
        }

        @Override public String getData() {
            // TODO This returns the data as a string
            return "";
        }
    }

    //--------------------------------------------------------------------------------------------------------------------//
    public static class Factory extends AttributeTypeFactory {

        @Nonnull @Override public String getAttributeTypeId() {
            return TYPE_IDENTIFIER;
        }

        @Nonnull @Override public AttributeType construct(@Nonnull ConfigurationSection section) throws InvalidConfigurationException {
            return new WaypointAttributeType(section);
        }

        @Override public Listener getListener() {
            return new Listener() {};
        }
    }
}
