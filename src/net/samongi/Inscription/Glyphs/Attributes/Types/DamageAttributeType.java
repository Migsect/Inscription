package net.samongi.Inscription.Glyphs.Attributes.Types;

import org.bukkit.ChatColor;

import net.samongi.Inscription.Glyphs.Glyph;
import net.samongi.Inscription.Glyphs.Attributes.Attribute;
import net.samongi.Inscription.Glyphs.Attributes.AttributeType;
import net.samongi.Inscription.Player.PlayerData;
import net.samongi.Inscription.TypeClasses.EntityClass;
import net.samongi.Inscription.TypeClasses.MaterialClass;

public class DamageAttributeType implements AttributeType
{
  private static final long serialVersionUID = -8367848352344774887L; 
  
  private final String type_name;
  private final String name_description;
  private final EntityClass target_entities;
  private final MaterialClass target_materials;
  
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
  
}
