package net.samongi.Inscription.Attributes.Types;

import java.util.HashMap;
import java.util.Map;

import net.samongi.Inscription.Attributes.Base.NumericalAttributeType;
import net.samongi.Inscription.Attributes.GeneralAttributeParser;
import net.samongi.Inscription.Inscription;
import net.samongi.Inscription.Glyphs.Glyph;
import net.samongi.Inscription.Attributes.Attribute;
import net.samongi.Inscription.Attributes.AttributeType;
import net.samongi.Inscription.Attributes.AttributeTypeFactory;
import net.samongi.Inscription.Player.CacheData;
import net.samongi.Inscription.Player.PlayerData;
import net.samongi.Inscription.TypeClass.TypeClasses.EntityClass;
import net.samongi.Inscription.TypeClass.TypeClasses.MaterialClass;

import org.bukkit.ChatColor;
import org.bukkit.Material;
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

public class DamageDoneAttributeType extends NumericalAttributeType {

    //----------------------------------------------------------------------------------------------------------------//
    private static final String TYPE_IDENTIFIER = "DAMAGE";

    //----------------------------------------------------------------------------------------------------------------//
    private EntityClass m_targetEntities;
    private MaterialClass m_targetMaterials;

    //----------------------------------------------------------------------------------------------------------------//

    protected DamageDoneAttributeType(@Nonnull ConfigurationSection section) throws InvalidConfigurationException {
        super(section);

        String targetEntitiesString = section.getString("target-entities");
        if (targetEntitiesString == null) {
            throw new InvalidConfigurationException("'target-entities' is not defined");
        }

        String targetMaterialString = section.getString("target-materials");
        if (targetMaterialString == null) {
            throw new InvalidConfigurationException("'target-materials' is not defined");
        }

        m_targetEntities = EntityClass.handler.getTypeClass(targetEntitiesString);
        if (m_targetEntities == null) {
            throw new InvalidConfigurationException("'" + targetEntitiesString + "' is not a valid entity class.");
        }

        m_targetMaterials = MaterialClass.handler.getTypeClass(targetMaterialString);
        if (m_targetMaterials == null) {
            throw new InvalidConfigurationException("'" + targetMaterialString + "' is not a valid material class.");
        }
    }

    //----------------------------------------------------------------------------------------------------------------//
    public static class Factory extends AttributeTypeFactory {

        @Nonnull @Override public String getAttributeTypeId() {
            return TYPE_IDENTIFIER;
        }

