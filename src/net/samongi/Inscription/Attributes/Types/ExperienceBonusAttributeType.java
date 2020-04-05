package net.samongi.Inscription.Attributes.Types;

import net.md_5.bungee.api.ChatColor;
import net.samongi.Inscription.Attributes.Attribute;
import net.samongi.Inscription.Attributes.AttributeType;
import net.samongi.Inscription.Attributes.AttributeTypeFactory;
import net.samongi.Inscription.Attributes.Base.NumericalAttributeType;
import net.samongi.Inscription.Attributes.GeneralAttributeParser;
import net.samongi.Inscription.Conditions.Condition;
import net.samongi.Inscription.Conditions.Helpers.PlayerConditionHelper;
import net.samongi.Inscription.Conditions.Helpers.TargetEntityConditionHelper;
import net.samongi.Inscription.Glyphs.Glyph;
import net.samongi.Inscription.Inscription;
import net.samongi.Inscription.Player.CacheData;
import net.samongi.Inscription.Player.CacheTypes.CompositeCacheData;
import net.samongi.Inscription.Player.CacheTypes.NumericCacheData;
import net.samongi.Inscription.Player.PlayerData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerExpChangeEvent;

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class ExperienceBonusAttributeType extends NumericalAttributeType {

    //--------------------------------------------------------------------------------------------------------------------//
    private static final String TYPE_IDENTIFIER = "EXPERIENCE_BONUS";

    //--------------------------------------------------------------------------------------------------------------------//
    private Set<Condition> m_conditions = new HashSet<>();

    //--------------------------------------------------------------------------------------------------------------------//
    protected ExperienceBonusAttributeType(@Nonnull ConfigurationSection section) throws InvalidConfigurationException {
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

                Data castedData = CacheData.getData(Data.class, TYPE_IDENTIFIER, playerData, Data::new);
                Inscription.logger.finer("  Caching attribute for " + m_displayName);
                for (Condition condition : m_conditions) {
                    Inscription.logger.finer("    Condition " + condition.toString());
                }

                double amount = getNumber(getGlyph());
                NumericalAttributeType.ReduceType reduceType = getReduceType();
                NumericCacheData numericCacheData = castedData
                    .getCacheData(reduceType, () -> new NumericData(reduceType, reduceType.getInitialAggregator()));

                Inscription.logger.finer("    +C '" + amount + "' reducer '" + reduceType + "'");
                numericCacheData.add(m_conditions, amount);

                Inscription.logger.finer("  Finished caching for " + m_displayName);
                playerData.setData(castedData);

            }

            @Override public String getLoreLine() {
                Glyph glyph = getGlyph();
                String multiplierString = getDisplayString(glyph, 100, isPositive(glyph) ? "+" : "-", "%");

                String infoLine = multiplierString + ChatColor.YELLOW + " extra experience" + Condition.concatConditionDisplays(m_conditions);

                return getDisplayLineId() + infoLine;
            }
        };
    }

    //--------------------------------------------------------------------------------------------------------------------//

    public static class Data extends CompositeCacheData<ReduceType, NumericCacheData> {

        //----------------------------------------------------------------------------------------------------------------//
        @Override public String getType() {
            return TYPE_IDENTIFIER;

        }
        @Override public String getData() {
            return "";
        }

        public double calculateAggregate(Player player)
        {
            Set<Condition> conditionGroups = PlayerConditionHelper.getConditionsForPlayer(player);
            return calculateConditionAggregate(conditionGroups, this);
        }
    }

    public static class NumericData extends NumericCacheData {

        NumericData(ReduceType reduceType, double dataGlobalInitial) {
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
            return new ExperienceBonusAttributeType(section);
        }
        @Override public Listener getListener() {
            return new Listener() {

                @EventHandler public void onExperienceChange(PlayerExpChangeEvent event) {
                    Player player = event.getPlayer();
                    PlayerData playerData = Inscription.getInstance().getPlayerManager().getData(player);

                    CacheData cacheData = playerData.getData(TYPE_IDENTIFIER);
                    if (!(cacheData instanceof Data)) {
                        return;
                    }
                    Data data = (Data) cacheData;
                    double experienceMultiplier = 1 + data.calculateAggregate(player);

                    int experience = event.getAmount();
                    double multipliedExperience = experience * experienceMultiplier;
                    double fractionalExperience = multipliedExperience - Math.floor(multipliedExperience);

                    Random rand = new Random();
                    if (rand.nextDouble() < fractionalExperience) {
                        multipliedExperience += 1;
                    }
                    event.setAmount((int) multipliedExperience);
                    Inscription.logger.finest(
                        "[PlayerExpChangeEvent] Extra Experience: " + experienceMultiplier + " " + experience
                            + " -> " + multipliedExperience);
                }
            };
        }
    }

}
