package net.samongi.Inscription.Glyphs;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.md_5.bungee.api.ChatColor;
import net.samongi.Inscription.Glyphs.Attributes.Attribute;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class Glyph implements Serializable
{
  private static final long serialVersionUID = -8133713348644333985L;
  public static final int MAX_LEVEL = 100;
  
  public static Glyph getGlyph(ItemStack item) 
  {
    ItemMeta item_meta = item.getItemMeta();
    if(item_meta == null) return null;
    
    if(!item_meta.hasDisplayName()) return null;
    if(!item_meta.hasLore()) return null;
    List<String> lore = item_meta.getLore();
    if(lore.size() < 2) return null;
    
    // Parsing the type line
    String type_line = ChatColor.stripColor(lore.get(0)).toLowerCase().replace("glyph of ", "").replace("lv. ", "");
    String[] split_type_line = type_line.split(" ");
    String level_string = split_type_line[0];
    String rarity_string = split_type_line[1];
    String element_string = split_type_line[2];
    
    int level = -1;
    try{level = Integer.parseInt(level_string);}catch(NumberFormatException e){return null;}
    if(level < 0) return null;
    
    GlyphRarity rarity = GlyphRarity.valueOf(rarity_string.toUpperCase());
    if(rarity == null) return null;
    GlyphElement element = GlyphElement.valueOf(element_string.toUpperCase());
    if(element == null) return null;
    
    for(int i = 2; i < lore.size(); i++)
    {
      
    }
    
    return null;
  }
  
  private GlyphRarity rarity = null;
  private GlyphElement element = null;
  
  // Attributes of the glyph
  private ArrayList<Attribute> attributes = new ArrayList<>();
  
  // Level of the glyph
  private int level = 0;
  
  
  
  public int getLevel(){return this.level;}
  public void setLevel(int level){this.level = level;}
  public void addLevel(int levels){this.level += levels;}
  
  /**Returns the experience needed to levelup this glyph.
   * This returns a map because there can be different types of experience.
   * 
   * @return A Map of experienc types mapped to amounts needed.
   */
  public Map<String, Integer> getLevelExperience()
  {
    Map<String, Integer> ret_exp = new HashMap<>();
    for(Attribute a : this.getAttributes())
    {
      Map<String, Integer> a_exp = a.getExperience();
      for(String k : a_exp.keySet())
      {
        if(ret_exp.containsKey(k)) ret_exp.put(k, ret_exp.get(k) + a_exp.get(k));
        else ret_exp.put(k, a_exp.get(k));
      }
    }
    return ret_exp;
  }
  
  public void setRarity(GlyphRarity rarity){this.rarity = rarity;}
  public GlyphRarity getRarity(){return this.rarity;}
  
  public void setElement(GlyphElement element){this.element = element;}
  public GlyphElement getElement(){return this.element;}
  
  public void addAttribute(Attribute attribute)
  {
    attribute.setGlyph(this);
    this.attributes.add(attribute);
  }
  public List<Attribute> getAttributes(){return this.attributes;}
  
  public ItemStack getItemStack()
  {
    ItemStack item = new ItemStack(Material.PAPER);
    ItemMeta item_meta = item.getItemMeta();
    
    // Creating the item name
    String item_name = "" + rarity.getColor();
    for(Attribute a : attributes) item_name += a.getType().getNameDescriptor() + " ";
    item_name += "Glyph";
    item_meta.setDisplayName(item_name);
    
    // Creating the lore
    List<String> lore = new ArrayList<>();
    
    // Creating the info line
    String type_line = ChatColor.GRAY + "Lv. " + this.level + " " + rarity.getColor() + rarity.getDisplay() + ChatColor.GRAY + " Glyph of " + element.getColor() + element.getDisplay();
    lore.add(type_line);
    
    // Adding all the attribute lines
    for(Attribute a : this.attributes) lore.add(a.getLoreLine());
    
    item_meta.setLore(lore);
    item.setItemMeta(item_meta);
    
    return item;
  }
  
  public void printItemStack()
  {
    String item_name = "" + rarity.getColor();
    for(Attribute a : attributes) item_name += a.getType().getNameDescriptor() + " ";
    item_name += "Glyph";
    
    System.out.println(item_name);
    
    List<String> lore = new ArrayList<>();
    
    // Creating the info line
    String type_line = ChatColor.GREEN + "Lv. " + this.level + " " + rarity.getColor() + rarity.getDisplay() + ChatColor.WHITE + " Glyph of " + element.getColor() + element.getDisplay();
    lore.add(type_line);
    
    // Adding all the attribute lines
    for(Attribute a : this.attributes) lore.add(a.getLoreLine());
    
    for(String s : lore) System.out.println(s);
    
    
    
  }
}
