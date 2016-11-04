package net.samongi.Inscription.Glyphs.Attributes.Base;

import java.util.Map;

import net.md_5.bungee.api.ChatColor;
import net.samongi.Inscription.Glyphs.Glyph;
import net.samongi.Inscription.Glyphs.Attributes.Attribute;
import net.samongi.Inscription.Glyphs.Attributes.AttributeType;

/**
 * This is an attribute type which relies on possibility.
 * It generally has a maximum chance and a minimum chance to do something on
 * some event.
 * 
 * @author Migsect
 *
 */
public abstract class ChanceAttributeType implements AttributeType
{

  private static final long serialVersionUID = -3776352837090077238L;

  /* *** class members *** */
  protected final String type_name;
  protected final String name_description;

  protected Map<String, Integer> base_experience;
  protected Map<String, Integer> level_experience;

  protected double min_chance;
  protected double max_chance;
  protected double rarity_mult;

  protected ChanceAttributeType(String type_name, String description)
  {
    this.type_name = type_name;
    this.name_description = description;
  }

  public void setMin(double chance)
  {
    this.min_chance = chance;
  }

  public void setMax(double chance)
  {
    this.max_chance = chance;
  }

  public void setMultiplier(double multiplier)
  {
    this.rarity_mult = multiplier;
  }

  public double getChance(Glyph glyph)
  {
    int glyph_level = glyph.getLevel();
    int rarity_level = glyph.getRarity().getRank();

    double rarity_multiplier = 1 + rarity_mult * rarity_level;
    double base_chance = this.min_chance + (this.max_chance - this.min_chance) * (glyph_level - 1)
        / (Glyph.MAX_LEVEL - 1);
    return rarity_multiplier * base_chance;
  }

  public String getChanceString(Glyph glyph)
  {
    return String.format("%.1f", this.getChance(glyph) * 100);
  }

  @Override
  public Attribute parse(String line)
  {
    if (ChatColor.stripColor(line.toLowerCase().trim()).startsWith(name_description.toLowerCase()))
    {
      return this.generate();
    }
    else return null;
  }

  @Override
  public String getName()
  {
    return this.type_name;
  }

  @Override
  public String getNameDescriptor()
  {
    return this.name_description;
  }

  @Override
  public Map<String, Integer> getBaseExperience()
  {
    return this.base_experience;
  }

  @Override
  public Map<String, Integer> getLevelExperience()
  {
    return this.level_experience;
  }

  @Override
  public double getRarityMultiplier()
  {
    return this.rarity_mult;
  }

}
