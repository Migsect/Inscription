package net.samongi.Inscription.Attributes.Types;

import net.md_5.bungee.api.ChatColor;
import net.samongi.Inscription.Attributes.Attribute;
import net.samongi.Inscription.Attributes.AttributeType;
import net.samongi.Inscription.Attributes.AttributeTypeFactory;
import net.samongi.Inscription.Attributes.Base.NumericalAttributeType;
import net.samongi.Inscription.Inscription;
import net.samongi.Inscription.Player.CacheData;
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
import java.util.Map;

public class DamageReductionAttributeType extends NumericalAttributeType {

    private static final String TYPE_IDENTIFIER = "DAMAGE_REDUCTION";

    //----------------------------------------------------------------------------------------------------------------//
    private MaterialClass m_targetArmorMaterials = null;
    private DamageClass m_targetDamageTypes = null;

    //----------------------------------------------------------------------------------------------------------------//
    protected DamageReductionAttributeType(@Nonnull ConfigurationSection section) throws InvalidConfigurationException {
        super(section);

        String targetArmorString = section.getString("target-armor");
        if (targetArmorString == null) {
            throw new InvalidConfigurationException("'target-armor' is not defined");
        }

        String targetDamageTypesString = section.getString("target-damage-type");
        if (targetDamageTypesString == null) {
            throw new InvalidConfigurationException("'target-damage-type' is not defined");
        }

        m_targetArmorMaterials = MaterialClass.handler.getTypeClass(targetArmorString);
        if (m_targetArmorMaterials == null) {
            throw new InvalidConfigurationException("'" + targetArmorString + "' is not a valid material class.");
        }

        m_targetDamageTypes = DamageClass.handler.getTypeClass(targetDamageTypesString);
        if (m_targetDamageTypes == null) {
            throw new InvalidConfigurationException("'" + targetDamageTypesString + "' is not a valid material class.");
        }


    }

        //----------------------------------------------------------------------------------------------------------------//

    @Override public Attribute generate() {
        return new Attribute(this) {

            @Override public void cache(PlayerData playerData) {
                CacheData cachedData = playerData.getData(DamageReductionAttributeType.TYPE_IDENTIFIER);
                if (cachedData == null) {
                    cachedData = new DamageReductionAttributeType.Data();
                }
                if (!(cachedData instanceof DamageReductionAttributeType.Data)) {
                    return;
                }

                Inscription.logger.finer("Caching attribute for " + m_displayName);
                Inscription.logger.finer("  'm_armorMaterials' is global?: " + m_targetArmorMaterials.isGlobal());
                Inscription.logger.finer("  'm_damageTypes' is global?: " + m_targetDamageTypes.isGlobal());

                DamageReductionAttributeType.Data data = (DamageReductionAttributeType.Data) cachedData;

                double multiplier = getNumber(this.getGlyph());
                if (m_targetArmorMaterials.isGlobal() && m_targetDamageTypes.isGlobal()) {

                    double currentValue = data.get();
                    double newValue = currentValue + (1 - currentValue) * multiplier;
                    data.set(newValue);
                    Inscription.logger.finer("  +C Added '" + multiplier + "' bonus " + currentValue + "->" + newValue);
                } else if (m_targetDamageTypes.isGlobal()) {
                    for (Material armorMaterial : m_targetArmorMaterials.getMaterials()) {
                        double currentValue = data.getArmor(armorMaterial);
                        double newValue = currentValue + (1 - currentValue) * multiplier;
                        data.setArmor(armorMaterial, newValue);

                        Inscription.logger
                            .finer("  +C Added '" + multiplier + "' bonus to '" + armorMaterial.toString() + "' " + currentValue + "->" + newValue);
                    }
                } else if (m_targetArmorMaterials.isGlobal()) {
                    for (EntityDamageEvent.DamageCause damageType : m_targetDamageTypes.getDamageTypes()) {
                        double currentValue = data.getDamageType(damageType);
                        double newValue = currentValue + (1 - currentValue) * multiplier;
                        data.setDamageType(damageType, newValue);

                        Inscription.logger.finer("  +C Added '" + multiplier + "' bonus to '" + damageType.toString() + "' " + currentValue + "->" + newValue);
                    }
                } else {
                    for (Material armorMaterial : m_targetArmorMaterials.getMaterials()) {
                        for (EntityDamageEvent.DamageCause damageType : m_targetDamageTypes.getDamageTypes()) {
                            double currentValue = data.getArmorDamageType(armorMaterial, damageType);
                            double newValue = currentValue + (1 - currentValue) * multiplier;
                            data.setArmorDamageType(armorMaterial, damageType, newValue);

                            Inscription.logger.finer(
                                "  +C Added '" + multiplier + "' bonus to '" + armorMaterial.toString() + "', ''" + damageType.toString() + "' " + currentValue
                                    + "->" + newValue);
                        }
                    }
                }

                playerData.setData(data); // setting the data again.
                Inscription.logger.finer("Finished caching for " + m_displayName);
            }

            @Override public String getLoreLine() {
                String multiplierString = getDisplayString(this.getGlyph(), 100, "-", "%");
                String armorClass = m_targetArmorMaterials.getName();
                String damageClass = m_targetDamageTypes.getName();

                String infoLine =
                    multiplierString + ChatColor.YELLOW + " " + ChatColor.BLUE + damageClass + ChatColor.YELLOW + " damage while wearing " + ChatColor.BLUE
                        + armorClass;
                return this.getType().getLoreLine() + infoLine;
            }
        };
    }

