package net.samongi.Inscription.Attributes.Types;

import net.md_5.bungee.api.ChatColor;
import net.samongi.Inscription.Attributes.Attribute;
import net.samongi.Inscription.Attributes.AttributeType;
import net.samongi.Inscription.Attributes.AttributeTypeFactory;
import net.samongi.Inscription.Attributes.Base.AmountAttributeType;
import net.samongi.Inscription.Attributes.Base.NumericalAttributeType;
import net.samongi.Inscription.Attributes.GeneralAttributeParser;
import net.samongi.Inscription.Conditions.Condition;
import net.samongi.Inscription.Conditions.Helpers.PlayerConditionHelper;
import net.samongi.Inscription.Conditions.Helpers.TargetEntityConditionHelper;
import net.samongi.Inscription.Conditions.Types.FromBiomeCondition;
import net.samongi.Inscription.Conditions.Types.ToBiomeCondition;
import net.samongi.Inscription.Glyphs.Glyph;
import net.samongi.Inscription.Inscription;
import net.samongi.Inscription.Player.CacheData;
import net.samongi.Inscription.Player.CacheTypes.CompositeCacheData;
import net.samongi.Inscription.Player.CacheTypes.NumericCacheData;
import net.samongi.Inscription.Player.PlayerData;
import net.samongi.Inscription.TypeClass.TypeClasses.BiomeClass;
import net.samongi.SamongiLib.Tuple.Tuple;
import org.bukkit.Location;
import org.bukkit.block.Biome;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.function.BinaryOperator;

public class WaypointAttributeType extends AmountAttributeType {

    //--------------------------------------------------------------------------------------------------------------------//
    public static final String TYPE_IDENTIFIER = "WAYPOINT";

    //--------------------------------------------------------------------------------------------------------------------//
    //    private BiomeClass m_fromBiome = null;
    //    private BiomeClass m_toBiome = null;
    private Set<Condition> m_conditions = new HashSet<>();

    //--------------------------------------------------------------------------------------------------------------------//
    protected WaypointAttributeType(@Nonnull ConfigurationSection section) throws InvalidConfigurationException {
        super(section);

        //        String fromBiomeString = section.getString("target-materials");
        //        if (fromBiomeString == null) {
        //            throw new InvalidConfigurationException("'target-materials' is not defined");
        //        }
        //
        //        String toBiomeString = section.getString("target-blocks");
        //        if (toBiomeString == null) {
        //            throw new InvalidConfigurationException("'target-blocks' is not defined");
        //        }
        //
        //        m_fromBiome = BiomeClass.handler.getTypeClass(fromBiomeString);
        //        if (m_fromBiome == null) {
        //            throw new InvalidConfigurationException("'" + fromBiomeString + "' is not a valid biome class.");
        //        }
        //
        //        m_toBiome = BiomeClass.handler.getTypeClass(toBiomeString);
        //        if (m_toBiome == null) {
        //            throw new InvalidConfigurationException("'" + toBiomeString + "' is not a valid biome class.");
        //        }
        ConfigurationSection conditionSection = section.getConfigurationSection("conditions");
        if (conditionSection != null) {
            m_conditions = Inscription.getInstance().getAttributeManager().parseConditions(conditionSection);
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
                    Inscription.logger.severe("CachedData with id '" + TYPE_IDENTIFIER + "' is not castable to its type");
                    return;
                }
                Data castedData = (Data) cachedData;

                Inscription.logger.finer("  Caching attribute for " + m_displayName);
                for (Condition condition : m_conditions) {
                    Inscription.logger.finer("    Condition " + condition.toString());
                }

                double amount = getNumber(getGlyph());
                NumericalAttributeType.ReduceType reduceType = getReduceType();
                NumericCacheData numericCacheData = castedData
                    .getCacheData(reduceType, () -> new NumericData(reduceType.getReduceOperator(), reduceType.getInitialAggregator()));

                Inscription.logger.finer("    +C '" + amount + "' reducer '" + reduceType + "'");
                numericCacheData.add(m_conditions, amount);
                //                int addedAmount = getAmount(this.getGlyph());
                //                if (m_fromBiome.isGlobal() && m_toBiome.isGlobal()) {
                //                    int currentAmount = amountData.get();
                //                    amountData.set(currentAmount + addedAmount);
                //                    Inscription.logger.finer("  +C Added '" + addedAmount + "' bonus");
                //
                //                } else if (m_fromBiome.isGlobal()) {
                //                    for (Biome toBiome : m_toBiome.getBiomes()) {
                //                        int currentAmount = amountData.get(null, toBiome);
                //                        amountData.set(null, toBiome, currentAmount + addedAmount);
                //                        Inscription.logger.finer("  +C Added '" + addedAmount + "' bonus to toBiome:'" + toBiome.toString() + "'");
                //                    }
                //
                //                } else if (m_toBiome.isGlobal()) {
                //                    for (Biome fromBiome : m_fromBiome.getBiomes()) {
                //                        int currentAmount = amountData.get(fromBiome, null);
                //                        amountData.set(fromBiome, null, currentAmount + addedAmount);
                //                        Inscription.logger.finer("  +C Added '" + addedAmount + "' bonus to fromBiome:'" + fromBiome.toString() + "'");
                //                    }
                //                } else {
                //                    for (Biome toBiome : m_toBiome.getBiomes())
                //                        for (Biome fromBiome : m_fromBiome.getBiomes()) {
                //                            int currentAmount = amountData.get(fromBiome, toBiome);
                //                            amountData.set(fromBiome, toBiome, currentAmount + addedAmount);
                //                            Inscription.logger.finer(
                //                                "  +C Added '" + addedAmount + "' bonus to fromBiome:'" + fromBiome.toString() + "'|toBiome:'" + fromBiome.toString() + "'");
                //                        }
                //
                //                }

                Inscription.logger.finer("  Finished caching for " + m_displayName);
                playerData.setData(cachedData);
            }

