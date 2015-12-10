package net.samongi.Inscription.Experience;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import net.samongi.Inscription.Inscription;
import net.samongi.Inscription.Glyphs.Attributes.AttributeManager;
import net.samongi.Inscription.Player.PlayerData;
import net.samongi.SamongiLib.Configuration.ConfigFile;
import net.samongi.SamongiLib.Items.ItemUtil;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.material.MaterialData;

public class ExperienceManager
{
  public static void log(String message){Inscription.log("[ExperienceManager] " + message);}
  public static void logDebug(String message){if(Inscription.debug()) AttributeManager.log(Inscription.debug_tag + message);}
  public static boolean debug(){return Inscription.debug();}
  
  private Map<EntityType, Map<String, Integer>> exp_per_kill = new HashMap<>();
  private Map<EntityType, Map<String, Integer>> exp_per_damage = new HashMap<>();
  
  private Map<MaterialData, Map<String, Integer>> exp_per_break = new HashMap<>();
  private Map<MaterialData, Map<String, Integer>> exp_per_place = new HashMap<>();
  private Map<MaterialData, Map<String, Integer>> exp_per_craft = new HashMap<>();
  

  public void onEntityDamageEntity(EntityDamageByEntityEvent event)
  {
    Entity damaged = event.getEntity();
    Entity damager = event.getDamager();
    if(!(damager instanceof Player)) return;
    
    PlayerData data = Inscription.getInstance().getPlayerManager().getData((Player) damager);
    if(data == null)
    {
      ExperienceManager.log("ERROR: Player Data return null on call for: " + damager.getName() + ":" + damager.getUniqueId());
      return;
    }
    
    EntityType damaged_t = damaged.getType();
    double damage_dealt = event.getFinalDamage();
    
    Map<String, Integer> exp_per = this.getExpPerDamage(damaged_t);
    for(String s : exp_per.keySet())
    {
      int exp = (int) (exp_per.get(s) * damage_dealt);
      data.addExperience(s, exp);
    } 
  }
  public void onEntityDeath(EntityDeathEvent event)
  {
    LivingEntity killed = event.getEntity();
    EntityDamageEvent damage_event = killed.getLastDamageCause();
    if(!(damage_event instanceof EntityDamageByEntityEvent)) return;
    EntityDamageByEntityEvent entity_damage_event = (EntityDamageByEntityEvent) damage_event;
    
    Entity damaged = entity_damage_event.getEntity();
    Entity damager = entity_damage_event.getDamager();
    if(!(damager instanceof Player)) return;
    
    PlayerData data = Inscription.getInstance().getPlayerManager().getData((Player) damager);
    if(data == null)
    {
      ExperienceManager.log("ERROR: Player Data return null on call for: " + damager.getName() + ":" + damager.getUniqueId());
      return;
    }
    
    EntityType damaged_t = damaged.getType();
    
    Map<String, Integer> exp_per = this.getExpPerKill(damaged_t);
    if(exp_per == null) return;
    for(String s : exp_per.keySet())
    {
      int exp = exp_per.get(s);
      data.addExperience(s, exp);
    }
  }
  public void onBlockBreak(BlockBreakEvent event)
  {
    MaterialData material_data = event.getBlock().getState().getData();
    Player player = event.getPlayer();
    
    PlayerData data = Inscription.getInstance().getPlayerManager().getData(player);
    if(data == null)
    {
      ExperienceManager.log("ERROR: Player Data return null on call for: " + player.getName() + ":" + player.getUniqueId());
      return;
    }
    
    Map<String, Integer> exp_per = this.getExpPerBreak(material_data);
    if(exp_per == null) return;
    for(String s : exp_per.keySet())
    {
      int exp = exp_per.get(s);
      data.addExperience(s, exp);
    }
  }
  public void onCraftItem(CraftItemEvent event)
  {
    // TODO add crafting for experience
  }
  
  public void setExpPerKill(EntityType type, String exp_type, int amount)
  {
    if(!this.exp_per_kill.containsKey(type)) this.exp_per_kill.put(type, new HashMap<String, Integer>());
    this.exp_per_kill.get(type).put(exp_type, amount);
  }
  public Map<String, Integer> getExpPerKill(EntityType type)
  {
    if(!this.exp_per_kill.containsKey(type)) return new HashMap<>();
    return this.exp_per_kill.get(type);
  }
  
  
  public void setExpPerDamage(EntityType type, String exp_type, int amount)
  {
    if(!this.exp_per_damage.containsKey(type)) this.exp_per_damage.put(type, new HashMap<String, Integer>());
    this.exp_per_damage.get(type).put(exp_type, amount);
  }
  public Map<String, Integer> getExpPerDamage(EntityType type)
  {
    if(!this.exp_per_damage.containsKey(type)) return new HashMap<>();
    return this.exp_per_damage.get(type);
  }
  
  
  public void setExpPerBreak(MaterialData mat, String exp_type, int amount)
  {
    if(!this.exp_per_break.containsKey(mat)) this.exp_per_break.put(mat, new HashMap<String, Integer>());
    this.exp_per_break.get(mat).put(exp_type, amount);
  }
  public Map<String, Integer> getExpPerBreak(MaterialData mat)
  {
    if(!this.exp_per_break.containsKey(mat)) return new HashMap<>();
    return this.exp_per_break.get(mat);
  }
  
  
  public void setExpPerPlace(MaterialData mat, String exp_type, int amount)
  {
    if(!this.exp_per_place.containsKey(mat)) this.exp_per_place.put(mat, new HashMap<String, Integer>());
    this.exp_per_place.get(mat).put(exp_type, amount);
  }
  public Map<String, Integer> getExpPerPlace(MaterialData mat)
  {
    if(!this.exp_per_place.containsKey(mat)) return new HashMap<>();
    return this.exp_per_place.get(mat);
  }
  
  
  public void setExpPerCraft(MaterialData mat, String exp_type, int amount)
  {
    if(!this.exp_per_craft.containsKey(mat)) this.exp_per_craft.put(mat, new HashMap<String, Integer>());
    this.exp_per_craft.get(mat).put(exp_type, amount);
  }
  public Map<String, Integer> getExpPerCraft(MaterialData mat)
  {
    if(!this.exp_per_craft.containsKey(mat)) return new HashMap<>();
    return this.exp_per_craft.get(mat);
  }
  
