package net.samongi.Inscription.Attributes.Types;

import net.samongi.Inscription.Attributes.Attribute;
import net.samongi.Inscription.Attributes.AttributeType;
import net.samongi.Inscription.Attributes.AttributeTypeFactory;
import net.samongi.Inscription.Attributes.Base.AmountAttributeType;
import net.samongi.Inscription.Attributes.Base.NumericalAttributeType;
import net.samongi.Inscription.Conditions.Condition;
import net.samongi.Inscription.Conditions.Helpers.PlayerConditionHelper;
import net.samongi.Inscription.Conditions.Helpers.TargetEntityConditionHelper;
import net.samongi.Inscription.Glyphs.Glyph;
import net.samongi.Inscription.Inscription;
import net.samongi.Inscription.Player.CacheData;
import net.samongi.Inscription.Player.CacheTypes.CompositeCacheData;
import net.samongi.Inscription.Player.CacheTypes.NumericCacheData;
import net.samongi.Inscription.Player.PlayerData;
import net.samongi.Inscription.Player.Ticks.PlayerTickEvent;
import net.samongi.SamongiLib.Blocks.BlockUtil;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.projectiles.ProjectileSource;

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class DarkDetectionAttributeType extends AmountAttributeType {

    //----------------------------------------------------------------------------------------------------------------//
    private static final String TYPE_IDENTIFIER = "DARK_DETECTION";

    private static final int LIGHT_LEVEL = 8;
    private static final double DENSITY = 1.0;
    private static final int MAX_RADIUS = 30;

    //----------------------------------------------------------------------------------------------------------------//
    private Set<Condition> m_conditions = new HashSet<>();

    //----------------------------------------------------------------------------------------------------------------//
    protected DarkDetectionAttributeType(@Nonnull ConfigurationSection section) throws InvalidConfigurationException {
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
                    cachedData = new DarkDetectionAttributeType.Data();
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
                NumericCacheData numericCacheData = castedData.getCacheData(reduceType, () -> new NumericData(reduceType, reduceType.getInitialAggregator()));

                Inscription.logger.finer("    +C '" + amount + "' reducer '" + reduceType + "'");
                numericCacheData.add(m_conditions, amount);

                Inscription.logger.finer("  Finished caching for " + m_displayName);
                playerData.setData(castedData);
            }

            @Override public String getLoreLine() {
                Glyph glyph = getGlyph();
                String damageStr = getDisplayString(glyph, isPositive(glyph) ? "+" : "-", "");

                String idLine = "" + ChatColor.YELLOW + ChatColor.ITALIC + this.getType().getDisplayName() + " - " + ChatColor.RESET;
                String infoLine = damageStr + ChatColor.YELLOW + " dark detection";
                for (Condition condition : m_conditions) {
                    infoLine += condition.getDisplay();
                }

                return idLine + infoLine;
            }
        };
    }
    //----------------------------------------------------------------------------------------------------------------//
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

    //----------------------------------------------------------------------------------------------------------------//
    public static class Factory extends AttributeTypeFactory {

        @Nonnull @Override public AttributeType construct(@Nonnull ConfigurationSection section) throws InvalidConfigurationException {
            return new DarkDetectionAttributeType(section);
        }
        @Override public Listener getListener() {
            return new Listener() {

                @EventHandler public void onPlayerTick(PlayerTickEvent event) {
                    if (event.getTickCount() % 20 != 0) {
                        return;
                    }
                    // getting the data and basic objects
                    Player player = event.getPlayer();
                    PlayerData playerData = Inscription.getInstance().getPlayerManager().getData(player);
                    CacheData data = playerData.getData(TYPE_IDENTIFIER);
                    if (!(data instanceof Data)) {
                        return;
                    }
                    Data darkDetectionData = (Data) data;

                    int radius = (int) Math.min(MAX_RADIUS, darkDetectionData.calculateAggregate(player));
                    if (radius == 0) {
                        return;
                    }
                    Inscription.logger.finest("[PlayerTickEvent] radius: " + radius);

                    Location playerLocation = player.getLocation();
                    World playerWorld = playerLocation.getWorld();
                    int playerX = playerLocation.getBlockX();
                    int playerY = playerLocation.getBlockY();
                    int playerZ = playerLocation.getBlockZ();

                    Random random = new Random();
                    for (int xRelative = -radius; xRelative < radius; xRelative++) {
                        for (int yRelative = -radius; yRelative < radius; yRelative++) {
                            for (int zRelative = -radius; zRelative < radius; zRelative++) {
                                if (random.nextDouble() > DENSITY) {
                                    continue;
                                }
                                Location location = new Location(playerWorld, playerX + xRelative, playerY + yRelative, playerZ + zRelative);

                                Block block = location.getBlock();
                                if (!block.isEmpty() || block.getLightLevel() > LIGHT_LEVEL) {
                                    continue;
                                }

                                Block lowerBlock = block.getRelative(0, -1, 0);
                                if (!BlockUtil.checkSpawnSurface(lowerBlock)) {
                                    continue;
                                }
                                player.spawnParticle(Particle.TOWN_AURA, location.getX() + 0.5, location.getY() + 0.5, location.getZ() + 0.5, 3, 0.25, 0.25, 0.25);
                            }
                        }
                    }

                }
            };
        }
        @Nonnull @Override public String getAttributeTypeId() {
            return TYPE_IDENTIFIER;
        }
    }
}
