package net.samongi.Inscription.Glyphs.Attributes;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import net.samongi.Inscription.Glyphs.Glyph;
import net.samongi.Inscription.Player.PlayerData;

public abstract class Attribute implements Serializable
{
  // Generated Serialization UID
  private static final long serialVersionUID = -5994047304332535496L;
  
  private Glyph glyph_container;
  private AttributeType type;
  
  private Map<String, Integer> base_experience = new HashMap<>();
  private Map<String, Integer> per_level_experience = new HashMap<>();
  
  public Attribute(AttributeType type)
  {
    this.type = type;
  }
  
  /**Sets the glyph that this attribute is a part of.
   * This will determine it's hashcode.
   * 
   * @param glyph
   */
  public void setGlyph(Glyph glyph){this.glyph_container = glyph;}
  /**Retrieves the glpyh this attribute is currently contained within.
   * By contract, a glyph should only be contained within one glyph.
   * This is due to the attribute referencing the glyph.
   * 
   * @return The glyph this is contained within.
   */
  public Glyph getGlyph(){return this.glyph_container;}
  
  /**Caches this glyph's effects with player data for quicker calculations
   * for events that are called rapidly and readily such as block breaks.
   * Implementation for this cahcing is required.
   * 
   * @param data
   */
  public abstract void cache(PlayerData data);
  
  
  /**Gets this attribute's type.
   * 
   * @return
   */
  public AttributeType getType(){return this.type;}
  
  /**Get the line of lore that can be parsed by the glyph's Parser object.
   * By contract when being parsed this should return a glyph that is identical when using
   * .equals given that .equals has been implemented.
   * 
   * @return
   */
  public abstract String getLoreLine();
  
  /**Returns the required experience that this attribute requires for the glyph to level up
   * This takes into account the level of the glyph it is currently set to.
   * 
   * @return A Mapping of experience type to the amount of experience.
   */
  public Map<String, Integer> getExperience()
  {
    Map<String, Integer> experience_map = new HashMap<>(base_experience);
    int glyph_level = this.getGlyph().getLevel();
    
    for(String s : per_level_experience.keySet()) 
    {
      if(!experience_map.containsKey(s)) experience_map.put(s, per_level_experience.get(s) * glyph_level);
      else experience_map.put(s, experience_map.get(s) + per_level_experience.get(s) * glyph_level);
    }
    return experience_map;
  }
  
  public void setBaseExperience(Map<String, Integer> experience_map){this.base_experience = experience_map;}
  public void setPerLevelExperience(Map<String, Integer> experience_map){this.per_level_experience = experience_map;}


  
}