        @Nonnull @Override public AttributeType construct(@Nonnull ConfigurationSection section)
            throws InvalidConfigurationException {
            return new DamageDoneAttributeType(section);
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

                    if (damager instanceof Player) {
                        // getting the data and basic objects
                        Player playerDamager = (Player) damager;
                        PlayerData player_data = Inscription.getInstance().getPlayerManager().getData(playerDamager);
                        CacheData data = player_data.getData(DamageDoneAttributeType.TYPE_IDENTIFIER);
                        if (!(data instanceof DamageDoneAttributeType.Data)) {
                            return;
                        }
                        DamageDoneAttributeType.Data damageData = (DamageDoneAttributeType.Data) data;

                        // getting damage bonus relavant information
                        ItemStack itemInHand = playerDamager.getInventory().getItemInMainHand();
                        Material material = Material.AIR;
                        if (itemInHand != null) {
                            material = itemInHand.getType();
                        }
                        EntityType entity = event.getEntity().getType();

                        // adding up the damage bonus
                        double damage_bonus = 0;
                        damage_bonus += damageData.get();
                        damage_bonus += damageData.get(material);
                        damage_bonus += damageData.get(entity);
                        damage_bonus += damageData.get(entity, material);

                        Inscription.logger.finest("[Damage Event] Damage Bonus: " + damage_bonus);

                        event.setDamage(event.getDamage() * (1 + damage_bonus));
                    } else {
                        return;
                    }
                }
            };
        }
    }

    @Override public Attribute generate() {
        return new Attribute(this) {

            // Caching the data
            @Override public void cache(PlayerData data) {
                CacheData cached_data = data.getData(DamageDoneAttributeType.TYPE_IDENTIFIER);
                if (cached_data == null) {
                    cached_data = new DamageDoneAttributeType.Data();
                }
                if (!(cached_data instanceof DamageDoneAttributeType.Data)) {
                    return;
                }

                Inscription.logger.fine("  Caching attribute for " + m_displayName);
                Inscription.logger.fine("    'target_entities' is global?: " + m_targetEntities.isGlobal());
                Inscription.logger.fine("    'target_materials' is global?: " + m_targetMaterials.isGlobal());

                DamageDoneAttributeType.Data damageData = (DamageDoneAttributeType.Data) cached_data;
                double damage = getNumber(this.getGlyph()); // getting the damage for
                // the attribute
                if (m_targetEntities.isGlobal() && m_targetMaterials.isGlobal()) {
                    double d = damageData.get();
                    Inscription.logger.fine("  +C Added '" + damage + "' damage");
                    damageData.set(d + damage);
                } else if (m_targetEntities.isGlobal()) {
                    for (Material m : m_targetMaterials.getMaterials()) {
                        double d = damageData.get(m);
                        Inscription.logger.fine("  +C Added '" + damage + "' damage to '" + m.toString() + "'");
                        damageData.set(m, d + damage);
                    }
                } else if (m_targetMaterials.isGlobal()) {
                    for (EntityType e : m_targetEntities.getEntities()) {
                        double d = damageData.get(e);
                        Inscription.logger.fine("  +C Added '" + damage + "' damage to '" + e.toString() + "'");
                        damageData.set(e, d + damage);
                    }
                } else {
                    for (EntityType e : m_targetEntities.getEntities())
                        for (Material m : m_targetMaterials.getMaterials()) {
                            double d = damageData.get(e, m);
                            Inscription.logger.fine("  +C Added '" + damage + "' damage to '" + e.toString() + "|" + m.toString() + "'");
                            damageData.set(e, m, d + damage);
                        }
                }
                Inscription.logger.fine("  Finished caching for " + m_displayName);
                data.setData(damageData); // setting the data again.
            }

            @Override public String getLoreLine() {
                String damageStr = getDisplayString(this.getGlyph(), 100, "+", "%");
                String itemClass = m_targetMaterials.getName();
                String entityClass = m_targetEntities.getName();

                String info_line =
                    damageStr + ChatColor.YELLOW + " damage to " + ChatColor.BLUE + entityClass + ChatColor.YELLOW + " using " + ChatColor.BLUE + itemClass;

                return "" + ChatColor.YELLOW + ChatColor.ITALIC + this.getType().getDisplayName() + " - " + ChatColor.RESET + info_line;
            }
        };
    }

    public static class Data implements CacheData {

        // Data members of the data
        private double global; // Global damage modifier
        private HashMap<Material, Double> material_damage = new HashMap<>();
        private HashMap<EntityType, Double> entity_damage = new HashMap<>();
        private HashMap<EntityType, HashMap<Material, Double>> material_entity_damage = new HashMap<>();

        // Setters
        public void set(Double amount) {
            this.global = amount;
        }
        public void set(Material mat, Double amount) {
            this.material_damage.put(mat, amount);
        }
        public void set(EntityType entity, Double amount) {
            this.entity_damage.put(entity, amount);
        }
        public void set(EntityType entity, Material mat, Double amount) {
            if (!material_entity_damage.containsKey(entity)) {
                material_entity_damage.put(entity, new HashMap<Material, Double>());
            }
            Map<Material, Double> e_damage = material_entity_damage.get(entity);
            e_damage.put(mat, amount);
        }

        // Getters
        public double get() {
            return this.global;
        }
        public double get(Material mat) {
            if (!this.material_damage.containsKey(mat)) {
                return 0.0;
            }
            return this.material_damage.get(mat);
        }
        public double get(EntityType entity) {
            if (!this.entity_damage.containsKey(entity)) {
                return 0.0;
            }
            return this.entity_damage.get(entity);
        }
        public double get(EntityType entity, Material mat) {
            if (!material_entity_damage.containsKey(entity)) {
                return 0;
            }
            Map<Material, Double> e_damage = material_entity_damage.get(entity);
            if (!e_damage.containsKey(mat)) {
                return 0;
            }
            return e_damage.get(mat);
        }

        // Clears the saved data
        @Override public void clear() {
            this.global = 0;
            this.material_damage = new HashMap<>();
            this.entity_damage = new HashMap<>();
            this.material_entity_damage = new HashMap<>();
        }

        @Override public String getType() {
            return TYPE_IDENTIFIER;
        }

        @Override public String getData() {
            // TODO
            return "";
        }

    } // End data definition

}
