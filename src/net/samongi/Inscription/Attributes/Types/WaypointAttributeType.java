package net.samongi.Inscription.Attributes.Types;

import net.md_5.bungee.api.ChatColor;
import net.samongi.Inscription.Altars;
import net.samongi.Inscription.Attributes.Attribute;
import net.samongi.Inscription.Attributes.AttributeType;
import net.samongi.Inscription.Attributes.AttributeTypeConstructor;
import net.samongi.Inscription.Attributes.Base.AmountAttributeType;
import net.samongi.Inscription.Attributes.Base.MultiplierAttributeType;
import net.samongi.Inscription.Attributes.GeneralAttributeParser;
import net.samongi.Inscription.Inscription;
import net.samongi.Inscription.Player.CacheData;
import net.samongi.Inscription.Player.PlayerData;
import net.samongi.Inscription.TypeClasses.BiomeClass;
import net.samongi.Inscription.TypeClasses.MaterialClass;
import net.samongi.Inscription.Waypoints.Waypoint;
import net.samongi.SamongiLib.Blocks.Altar;
import net.samongi.SamongiLib.Menu.InventoryMenu;
import net.samongi.SamongiLib.Tuple.Tuple;
import net.samongi.SamongiLib.Vector.SamIntVector;
import net.samongi.SamongiLib.Vector.SamVector;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Banner;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;

import javax.annotation.Nonnull;
import java.util.*;

public class WaypointAttributeType extends AmountAttributeType {

    //--------------------------------------------------------------------------------------------------------------------//
    public static final String TYPE_IDENTIFIER = "WAYPOINT";

    //--------------------------------------------------------------------------------------------------------------------//
    private BiomeClass m_fromBiome = BiomeClass.getGlobal("any items");
    private BiomeClass m_toBiome = BiomeClass.getGlobal("any items");

    //--------------------------------------------------------------------------------------------------------------------//
    protected WaypointAttributeType(GeneralAttributeParser parser) {
        super(parser);
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

                Inscription.logger.finer("  Caching attribute for " + m_typeDescription);
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

                Inscription.logger.finer("  Finished caching for " + m_typeDescription);
                playerData.setData(amountData);
            }

            @Override public String getLoreLine() {
                String amountString = ((WaypointAttributeType) this.getType()).getDisplayString(this.getGlyph(), "+", "m");
                String fromClass = m_fromBiome.getName();
                String toClass = m_toBiome.getName();

                String infoLine =
                    amountString + ChatColor.YELLOW + " distance for warping from " + ChatColor.BLUE + fromClass + ChatColor.YELLOW + " biomes to "
                        + ChatColor.BLUE + toClass + ChatColor.YELLOW + " biomes";
                return "" + ChatColor.YELLOW + ChatColor.ITALIC + this.getType().getNameDescriptor() + " - " + ChatColor.RESET + infoLine;
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
    public static class Constructor extends AttributeTypeConstructor {

        @Override public AttributeType construct(ConfigurationSection section) {

            GeneralAttributeParser parser = new GeneralAttributeParser(section, TYPE_IDENTIFIER);
            if (!parser.checkType()) {
                return null;
            }
            if (!parser.loadInfo()) {
                return null;
            }

            WaypointAttributeType attributeType = new WaypointAttributeType(parser);

            double minAmount = section.getInt("min-amount");
            double maxAmount = section.getInt("max-amount");
            if (minAmount > maxAmount) {
                Inscription.logger.warning(section.getName() + " : min amount is bigger than max chance");
                return null;
            }

            attributeType.setMin(minAmount);
            attributeType.setMax(maxAmount);

            String fromBiome = section.getString("from-biome-class");
            if (fromBiome != null) {
                BiomeClass biomeClass = Inscription.getInstance().getTypeClassManager().getBiomeClass(fromBiome);
                if (biomeClass == null) {
                    Inscription.logger.warning("[WaypointAttributeType] '" + fromBiome + "' is not a valid biome class.");
                    return null;
                }
                attributeType.m_fromBiome = biomeClass;
            }

            String toBiome = section.getString("to-biome-class");
            if (toBiome != null) {
                BiomeClass biomeClass = Inscription.getInstance().getTypeClassManager().getBiomeClass(toBiome);
                if (biomeClass == null) {
                    Inscription.logger.warning("[WaypointAttributeType] '" + toBiome + "' is not a valid biome class.");
                    return null;
                }
                attributeType.m_toBiome = biomeClass;
            }

            return attributeType;
        }

        @Override public Listener getListener() {
            return new Listener() {


            };
        }
    }
}
