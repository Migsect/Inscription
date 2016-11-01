package net.samongi.Inscription.Glyphs.Attributes.Types;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;

import net.samongi.Inscription.Inscription;
import net.samongi.Inscription.Glyphs.Glyph;
import net.samongi.Inscription.Glyphs.Attributes.Attribute;
import net.samongi.Inscription.Glyphs.Attributes.AttributeType;
import net.samongi.Inscription.Glyphs.Attributes.AttributeTypeConstructor;
import net.samongi.Inscription.Player.CacheData;
import net.samongi.Inscription.Player.PlayerData;
import net.samongi.Inscription.TypeClasses.EntityClass;
import net.samongi.Inscription.TypeClasses.MaterialClass;

public class DamageAttributeType implements AttributeType
{
  private static void log(String message){Inscription.log("[DamageAttributeType] " + message);}
  private static void logDebug(String message){if(Inscription.debug()) DamageAttributeType.log(Inscription.debug_tag + message);}
  @SuppressWarnings("unused")
  private static boolean debug(){return Inscription.debug();}
  
  private static final long serialVersionUID = -8367848352344774887L;
  private static final String TYPE_IDENTIFIER = "DAMAGE";
  
  // The type name
  private final String type_name;
  // The description of the attribute type (used within lore)
  private final String name_description;
  
  // The entity class that this DamageAttributeType will target
  private EntityClass target_entities;
  // The material class that this DamageAttributeType will apply to
  private MaterialClass target_materials;
  
  // The minimum and maximum damage multiplier
  private double min_damage;
  private double max_damage;
  // The rarity multiplier
  private double rarity_mult;
  
  // experience values
  private Map<String, Integer> base_experience;
  private Map<String, Integer> level_experience;
  
  // TODO this constructor should be private
  /**Consturctor for a DamageAttributeType
   * 
   * @param type_name The name of the type (display)
   * @param description An item name descriptor
   * @param min_damage The min damage this damage attribute can have
   * @param max_damage The max damage this damage attribute can have
   * @param rarity_mult The multiplier based on rarity level of the item.
   */
  private DamageAttributeType(String type_name, String description, double min_damage, double max_damage, double rarity_mult)
  {
    // TODO reduce the variables and handle them through setters instead
    this.type_name = type_name;
    this.name_description = description;
    
    this.target_entities = EntityClass.getGlobalLiving("all creatures");
    this.target_materials = MaterialClass.getGlobal("any items");
    
    this.min_damage = min_damage;
    this.max_damage = max_damage;
    this.rarity_mult = rarity_mult;
  }
  
  @Override
  public double getRarityMultiplier(){return this.rarity_mult;}
  
  public static class Constructor implements AttributeTypeConstructor
  {
    @Override
    public AttributeType construct(ConfigurationSection section)
    {
      String type = section.getString("type");
      if(type == null) return null;
      if(!type.toUpperCase().equals("DAMAGE")) return null;
      
      String name = section.getString("name");
      if(name == null) return null;
      
      String descriptor = section.getString("descriptor");
      if(descriptor == null) return null;
      
      double min_damage = section.getDouble("min-damage");
      double max_damage = section.getDouble("max-damage");
      if(min_damage > max_damage)
      {
        DamageAttributeType.log(section.getName() + " : min damage is bigger than max damage");
        return null;
      }
      double rarity_mult = section.getDouble("rarity-multiplier");
      
      String target_entities = section.getString("target-entities");
      String target_materials = section.getString("target-materials");
      
      DamageAttributeType attribute_type = new DamageAttributeType(name, descriptor, min_damage, max_damage, rarity_mult);
      attribute_type.base_experience = AttributeType.getIntMap(section.getConfigurationSection("base-experience"));
      attribute_type.level_experience = AttributeType.getIntMap(section.getConfigurationSection("level-experience"));
      
      // Setting all the targeting if there is any
      if(target_entities != null) 
      {
        EntityClass e_class = Inscription.getInstance().getTypeClassManager().getEntityClass(target_entities);
        attribute_type.setTargetEntities(e_class);
      }
      if(target_materials != null)
      {
        MaterialClass m_class = Inscription.getInstance().getTypeClassManager().getMaterialClass(target_materials);
        attribute_type.setTargetMaterials(m_class);
      }
      
      return attribute_type;
    }

