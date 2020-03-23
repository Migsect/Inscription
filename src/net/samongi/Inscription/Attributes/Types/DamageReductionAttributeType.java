package net.samongi.Inscription.Attributes.Types;

import net.md_5.bungee.api.ChatColor;
import net.samongi.Inscription.Attributes.Attribute;
import net.samongi.Inscription.Attributes.AttributeType;
import net.samongi.Inscription.Attributes.AttributeTypeFactory;
import net.samongi.Inscription.Attributes.Base.NumericalAttributeType;
import net.samongi.Inscription.Conditions.Condition;
import net.samongi.Inscription.Conditions.Helpers.PlayerConditionHelper;
import net.samongi.Inscription.Conditions.Types.ToDamageTakenCondition;
import net.samongi.Inscription.Glyphs.Glyph;
import net.samongi.Inscription.Inscription;
import net.samongi.Inscription.Player.CacheData;
import net.samongi.Inscription.Player.CacheTypes.CompositeCacheData;
import net.samongi.Inscription.Player.CacheTypes.NumericCacheData;
import net.samongi.Inscription.Player.PlayerData;
import net.samongi.Inscription.TypeClass.TypeClasses.DamageClass;
import net.samongi.Inscription.TypeClass.TypeClasses.MaterialClass;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class DamageReductionAttributeType extends NumericalAttributeType {

    private static final String TYPE_IDENTIFIER = "DAMAGE_REDUCTION";

    //----------------------------------------------------------------------------------------------------------------//
    private Set<Condition> m_conditions = new HashSet<>();

    //----------------------------------------------------------------------------------------------------------------//
    protected DamageReductionAttributeType(@Nonnull ConfigurationSection section) throws InvalidConfigurationException {
        super(section);

        ConfigurationSection conditionSection = section.getConfigurationSection("conditions");
        if (conditionSection != null) {
            m_conditions = Inscription.getInstance().getAttributeManager().parseConditions(conditionSection);
        }
    }

    //----------------------------------------------------------------------------------------------------------------//

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
                NumericCacheData numericCacheData = castedData.getCacheData(reduceType, () -> new NumericData(reduceType, reduceType.getInitialAggregator()));

                Inscription.logger.finer("    +C '" + amount + "' reducer '" + reduceType + "'");
                numericCacheData.add(m_conditions, amount);

                Inscription.logger.finer("  Finished caching for " + m_displayName);
                playerData.setData(castedData);
            }

            @Override public String getLoreLine() {
                Glyph glyph = getGlyph();
                String multiplierString = getDisplayString(glyph, 100, isPositive(glyph) ? "-" : "+", "%");

                String infoLine = multiplierString + ChatColor.YELLOW + " damage taken" + Condition.concatConditionDisplays(m_conditions);

                return getDisplayLineId() + infoLine;
            }
        };
    }


    public static class Data extends CompositeCacheData<ReduceType, NumericCacheData> {

        //----------------------------------------------------------------------------------------------------------------//
        @Override public String getType() {
            return TYPE_IDENTIFIER;

        }
        @Override public String getData() {
            return "";
        }

        public double calculateAggregate(Player player, EntityDamageEvent.DamageCause damageCause) {
            Set<Condition> conditionGroupsHelmet = PlayerConditionHelper.getConditionsForPlayer(player, PlayerConditionHelper.Option.ONLY_WEARING_HELMET);
            Set<Condition> conditionGroupsChest = PlayerConditionHelper.getConditionsForPlayer(player, PlayerConditionHelper.Option.ONLY_WEARING_CHEST);
            Set<Condition> conditionGroupsLegs = PlayerConditionHelper.getConditionsForPlayer(player, PlayerConditionHelper.Option.ONLY_WEARING_LEGS);
            Set<Condition> conditionGroupsBoots = PlayerConditionHelper.getConditionsForPlayer(player, PlayerConditionHelper.Option.ONLY_WEARING_BOOTS);

            Set<Condition> damageConditions = DamageClass.handler.getInvolvedAsCondition(damageCause, (tc) -> new ToDamageTakenCondition((DamageClass) tc));
            conditionGroupsHelmet.addAll(damageConditions);
            conditionGroupsChest.addAll(damageConditions);
            conditionGroupsLegs.addAll(damageConditions);
            conditionGroupsBoots.addAll(damageConditions);

            double amountHelmet = calculateConditionAggregate(conditionGroupsHelmet, this);
            double amountChest = calculateConditionAggregate(conditionGroupsChest, this);
            double amountLegs = calculateConditionAggregate(conditionGroupsLegs, this);
            double amountBoots = calculateConditionAggregate(conditionGroupsBoots, this);

            return (amountHelmet + amountChest + amountLegs + amountBoots) / 4;
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

    //----------------------------------------------------------------------------------------------------------------//
    public static class Factory extends AttributeTypeFactory {

        //------------------------------------------------------------------------------------------------------------//
        @Nonnull @Override public String getAttributeTypeId() {
            return TYPE_IDENTIFIER;
        }

        //------------------------------------------------------------------------------------------------------------//
        @Nonnull @Override public AttributeType construct(@Nonnull ConfigurationSection section) throws InvalidConfigurationException {
            return new DamageReductionAttributeType(section);
        }
        //------------------------------------------------------------------------------------------------------------//
        @Override public Listener getListener() {
            return new Listener() {

                @EventHandler public void onCharacterDamaged(EntityDamageEvent event) {
                    if (event.isCancelled()) {
                        return;
                    }
                    Entity entity = event.getEntity();
                    if (!(entity instanceof Player)) {
                        return;
                    }
                    Player player = (Player) entity;

                    double damageDone = event.getDamage();
                    EntityDamageEvent.DamageCause damageCause = event.getCause();

                    PlayerData playerData = Inscription.getInstance().getPlayerManager().getData(player);
                    CacheData cacheData = playerData.getData(DamageReductionAttributeType.TYPE_IDENTIFIER);
                    if (!(cacheData instanceof DamageReductionAttributeType.Data)) {
                        return;
                    }
                    DamageReductionAttributeType.Data data = (DamageReductionAttributeType.Data) cacheData;

                    double totalMultiplier = data.calculateAggregate(player, damageCause);

                    double reducedDamage = damageDone - Math.max(0, damageDone * totalMultiplier);
                    event.setDamage(reducedDamage);

                    Inscription.logger.finest("" + "[EntityDamageEvent] DamageReduction " +  (1 - totalMultiplier) + " " + damageDone + " -> " + reducedDamage);
                }
            };
        }
    }
}
