package net.samongi.Inscription.Attributes.Types;

import java.util.HashMap;
import java.util.Random;

import net.md_5.bungee.api.ChatColor;
import net.samongi.Inscription.Attributes.Base.NumericalAttributeType;
import net.samongi.Inscription.Inscription;
import net.samongi.Inscription.Attributes.Attribute;
import net.samongi.Inscription.Attributes.AttributeType;
import net.samongi.Inscription.Attributes.AttributeTypeFactory;
import net.samongi.Inscription.Player.CacheData;
import net.samongi.Inscription.Player.PlayerData;
import net.samongi.Inscription.TypeClass.TypeClasses.MaterialClass;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nonnull;

public class DurabilityAttributeType extends NumericalAttributeType {

    //--------------------------------------------------------------------------------------------------------------------//
    private static final String TYPE_IDENTIFIER = "DURABILITY";

    //--------------------------------------------------------------------------------------------------------------------//
    private MaterialClass m_targetMaterials = null;

    //----------------------------------------------------------------------------------------------------------------//
    protected DurabilityAttributeType(@Nonnull ConfigurationSection section) throws InvalidConfigurationException {
        super(section);

        String targetMaterialsString = section.getString("target-materials");
        if (targetMaterialsString == null) {
            throw new InvalidConfigurationException("'target-materials' is not defined");
        }

        m_targetMaterials = MaterialClass.handler.getTypeClass(targetMaterialsString);
        if (m_targetMaterials == null) {
            throw new InvalidConfigurationException("'" + targetMaterialsString + "' is not a valid material class.");
        }


    }
    @Override public Attribute generate() {
        return new Attribute(this) {

            @Override public void cache(PlayerData playerData) {
                CacheData cached_data = playerData.getData(DurabilityAttributeType.TYPE_IDENTIFIER);
                if (cached_data == null) {
                    cached_data = new DurabilityAttributeType.Data();
                }
                if (!(cached_data instanceof DurabilityAttributeType.Data)) {
                    return;
                }

                Inscription.logger.finer("Caching attribute for " + m_displayName);
                Inscription.logger.finer(" - 'm_toolMaterials' is global?: " + m_targetMaterials.isGlobal());

                DurabilityAttributeType.Data data = (DurabilityAttributeType.Data) cached_data;
                double chance = getNumber(this.getGlyph());
                if (m_targetMaterials.isGlobal()) {
                    double currentValue = data.get();
                    /* Multiplicative bonus */
                    double newValue = currentValue + (1 - currentValue) * chance;
                    data.set(newValue > 1 ? 1 : newValue);

                    Inscription.logger.finer("  +C Added '" + chance + "' bonus " + currentValue + "-->" + newValue);
                } else {
                    for (Material t : m_targetMaterials.getMaterials()) {
                        double currentValue = data.getTool(t);
                        /* Multiplicative bonus */
                        double newValue = currentValue + (1 - currentValue) * chance;
                        data.setTool(t, newValue > 1 ? 1 : newValue);

                        Inscription.logger.finer("  +C Added '" + chance + "' bonus to '" + t.toString() + "' " + currentValue + "->" + newValue);
                    }
                }
                playerData.setData(data);
            }

            @Override public String getLoreLine() {
                String chanceString = getDisplayString(this.getGlyph(), "+", "%");
                String toolClass = m_targetMaterials.getName();

                String info_line = chanceString + ChatColor.YELLOW + " chance to not use durability using " + ChatColor.BLUE + toolClass;
                return this.getType().getLoreLine() + info_line;
            }

        };
    }
    //--------------------------------------------------------------------------------------------------------------------//
    public static class Data implements CacheData {

        /* Data members of the the data */
        private double global = 0.0;
        private HashMap<Material, Double> tool_chance = new HashMap<>();

        /* *** Setters *** */
        public void set(Double amount) {
            this.global = amount;
        }
        public void setTool(Material mat, double amount) {
            this.tool_chance.put(mat, amount);
        }

        /* *** Getters *** */
        public double get() {
            return this.global;
        }
        public double getTool(Material mat) {
            if (!this.tool_chance.containsKey(mat)) {
                return 0;
            }
            return this.tool_chance.get(mat);
        }

        @Override public void clear() {
            this.global = 0.0;
            this.tool_chance = new HashMap<>();
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

        //----------------------------------------------------------------------------------------------------------------//
        @Nonnull @Override public String getAttributeTypeId() {
            return TYPE_IDENTIFIER;
        }
        //----------------------------------------------------------------------------------------------------------------//
        @Nonnull @Override public AttributeType construct(@Nonnull ConfigurationSection section) throws InvalidConfigurationException {
            return new DurabilityAttributeType(section);
        }

        //----------------------------------------------------------------------------------------------------------------//
        @Override public Listener getListener() {
            return new Listener() {

                @EventHandler public void onItemDamaged(PlayerItemDamageEvent event) {
                    Player player = event.getPlayer();
                    PlayerData playerData = Inscription.getInstance().getPlayerManager().getData(player);
                    CacheData cacheData = playerData.getData(DurabilityAttributeType.TYPE_IDENTIFIER);
                    if (!(cacheData instanceof DurabilityAttributeType.Data)) {
                        return;
                    }
                    DurabilityAttributeType.Data data = (DurabilityAttributeType.Data) cacheData;

                    ItemStack tool = event.getItem();
                    if (tool == null) {
                        return;
                    }

                    Material toolMaterial = tool.getType();

                    /*
                     * Calculating the chance for there not to be any durability, this has
                     * to be between 0 and 1. The chances are multiplicative and as such
                     * makes it hard to reach 100% chance.
                     */
                    double noDurabilityChance = data.get();
                    noDurabilityChance += (1 - noDurabilityChance) * data.getTool(toolMaterial);

                    Inscription.logger.finest("[PlayerItemDamageEvent] No Durability Chance: " + noDurabilityChance);

                    /*
                     * Canceling the event if its larger than the random number, note that
                     * this will prevent an AMOUNT of durability loss. In the future we
                     * may want to have a chance
                     * for each durability.
                     */
                    Random random = new Random();
                    if (random.nextDouble() < noDurabilityChance) {
                        event.setCancelled(true);
                    }
                }
            };
        }

        //----------------------------------------------------------------------------------------------------------------//
    }

    //--------------------------------------------------------------------------------------------------------------------//
}