  /**Parses a file with experience mappings.
   * 
   * @param config A filling with experience mappings
   */
  public void parse(ConfigFile config)
  {
    FileConfiguration root = config.getConfig();
    
    // All the configuration sections with experience data.
    ConfigurationSection entity_damage = root.getConfigurationSection("entity-damage");
    ConfigurationSection entity_kill = root.getConfigurationSection("entity-kill");
    ConfigurationSection material_break = root.getConfigurationSection("material-break");
    ConfigurationSection material_craft = root.getConfigurationSection("material-craft");
    ConfigurationSection material_place = root.getConfigurationSection("material-place");
    
    // Entity Damage configuration reading and setting
    if(entity_damage != null)
    {
      ExperienceManager.logDebug("Parsing Entity Damage Experience Rewards");
      Set<String> entity_damage_keys = entity_damage.getKeys(false);
      for(String k : entity_damage_keys)
      {
        ExperienceManager.logDebug("  Parsing: " + k);
        EntityType t = EntityType.valueOf(k);
        if(t == null) continue;
        
        ConfigurationSection key_section = entity_damage.getConfigurationSection(k);
        if(key_section == null) continue;
        Set<String> key_section_keys = key_section.getKeys(false);
        for(String exp : key_section_keys)
        {
          this.setExpPerDamage(t, exp, key_section.getInt(exp));
        }
      }
    }
    
    // Entity Kills configuration reading and setting
    if(entity_kill != null)
    {
      ExperienceManager.logDebug("Parsing Entity Kill Experience Rewards");
      Set<String> entity_kill_keys = entity_kill.getKeys(false);
      for(String k : entity_kill_keys)
      {
        ExperienceManager.logDebug("  Parsing: " + k);
        EntityType t = EntityType.valueOf(k);
        if(t == null) continue;
        
        ConfigurationSection key_section = entity_kill.getConfigurationSection(k);
        if(key_section == null) continue;
        Set<String> key_section_keys = key_section.getKeys(false);
        for(String exp : key_section_keys)
        {
          this.setExpPerKill(t, exp, key_section.getInt(exp));
        }
      }
    }
    // Material Breaking configuration reading and setting
    if(material_break != null)
    {
      ExperienceManager.logDebug("Parsing Material Break Experience Rewards");
      Set<String> material_break_keys = material_break.getKeys(false);
      for(String k : material_break_keys)
      {
        ExperienceManager.logDebug("  Parsing: " + k);
        MaterialData t = ItemUtil.getMaterialData(k);
        if(t == null) continue;
        
        ConfigurationSection key_section = material_break.getConfigurationSection(k);
        if(key_section == null) continue;
        Set<String> key_section_keys = key_section.getKeys(false);
        for(String exp : key_section_keys)
        {
          this.setExpPerBreak(t, exp, key_section.getInt(exp));
        }
      }
    }
    // Material Crafting configuration reading and setting
    if(material_craft != null)
    {
      ExperienceManager.logDebug("Parsing Material Place Experience Rewards");
      Set<String> material_craft_keys = material_craft.getKeys(false);
      for(String k : material_craft_keys)
      {
        ExperienceManager.logDebug("  Parsing: " + k);
        MaterialData t = ItemUtil.getMaterialData(k);
        if(t == null) continue;
        
        ConfigurationSection key_section = material_craft.getConfigurationSection(k);
        if(key_section == null) continue;
        Set<String> key_section_keys = key_section.getKeys(false);
        for(String exp : key_section_keys)
        {
          this.setExpPerCraft(t, exp, key_section.getInt(exp));
        }
      }
    }
    // Material Placing configuration reading and setting
    if(material_place != null)
    {
      ExperienceManager.logDebug("Parsing Material Craft Experience Rewards");
      Set<String> material_place_keys = material_place.getKeys(false);
      for(String k : material_place_keys)
      {
        ExperienceManager.logDebug("  Parsing: " + k);
        MaterialData t = ItemUtil.getMaterialData(k);
        if(t == null) continue;
        
        ConfigurationSection key_section = material_place.getConfigurationSection(k);
        if(key_section == null) continue;
        Set<String> key_section_keys = key_section.getKeys(false);
        for(String exp : key_section_keys)
        {
          this.setExpPerPlace(t, exp, key_section.getInt(exp));
        }
      }
    }
  }
}