    public static class Data implements CacheData {

        private double m_global = 0.0;
        private Map<Material, Double> m_armorReducation = new HashMap<>();
        private Map<EntityDamageEvent.DamageCause, Double> m_damageReduction = new HashMap<>();
        private HashMap<Material, HashMap<EntityDamageEvent.DamageCause, Double>> m_armorDamageReduction = new HashMap<>();

        //----------------------------------------------------------------------------------------------------------------//

        public void set(Double amount) {
            this.m_global = amount;
        }
        public double get() {
            return this.m_global;
        }
        public void setArmor(Material armorMaterial, double amount) {
            this.m_armorReducation.put(armorMaterial, amount);
        }
        public double getArmor(Material armorMaterial) {
            return this.m_armorReducation.getOrDefault(armorMaterial, 0.0);
        }
        public void setDamageType(EntityDamageEvent.DamageCause damageType, double amount) {
            this.m_damageReduction.put(damageType, amount);
        }
        public double getDamageType(EntityDamageEvent.DamageCause damageType) {
            return this.m_damageReduction.getOrDefault(damageType, 0.0);
        }
        public void setArmorDamageType(Material armorMaterial, EntityDamageEvent.DamageCause damageType, double amount) {
            if (!this.m_armorDamageReduction.containsKey(armorMaterial)) {
                this.m_armorDamageReduction.put(armorMaterial, new HashMap<>());
            }
            HashMap<EntityDamageEvent.DamageCause, Double> damageReduction = this.m_armorDamageReduction.get(armorMaterial);
            damageReduction.put(damageType, amount);
        }
        public double getArmorDamageType(Material armorMaterial, EntityDamageEvent.DamageCause damageType) {
            if (!this.m_armorDamageReduction.containsKey(armorMaterial)) {
                return 0;
            }
            HashMap<EntityDamageEvent.DamageCause, Double> block_bonus = this.m_armorDamageReduction.get(armorMaterial);

            return block_bonus.getOrDefault(damageType, 0.0);
        }
        //----------------------------------------------------------------------------------------------------------------//
        @Override public void clear() {
            this.m_global = 0.0;
            this.m_armorReducation.clear();
            this.m_damageReduction.clear();
            this.m_armorDamageReduction.clear();
        }
        @Override public String getType() {
            return TYPE_IDENTIFIER;
        }
        @Override public String getData() {
            // TODO This returns the data as a string
            return "";
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

                    double baseReduction = data.get();
                    double armorReduction = 0; // Can be partial based on armor loadout.
                    double damageTypeReduction = data.getDamageType(damageCause);
                    double armorDamageTypeReduction = 0; // Can be partial based on loadout.

                    PlayerInventory playerInventory = player.getInventory();
                    ItemStack[] playerArmor = playerInventory.getArmorContents();
                    for (ItemStack armor : playerArmor) {
                        if (armor == null) {
                            continue;
                        }
                        Material armorMaterial = armor.getType();
                        armorReduction += data.getArmor(armorMaterial) * 0.25;
                        armorDamageTypeReduction += data.getArmorDamageType(armorMaterial, damageCause) * 0.25;
                    }

                    double totalMultiplier = (1 - baseReduction) * (1 - armorReduction) * (1 - damageTypeReduction) * (1 - armorDamageTypeReduction);
                    double reducedDamage = Math.max(0, damageDone * totalMultiplier);
                    event.setDamage(reducedDamage);

                    Inscription.logger.finest("" + "[EntityDamageEvent] DamageReduction " + (1 - totalMultiplier) + " " + damageDone + " -> " + reducedDamage);
                }
            };
        }
    }
}
