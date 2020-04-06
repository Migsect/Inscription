package net.samongi.Inscription.Attributes.Types;

import net.md_5.bungee.api.ChatColor;
import net.samongi.Inscription.Attributes.Attribute;
import net.samongi.Inscription.Attributes.AttributeType;
import net.samongi.Inscription.Attributes.AttributeTypeFactory;
import net.samongi.Inscription.Attributes.Base.AmountAttributeType;
import net.samongi.Inscription.Attributes.Base.NumericalAttributeType;
import net.samongi.Inscription.Conditions.Condition;
import net.samongi.Inscription.Conditions.Helpers.PlayerConditionHelper;
import net.samongi.Inscription.Conditions.Types.FromBiomeCondition;
import net.samongi.Inscription.Conditions.Types.ToBiomeCondition;
import net.samongi.Inscription.Glyphs.Glyph;
import net.samongi.Inscription.Inscription;
import net.samongi.Inscription.Player.CacheData;
import net.samongi.Inscription.Player.CacheTypes.CompositeCacheData;
import net.samongi.Inscription.Player.CacheTypes.NumericCacheData;
import net.samongi.Inscription.Player.PlayerData;
import net.samongi.Inscription.TypeClass.TypeClasses.BiomeClass;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.Set;

public class VillagerScanAttributeType extends AmountAttributeType {
    //--------------------------------------------------------------------------------------------------------------------//
    public static final String TYPE_IDENTIFIER = "VILLAGER_SCAN";

    //--------------------------------------------------------------------------------------------------------------------//
    private Set<Condition> m_conditions = new HashSet<>();

    //--------------------------------------------------------------------------------------------------------------------//
    protected VillagerScanAttributeType(@Nonnull ConfigurationSection section) throws InvalidConfigurationException {
        super(section);

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
                    cachedData = new VillagerScanAttributeType.Data();
                }
                if (!(cachedData instanceof VillagerScanAttributeType.Data)) {
                    Inscription.logger.severe("CachedData with id '" + TYPE_IDENTIFIER + "' is not castable to its type");
                    return;
                }
                VillagerScanAttributeType.Data castedData = (VillagerScanAttributeType.Data) cachedData;

                Inscription.logger.finer("  Caching attribute for " + m_displayName);
                for (Condition condition : m_conditions) {
                    Inscription.logger.finer("    Condition " + condition.toString());
                }

                double amount = getNumber(getGlyph());
                NumericalAttributeType.ReduceType reduceType = getReduceType();
                NumericCacheData numericCacheData = castedData
                    .getCacheData(reduceType, () -> new VillagerScanAttributeType.NumericData(reduceType, reduceType.getInitialAggregator()));

                Inscription.logger.finer("    +C '" + amount + "' reducer '" + reduceType + "'");
                numericCacheData.add(m_conditions, amount);

                Inscription.logger.finer("  Finished caching for " + m_displayName);
                playerData.setData(cachedData);
            }

            @Override public String getLoreLine() {
                Glyph glyph = getGlyph();
                String amountString = getDisplayString(glyph, isPositive(glyph) ? "+" : "-", "m");

                String idLine = "" + ChatColor.YELLOW + ChatColor.ITALIC + this.getType().getDisplayName() + " - " + ChatColor.RESET;
                String infoLine = amountString + ChatColor.YELLOW + " range for the villager scan ability";
                for (Condition condition : m_conditions) {
                    infoLine += condition.getDisplay();
                }

                return idLine + infoLine;
            }
        };
    }

    public static class Data extends CompositeCacheData<NumericalAttributeType.ReduceType, NumericCacheData> {

        //----------------------------------------------------------------------------------------------------------------//
        @Override public String getType() {
            return TYPE_IDENTIFIER;

        }
        @Override public String getData() {
            return "";
        }

        public double calculateAggregate(Player player) {
            Set<Condition> conditionGroups = PlayerConditionHelper.getConditionsForPlayer(player);

            return calculateConditionAggregate(conditionGroups, this);
        }
    }

    public static class NumericData extends NumericCacheData {

        NumericData(NumericalAttributeType.ReduceType reduceType, double dataGlobalInitial) {
            super(reduceType);
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
            return new VillagerScanAttributeType(section);
        }

        @Override public Listener getListener() {
            return new Listener() {

            };
        }
    }
}
