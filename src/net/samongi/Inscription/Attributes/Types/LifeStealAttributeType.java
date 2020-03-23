package net.samongi.Inscription.Attributes.Types;

import net.samongi.Inscription.Attributes.Attribute;
import net.samongi.Inscription.Attributes.AttributeType;
import net.samongi.Inscription.Attributes.AttributeTypeFactory;
import net.samongi.Inscription.Attributes.Base.NumericalAttributeType;
import net.samongi.Inscription.Conditions.ComparativeCondition;
import net.samongi.Inscription.Conditions.Condition;
import net.samongi.Inscription.Conditions.Helpers.PlayerConditionHelper;
import net.samongi.Inscription.Conditions.Helpers.TargetEntityConditionHelper;
import net.samongi.Inscription.Conditions.Types.*;
import net.samongi.Inscription.Glyphs.Glyph;
import net.samongi.Inscription.Inscription;
import net.samongi.Inscription.Player.CacheData;
import net.samongi.Inscription.Player.CacheTypes.CompositeCacheData;
import net.samongi.Inscription.Player.CacheTypes.NumericCacheData;
import net.samongi.Inscription.Player.PlayerData;
import net.samongi.Inscription.TypeClass.TypeClasses.BiomeClass;
import net.samongi.Inscription.TypeClass.TypeClasses.EntityClass;
import net.samongi.Inscription.TypeClass.TypeClasses.MaterialClass;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.projectiles.ProjectileSource;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BinaryOperator;

public class LifeStealAttributeType extends NumericalAttributeType {

    //----------------------------------------------------------------------------------------------------------------//
    private static final String TYPE_IDENTIFIER = "LIFESTEAL";

    //----------------------------------------------------------------------------------------------------------------//
    private Set<Condition> m_conditions = new HashSet<>();

    //----------------------------------------------------------------------------------------------------------------//
    protected LifeStealAttributeType(@Nonnull ConfigurationSection section) throws InvalidConfigurationException {
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
                CacheData cachedData = playerData.getData(TYPE_IDENTIFIER);
                if (cachedData == null) {
                    cachedData = new Data();
                }
                if (!(cachedData instanceof Data)) {
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
                    .getCacheData(reduceType, () -> new NumericData(reduceType, reduceType.getInitialAggregator()));

                Inscription.logger.finer("    +C '" + amount + "' reducer '" + reduceType + "'");
                numericCacheData.add(m_conditions, amount);

                Inscription.logger.finer("  Finished caching for " + m_displayName);
                playerData.setData(castedData);
            }

            @Override public String getLoreLine() {
                Glyph glyph = getGlyph();
                String damageStr = getDisplayString(glyph, 100, isPositive(glyph) ? "+" : "-", "%");

                String idLine = "" + ChatColor.YELLOW + ChatColor.ITALIC + this.getType().getDisplayName() + " - " + ChatColor.RESET;
                String infoLine = damageStr + ChatColor.YELLOW + " lifesteal";
                for (Condition condition : m_conditions) {
                    infoLine += condition.getDisplay();
                }

                return idLine + infoLine;
            }
        };
    }
    //----------------------------------------------------------------------------------------------------------------//
    public static class Data extends CompositeCacheData<ReduceType, NumericCacheData> {

        //----------------------------------------------------------------------------------------------------------------//
        @Override public String getType() {
            return TYPE_IDENTIFIER;

        }
        @Override public String getData() {
            return "";
        }

        public double calculateAggregate(Player player, Entity target)
        {
            Set<Condition> conditionGroups = new HashSet<>();

            conditionGroups.addAll(TargetEntityConditionHelper.getConditionsForTargetEntity(target));
            conditionGroups.addAll(PlayerConditionHelper.getConditionsForPlayer(player));

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

    //----------------------------------------------------------------------------------------------------------------//
    public static class Factory extends AttributeTypeFactory {

        @Nonnull @Override public AttributeType construct(@Nonnull ConfigurationSection section) throws InvalidConfigurationException {
            return new LifeStealAttributeType(section);
        }
        @Override public Listener getListener() {
            return new Listener() {

                @EventHandler public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
                    if (event.isCancelled()) {
                        return;
                    }

                    Entity damager = event.getDamager();

                    if (damager instanceof Arrow) {
                        ProjectileSource shooter = ((Arrow) damager).getShooter();
                        if (!(shooter instanceof Entity)) {
                            return;
                        }
                        damager = (Entity) shooter;
                    }

                    if (!(damager instanceof Player)) {
                        return;
                    }
                    // getting the data and basic objects
                    Player playerDamager = (Player) damager;
                    PlayerData playerData = Inscription.getInstance().getPlayerManager().getData(playerDamager);
                    CacheData data = playerData.getData(LifeStealAttributeType.TYPE_IDENTIFIER);
                    if (!(data instanceof LifeStealAttributeType.Data)) {
                        return;
                    }
                    LifeStealAttributeType.Data lifestealData = (LifeStealAttributeType.Data) data;

                    ItemStack itemInHand = playerDamager.getInventory().getItemInMainHand();
                    Material material = Material.AIR;
                    if (itemInHand != null) {
                        material = itemInHand.getType();
                    }

                    Entity entity = event.getEntity();

                    double lifestealAggregate = lifestealData.calculateAggregate(playerDamager, entity);
                    Inscription.logger.finest("[Damage Event] Lifesteal bonus: " + lifestealAggregate);

                    double maxLife = playerDamager.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue();
                    double newLife = Double.min(maxLife,playerDamager.getHealth() + event.getDamage() * lifestealAggregate);
                    playerDamager.setHealth(newLife);


                }
            };
        }
        @Nonnull @Override public String getAttributeTypeId() {
            return TYPE_IDENTIFIER;
        }
    }
}