            @Override public String getLoreLine() {
                Glyph glyph = getGlyph();
                String amountString = getDisplayString(glyph, isPositive(glyph) ? "+" : "-", "m");

                String idLine = "" + ChatColor.YELLOW + ChatColor.ITALIC + this.getType().getDisplayName() + " - " + ChatColor.RESET;
                String infoLine = amountString + ChatColor.YELLOW + " distance for waypoints";
                for (Condition condition : m_conditions) {
                    infoLine += condition.getDisplay();
                }

                return idLine + infoLine;
            }
        };
    }

    //--------------------------------------------------------------------------------------------------------------------//
    //    public static class Data implements CacheData {
    //
    //        /* Data members of the the data */
    //        private int m_globalWaypointAmount = 0;
    //        private Map<Tuple, Integer> m_speciedWaypointAmounts = new HashMap<>();
    //
    //        // Setters
    //        public void set(int amount) {
    //            this.m_globalWaypointAmount = amount;
    //        }
    //        public void set(Biome fromBiome, Biome toBiome, int amount) {
    //            Tuple key = new Tuple(fromBiome, toBiome);
    //            m_speciedWaypointAmounts.put(key, amount);
    //        }
    //
    //        // Getters
    //        public int get() {
    //            return this.m_globalWaypointAmount;
    //        }
    //        public int get(Biome fromBiome, Biome toBiome) {
    //            Tuple key = new Tuple(fromBiome, toBiome);
    //            int value = m_speciedWaypointAmounts.getOrDefault(key, 0);
    //            return value;
    //        }
    //
    //        @Override public void clear() {
    //            m_globalWaypointAmount = 0;
    //            m_speciedWaypointAmounts.clear();
    //        }
    //
    //        @Override public String getType() {
    //            return TYPE_IDENTIFIER;
    //        }
    //
    //        @Override public String getData() {
    //            // TODO This returns the data as a string
    //            return "";
    //        }
    //    }

    public static class Data extends CompositeCacheData<ReduceType, NumericCacheData> {

        //----------------------------------------------------------------------------------------------------------------//
        @Override public String getType() {
            return TYPE_IDENTIFIER;

        }
        @Override public String getData() {
            return "";
        }

        public double calculateAggregate(Player player, Location from, Location to) {
            List<Set<Condition>> conditionGroups = PlayerConditionHelper.getConditionsForPlayer(player);
            conditionGroups.add(BiomeClass.handler.getInvolvedAsCondition(from.getBlock().getBiome(), (tc) -> new FromBiomeCondition((BiomeClass) tc)));
            conditionGroups.add(BiomeClass.handler.getInvolvedAsCondition(to.getBlock().getBiome(), (tc) -> new ToBiomeCondition((BiomeClass) tc)));
            return calculateConditionAggregate(conditionGroups, this);
        }
    }

    public static class NumericData extends NumericCacheData {

        NumericData(BinaryOperator<Double> reduceOperator, double dataGlobalInitial) {
            super(reduceOperator);
            set(dataGlobalInitial);
        }

        @Override public String getType() {
            return TYPE_IDENTIFIER;
        }
        @Override public String getData() {
            return null;
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
            return new Listener() {

            };
        }
    }
}
