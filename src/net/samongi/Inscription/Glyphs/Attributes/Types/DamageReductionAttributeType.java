package net.samongi.Inscription.Glyphs.Attributes.Types;

import net.md_5.bungee.api.ChatColor;
import net.samongi.Inscription.Glyphs.Attributes.Attribute;
import net.samongi.Inscription.Glyphs.Attributes.AttributeType;
import net.samongi.Inscription.Glyphs.Attributes.AttributeTypeConstructor;
import net.samongi.Inscription.Glyphs.Attributes.Base.MultiplierAttributeType;
import net.samongi.Inscription.Inscription;
import net.samongi.Inscription.Player.CacheData;
import net.samongi.Inscription.Player.PlayerData;
import net.samongi.Inscription.TypeClasses.DamageClass;
import net.samongi.Inscription.TypeClasses.MaterialClass;
import net.samongi.SamongiLib.Exceptions.InvalidConfigurationException;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class DamageReductionAttributeType extends MultiplierAttributeType {

    private static final long serialVersionUID = 4295276441414911104L;
    private static final String TYPE_IDENTIFIER = "DAMAGE_REDUCTION";

    //----------------------------------------------------------------------------------------------------------------//

    private MaterialClass m_armorMaterials = MaterialClass.getGlobal("any items");
    private DamageClass m_damageTypes = DamageClass.getGlobal("any damage");

    //----------------------------------------------------------------------------------------------------------------//
    protected DamageReductionAttributeType(String typeName, String description) {
        super(typeName, description);
    }

    //----------------------------------------------------------------------------------------------------------------//

    @Override public Attribute generate() {
        return new Attribute(this) {

            private static final long serialVersionUID = -1151134999201153827L;

            @Override public void cache(PlayerData playerData) {
                CacheData cachedData = playerData.getData(DamageReductionAttributeType.TYPE_IDENTIFIER);
                if (cachedData == null) {
                    cachedData = new DamageReductionAttributeType.Data();
                }
                if (!(cachedData instanceof DamageReductionAttributeType.Data)) {
                    return;
                }

                Inscription.logger.finer("Caching attribute for " + typeDescription);
                Inscription.logger.finer("  'm_armorMaterials' is global?: " + m_armorMaterials.isGlobal());
                Inscription.logger.finer("  'm_damageTypes' is global?: " + m_damageTypes.isGlobal());

                DamageReductionAttributeType.Data data = (DamageReductionAttributeType.Data) cachedData;

                double multiplier = getMultiplier(this.getGlyph());
                if (m_armorMaterials.isGlobal() && m_damageTypes.isGlobal()) {

                    double currentValue = data.get();
                    double newValue = currentValue + (1 - currentValue) * multiplier;
                    data.set(newValue);
                    Inscription.logger.finer("  +C Added '" + multiplier + "' bonus " + currentValue + "->" + newValue);
                } else if (m_damageTypes.isGlobal()) {
                    for (Material armorMaterial : m_armorMaterials.getMaterials()) {
                        double currentValue = data.getArmor(armorMaterial);
                        double newValue = currentValue + (1 - currentValue) * multiplier;
                        data.setArmor(armorMaterial, newValue);

                        Inscription.logger.finer("  +C Added '" + multiplier + "' bonus to '" + armorMaterial.toString() + "' " + currentValue + "->" + newValue);
                    }
                } else if (m_armorMaterials.isGlobal()) {
                    for (EntityDamageEvent.DamageCause damageType : m_damageTypes.getDamageTypes()) {
                        double currentValue = data.getDamageType(damageType);
                        double newValue = currentValue + (1 - currentValue) * multiplier;
                        data.setDamageType(damageType, newValue);

                        Inscription.logger.finer("  +C Added '" + multiplier + "' bonus to '" + damageType.toString() + "' " + currentValue + "->" + newValue);
                    }
                } else {
                    for (Material armorMaterial : m_armorMaterials.getMaterials()) {
                        for (EntityDamageEvent.DamageCause damageType : m_damageTypes.getDamageTypes()) {
                            double currentValue = data.getArmorDamageType(armorMaterial, damageType);
                            double newValue = currentValue + (1 - currentValue) * multiplier;
                            data.setArmorDamageType(armorMaterial, damageType, newValue);

                            Inscription.logger.finer("  +C Added '" + multiplier + "' bonus to '" + armorMaterial.toString() + "', ''" + damageType.toString() + "' " + currentValue + "->" + newValue);
                        }
                    }
                }

                playerData.setData(data); // setting the data again.
                Inscription.logger.finer("Finished caching for " + typeDescription);
            }

            @Override public String getLoreLine() {
                String multiplierString = getMultiplierPercentageString(this.getGlyph());
                String armorClass = m_armorMaterials.getName();
                String damageClass = m_damageTypes.getName();

                String infoLine =
                    ChatColor.BLUE + "-" + multiplierString + "%" + ChatColor.YELLOW + " " + ChatColor.BLUE + damageClass + ChatColor.YELLOW + " damage while wearing " + ChatColor.BLUE + armorClass;
                return this.getType().getDescriptionLoreLine() + infoLine;
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
    public static class Constructor extends AttributeTypeConstructor {

        @Override public AttributeType construct(ConfigurationSection section) throws InvalidConfigurationException {
            String type = section.getString("type");
            if (type == null || !type.toUpperCase().equals(TYPE_IDENTIFIER))
                return null;

            String name = section.getString("name");
            if (name == null)
                return null;

            String descriptor = section.getString("descriptor");
            if (descriptor == null)
                return null;

            double minReduction = section.getDouble("min-reduction");
            double maxReduction = section.getDouble("max-reduction");
            if (minReduction > maxReduction) {
                Inscription.logger.warning(section.getName() + " : min reduction is bigger than max chance");
                return null;
            }
            double rarityMultiplier = section.getDouble("rarity-multiplier");

            String targetDamageTypes = section.getString("target-damage");
            String targetArmor = section.getString("target-materials");

            DamageReductionAttributeType attributeType = new DamageReductionAttributeType(name, descriptor);
            attributeType.setMin(minReduction);
            attributeType.setMax(maxReduction);
            attributeType.setRarityMultiplier(rarityMultiplier);

            int modelIncrement = section.getInt("model", 0);
            attributeType.setModelIncrement(modelIncrement);

            attributeType.baseExperience = AttributeType.getIntMap(section.getConfigurationSection("base-experience"));
            attributeType.levelExperience = AttributeType.getIntMap(section.getConfigurationSection("level-experience"));

            if (targetArmor != null) {
                MaterialClass materialClass = Inscription.getInstance().getTypeClassManager().getMaterialClass(targetArmor);
                if (materialClass == null) {
                    throw new InvalidConfigurationException("Material class was undefined:" + targetArmor);
                }
                attributeType.m_armorMaterials = materialClass;
            }
            if (targetDamageTypes != null) {
                DamageClass damageTypeClass = Inscription.getInstance().getTypeClassManager().getDamageClass(targetDamageTypes);
                if (damageTypeClass == null) {
                    throw new InvalidConfigurationException("Damage class was undefined:" + targetDamageTypes);
                }
                attributeType.m_damageTypes = damageTypeClass;
            }

            return attributeType;
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
