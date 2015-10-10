package net.samongi.Inscription.Glyphs;

import java.util.List;

import net.samongi.Inscription.Glyphs.Attributes.GlyphAttribute;

import org.bukkit.inventory.ItemStack;

public class Glyph
{
  private GlyphRarity rarity;
  private GlyphElement element;
  
  private List<GlyphAttribute> attribute;
  
  private int level;
  private int experience;
  
  public int getLevel(){return this.level;}
  public void setLevel(int level){this.level = level;}
  public void addLevel(int levels){this.level += levels;}
  
  public int getExperience(){return this.experience;}
  public void setExperience(int experience){this.experience = experience;}
  public void addExperience(int experience){this.level += experience;}
  
  public GlyphRarity getRarity(){return this.rarity;}
  public GlyphElement getElement(){return this.element;}
  
  /**Will get an itemstack representation of the glyph.
   * 
   * @return
   */
  public ItemStack getItemStack()
  {
    return null;
  }
  
  /**Will return a glyph representation of the itemstack.
   * Will return null if the itemstack does not represent a glyph
   * 
   * @param item
   * @return
   */
  static public Glyph getGlyph(ItemStack item)
  {
    return null;
  }
}
