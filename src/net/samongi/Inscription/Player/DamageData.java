package net.samongi.Inscription.Player;

import java.util.HashMap;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;

public class DamageData
{ 
  private double global_damage_modifier = 0;
  private HashMap<Material, Double> material_damage_modifiers = new HashMap<>();
  private HashMap<EntityType, Double> entity_damage_modifiers = new HashMap<>();
  private HashMap<EntityType, HashMap<Material, Double>> damage_modifiers = new HashMap<>();
  
  /**Resets all the data stored for damage
   * 
   */
  public void reset()
  {
    this.global_damage_modifier = 0;
    this.damage_modifiers.clear();
    this.material_damage_modifiers.clear();
    this.entity_damage_modifiers.clear();
  }
  
  public void add(EntityType type, Material material, double modifier)
  {
    if(!damage_modifiers.containsKey(type)) damage_modifiers.put(type, new HashMap<Material, Double>());
    HashMap<Material, Double> material_modifiers = damage_modifiers.get(type);
    if(!material_modifiers.containsKey(material)) material_modifiers.put(material, modifier);
    else material_modifiers.put(material, material_modifiers.get(material) + modifier);
  }
  public void add(EntityType type, double modifier)
  {
    if(!this.entity_damage_modifiers.containsKey(type)) this.entity_damage_modifiers.put(type, modifier);
    else this.entity_damage_modifiers.put(type, this.entity_damage_modifiers.get(type) + modifier);
  }
  public void add(Material material, double modifier)
  {
    if(!this.material_damage_modifiers.containsKey(material)) this.material_damage_modifiers.put(material, modifier);
    else this.material_damage_modifiers.put(material, this.material_damage_modifiers.get(material) + modifier);
  }
  public void add(double modifier)
  {
    this.global_damage_modifier += modifier;
  }
  
  public double getModifierSum(EntityType type, Material material)
  {
    double global = this.getModifier();
    double em_type = this.getModifier(type, material);
    double e_type = this.getModifier(type);
    double m_type = this.getModifier(material);
    
    return global + em_type + e_type + m_type;
  }
  public double getModifier(EntityType type, Material material)
  {
    if(!this.damage_modifiers.containsKey(type)) return 0;
    HashMap<Material, Double> material_modifiers = damage_modifiers.get(type);
    if(!material_modifiers.containsKey(material)) return 0;
    return material_modifiers.get(material);
  }
  public double getModifier(EntityType type)
  {
    if(!this.entity_damage_modifiers.containsKey(type)) return 0;
    return this.entity_damage_modifiers.get(type);
  }
  public double getModifier(Material material)
  {
    if(!this.material_damage_modifiers.containsKey(material)) return 0;
    return this.material_damage_modifiers.get(material);
  }
  public double getModifier(){return this.global_damage_modifier;}
  
}