    @Override
    public Listener getListener()
    {
      return new Listener()
      {
        @EventHandler
        public void onEntityDamageByEntity(EntityDamageByEntityEvent event)
        {
          if(event.isCancelled()) return;
          
          Entity damager = event.getDamager();
          if(damager instanceof Player)
          {
            // getting the data and basic objects
            Player player_damager = (Player) damager;
            PlayerData player_data = Inscription.getInstance().getPlayerManager().getData(player_damager);
            CacheData data = player_data.getData(DamageAttributeType.TYPE_IDENTIFIER);
            if(!(data instanceof DamageAttributeType.Data)) return;
            DamageAttributeType.Data damage_data = (DamageAttributeType.Data) data;
            
            // getting damage bonus relavant information
            ItemStack item_in_hand = player_damager.getInventory().getItemInMainHand();
            Material material = Material.AIR;
            if(item_in_hand != null) material = item_in_hand.getType();
            EntityType entity = event.getEntity().getType();
            
            // adding up the damage bonus
            double damage_bonus = 0;
            damage_bonus += damage_data.get();
            damage_bonus += damage_data.get(material);
            damage_bonus += damage_data.get(entity);
            damage_bonus += damage_data.get(entity, material);
            
            DamageAttributeType.logDebug("[Damage Event] Damage Bonus: " + damage_bonus);
            
            event.setDamage(event.getDamage() * (1 + damage_bonus));
          }
          else return;
        }
      }; // End Attribute Listener definition
    }
  }

  @Override
  public Attribute generate()
  {
    return new Attribute(this){
      private static final long serialVersionUID = 7422514378631333199L;

      // Caching the data
      @Override
      public void cache(PlayerData data)
      {
        CacheData cached_data = data.getData(DamageAttributeType.TYPE_IDENTIFIER);
        if(cached_data == null) cached_data = new DamageAttributeType.Data();
        if(!(cached_data instanceof DamageAttributeType.Data)) return; // Checking to make sure it is the right data type
        
        DamageAttributeType.logDebug("  Caching attribute for " + name_description);
        DamageAttributeType.logDebug("    'target_entities' is global?: " + target_entities.isGlobal());
        DamageAttributeType.logDebug("    'target_materials' is global?: " + target_materials.isGlobal());
        
        DamageAttributeType.Data damage_data = (DamageAttributeType.Data) cached_data;
        double damage = getDamage(this.getGlyph()); // getting the damage for the attribute
        if(target_entities.isGlobal() && target_materials.isGlobal()) // If this attribute is a global attribute for both (omnidamage)
        {
          double d = damage_data.get();
          DamageAttributeType.logDebug("C- Added '" + damage + "' damage");
          damage_data.set(d + damage);
        }
        else if(target_entities.isGlobal()) // If this attribute is global attribute for entities and varies on materials
        {
          for(Material m : target_materials.getMaterials())
          {
            double d = damage_data.get(m);
            DamageAttributeType.logDebug("C- Added '" + damage + "' damage to '" + m.toString() + "'");
            damage_data.set(m, d + damage);
          }
        }
        else if(target_materials.isGlobal()) // If this attribute is a global attribute for materials and varies on entities
        {
          for(EntityType e : target_entities.getEntities())
          {
            double d = damage_data.get(e);
            DamageAttributeType.logDebug("C- Added '" + damage + "' damage to '" + e.toString() + "'");
            damage_data.set(e, d + damage);
          }
        }
        else // This attribute varies on both entities and materials
        {
          for(EntityType e : target_entities.getEntities()) for(Material m : target_materials.getMaterials())
          {
              double d = damage_data.get(e, m);
              DamageAttributeType.logDebug("C- Added '" + damage + "' damage to '" + e.toString() + "|" + m.toString() + "'");
              damage_data.set(e, m, d + damage);
          }
        }
        DamageAttributeType.logDebug("  Finished caching for " + name_description);
        data.setData(damage_data); // setting the data again.
      }

      @Override
      public String getLoreLine()
      {
        String damage_str = getDamageString(this.getGlyph());
        String item_class = target_materials.getName();
        String entity_class = target_entities.getName();
        
        String info_line = ChatColor.BLUE + "+" + damage_str + "%" + ChatColor.YELLOW + " damage to " + ChatColor.BLUE + entity_class + ChatColor.YELLOW + " using " +  ChatColor.BLUE + item_class;
        
        return "" + ChatColor.YELLOW + ChatColor.ITALIC + this.getType().getNameDescriptor() + " - " + ChatColor.RESET + info_line;
      }
    };
  }

