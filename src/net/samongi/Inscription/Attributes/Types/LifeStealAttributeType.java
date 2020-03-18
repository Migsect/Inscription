package net.samongi.Inscription.Attributes.Types;

import net.samongi.Inscription.Attributes.Attribute;
import net.samongi.Inscription.Attributes.AttributeType;
import net.samongi.Inscription.Attributes.AttributeTypeFactory;
import net.samongi.Inscription.Attributes.Base.NumericalAttributeType;
import net.samongi.Inscription.Conditions.ComparativeCondition;
import net.samongi.Inscription.Conditions.Condition;
import net.samongi.Inscription.Conditions.ConditionPermutator;
import net.samongi.Inscription.Conditions.Types.*;
import net.samongi.Inscription.Inscription;
import net.samongi.Inscription.Player.CacheData;
import net.samongi.Inscription.Player.CacheTypes.CompositeCacheData;
import net.samongi.Inscription.Player.CacheTypes.NumericCacheData;
import net.samongi.Inscription.Player.PlayerData;
import net.samongi.Inscription.TypeClass.TypeClasses.BiomeClass;
import net.samongi.Inscription.TypeClass.TypeClasses.EntityClass;
import net.samongi.Inscription.TypeClass.TypeClasses.MaterialClass;
import net.samongi.SamongiLib.DataStructures.PartialKeyMap;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
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

            @Override public void cache(PlayerData data) {
                CacheData cachedData = data.getData(TYPE_IDENTIFIER);
                if (cachedData == null) {
                    cachedData = new LifeStealAttributeType.Data();
                }
                if (!(cachedData instanceof LifeStealAttributeType.Data)) {
                    Inscription.logger.severe("CachedData with id '" + TYPE_IDENTIFIER + "' is not castable to its type");
                    return;
                }
                LifeStealAttributeType.Data castedData = (LifeStealAttributeType.Data) cachedData;

                Inscription.logger.finer("  Caching attribute for " + m_displayName);
                for (Condition condition : m_conditions) {
                    Inscription.logger.finer("    Condition " + condition.getClass().getSimpleName());
                }

                double amount = getNumber(getGlyph());
                NumericalAttributeType.ReduceType reduceType = getReduceType();
                NumericCacheData numericCacheData = castedData
                    .getCacheData(reduceType, () -> new NumericData(reduceType.getReduceOperator(), reduceType.getInitialAggregator()));

                Inscription.logger.finer("    +C '" + amount + "' reducer '" + reduceType + "'");
                numericCacheData.add(m_conditions, amount);
                Inscription.logger.finer("  Finished caching for " + m_displayName);

                data.setData(castedData);
            }

            @Override public String getLoreLine() {
                String damageStr = getDisplayString(this.getGlyph(), 100, "+", "%");

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

                    EntityType entityType = entity.getType();
                    Biome entityBiome = entity.getLocation().getBlock().getBiome();
                    Material weaponMaterial = playerDamager.getInventory().getItemInMainHand().getType();
                    int level = playerDamager.getLevel();

                    Set<Condition> toEntityConditions = EntityClass.handler.getInvolvedAsCondition(entityType, (tc) -> new ToEntityCondition((EntityClass) tc));
                    Set<Condition> toBiomeConditions = BiomeClass.handler.getInvolvedAsCondition(entityBiome, (tc) -> new InBiomeCondition((BiomeClass) tc));
                    Set<Condition> usingMaterialConditions = MaterialClass.handler
                        .getInvolvedAsCondition(weaponMaterial, (tc) -> new UsingMaterialCondition((MaterialClass) tc));
                    Set<Condition> whileLevelConditions = new HashSet<>();
                    whileLevelConditions.add(new WhileLevelCondition((double)level, ComparativeCondition.Mode.NULL));

                    List<Set<Condition>> conditionGroups = new ArrayList<>();
                    conditionGroups.add(toEntityConditions);
                    conditionGroups.add(toBiomeConditions);
                    conditionGroups.add(usingMaterialConditions);
                    conditionGroups.add(whileLevelConditions);

                    double lifestealAggregate = calculateConditionAggregate(conditionGroups, lifestealData);
                    Inscription.logger.finest("[Damage Event] Lifesteal bonus: " + lifestealAggregate);

                }
            };
        }
        @Nonnull @Override public String getAttributeTypeId() {
            return TYPE_IDENTIFIER;
        }
    }
}

