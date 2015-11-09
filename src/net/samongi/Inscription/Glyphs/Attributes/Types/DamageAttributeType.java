package net.samongi.Inscription.Glyphs.Attributes.Types;

import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;

import net.samongi.Inscription.Inscription;
import net.samongi.Inscription.Glyphs.Glyph;
import net.samongi.Inscription.Glyphs.Attributes.Attribute;
import net.samongi.Inscription.Glyphs.Attributes.AttributeType;
import net.samongi.Inscription.Glyphs.Attributes.AttributeTypeConstructor;
import net.samongi.Inscription.Player.PlayerData;
import net.samongi.Inscription.TypeClasses.EntityClass;
import net.samongi.Inscription.TypeClasses.MaterialClass;

public class DamageAttributeType implements AttributeType
{
  public static void log(String message){Inscription.log("[DamageAttributeType] " + message);}
  public static void logDebug(String message){if(Inscription.debug()) DamageAttributeType.log(Inscription.debug_tag + message);}
  public static boolean debug(){return Inscription.debug();}
  
  private static final long serialVersionUID = -8367848352344774887L; 
  
  private final String type_name;
  private final String name_description;
  private EntityClass target_entities;
  private MaterialClass target_materials;
  
  private final double min_damage;
  private final double max_damage;
  private final double rarity_mult;
  
  /**Consturctor for a DamageAttributeType
   * 
   * @param type_name The name of the type (display)
   * @param description An item name descriptor
   * @param min_damage The min damage this damage attribute can have
   * @param max_damage The max damage this damage attribute can have
   * @param rarity_mult The multiplier based on rarity level of the item.
   */
  public DamageAttributeType(String type_name, String description, double min_damage, double max_damage, double rarity_mult)
  {
    this.type_name = type_name;
    this.name_description = description;
    
    this.target_entities = EntityClass.getGlobalLiving("all creatures");
    this.target_materials = MaterialClass.getGlobal("any items");
    
    this.min_damage = min_damage;
    this.max_damage = max_damage;
    this.rarity_mult = rarity_mult;
  }
  
  public static class Constructor implements AttributeTypeConstructor
  {
    @Override
    public AttributeType construct(ConfigurationSection section)
    {
      String type = section.getString("type");
      if(type == null)
      {
        DamageAttributeType.log(section.getName() + " : Could not find attribute 'type' in section.");
        return null;
      }
      if(!type.toUpperCase().equals("DAMAGE")) return null;
      
      String name = section.getString("name");
      if(name == null)
      {
        DamageAttributeType.log(section.getName() + " : Could not find attribute 'name' in section.");
        return null;
      }
      
      String descriptor = section.getString("descriptor");
      if(descriptor == null)
      {
        DamageAttributeType.log(section.getName() + " : Could not find attribute 'descriptor' in section.");
        return null;
      }
      
      int min_damage = section.getInt("min-damage");
      int max_damage = section.getInt("max-damage");
      if(min_damage > max_damage)
      {
        DamageAttributeType.log(section.getName() + " : min damage is bigger than max damage");
        return null;
      }
      double rarity_mult = section.getDouble("rarity-multiplier");
      
      String target_entities = section.getString("target-entities");
      String target_materials = section.getString("target-materials");
      
      DamageAttributeType attribute_type = new DamageAttributeType(name, descriptor, min_damage, max_damage, rarity_mult);
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
  }

  @Override
  public Attribute generate()
  {
    return new Attribute(this){
      private static final long serialVersionUID = 7422514378631333199L;

      @Override
      public void cache(PlayerData data)
      {
        // TODO Auto-generated method stub
        
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
  
  private double getDamage(Glyph glyph)
  {
    int glyph_level = glyph.getLevel();
    int rarity_level = glyph.getRarity().getRank();
    
    double rarity_multiplier = 1 + rarity_mult * rarity_level;
    double base_damage = min_damage + (max_damage - min_damage) * (glyph_level - 1) / (Glyph.MAX_LEVEL - 1);
    return rarity_multiplier * base_damage;
  }
  private String getDamageString(Glyph glyph){return String.format("%.1f", this.getDamage(glyph));}
  
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
  
}