  public static class Data implements CacheData
  {
    // Data members of the data
    private double global; // Global damage modifier
    private HashMap<Material, Double> material_damage = new HashMap<>(); // damage modifier when using a material
    private HashMap<EntityType, Double> entity_damage = new HashMap<>(); // damage modifier when used against an entity type
    private HashMap<EntityType, HashMap<Material, Double>> material_entity_damage = new HashMap<>(); // damage modifer whe used against an entity type using a material
    
    // Setters
    public void set(Double amount){this.global = amount;}
    public void set(Material mat, Double amount){this.material_damage.put(mat, amount);}
    public void set(EntityType entity, Double amount){this.entity_damage.put(entity, amount);}
    public void set(EntityType entity, Material mat, Double amount)
    {
      if(!material_entity_damage.containsKey(entity)) material_entity_damage.put(entity, new HashMap<Material,Double>());
      Map<Material,Double> e_damage = material_entity_damage.get(entity);
      e_damage.put(mat, amount);
    }
    
    // Getters
    public double get(){return this.global;}
    public double get(Material mat)
    {
      if(!this.material_damage.containsKey(mat)) return 0.0;
      return this.material_damage.get(mat);
    }
    public double get(EntityType entity)
    {
      if(!this.entity_damage.containsKey(entity)) return 0.0;
      return this.entity_damage.get(entity);
    }
    public double get(EntityType entity, Material mat)
    {
      if(!material_entity_damage.containsKey(entity)) return 0;
      Map<Material,Double> e_damage = material_entity_damage.get(entity);
      if(!e_damage.containsKey(mat)) return 0;
      return e_damage.get(mat);
    }
    
    // Clears the saved data
    @Override
    public void clear()
    {
      this.global = 0;
      this.material_damage = new HashMap<>();
      this.entity_damage = new HashMap<>();
      this.material_entity_damage = new HashMap<>();
    }

    @Override
    public String getType(){return TYPE_IDENTIFIER;}
    
    @Override
    public String getData()
    {
      // TODO
      return "";
    }
    
  } // End data definition
  
  /**Get the damage this attribute will provide given the glyph it is applied to
   * 
   * @param glyph The glyph that will be used to calculate the damage
   * @return A ratio of damage multiplication
   */
  private double getDamage(Glyph glyph)
  {
    int glyph_level = glyph.getLevel();
    int rarity_level = glyph.getRarity().getRank();
    
    double rarity_multiplier = 1 + rarity_mult * rarity_level;
    double base_damage = min_damage + (max_damage - min_damage) * (glyph_level - 1) / (Glyph.MAX_LEVEL - 1);
    return rarity_multiplier * base_damage;
  }
  /**Returns the damage as a string
   * 
   * @param glyph The glyph that will be used to calculate the damage
   * @return A string that will show the percentage increase
   */
  private String getDamageString(Glyph glyph){return String.format("%.1f", this.getDamage(glyph) * 100);}
  
  @Override
  public Attribute parse(String line)
  {
    if(ChatColor.stripColor(line.toLowerCase().trim()).startsWith(name_description.toLowerCase()))
    {
      return this.generate();
    }
    else return null;
  }
  
  @Override
  public String getName(){return this.type_name;}
  @Override
  public String getNameDescriptor(){return this.name_description;}
  
  public void setTargetEntities(EntityClass e_class){this.target_entities = e_class;}
  public void setTargetMaterials(MaterialClass m_class){this.target_materials = m_class;}
  @Override
  public Map<String, Integer> getBaseExperience()
  {return this.base_experience;}
  @Override
  public Map<String, Integer> getLevelExperience(){return this.level_experience;}
  
  
}
