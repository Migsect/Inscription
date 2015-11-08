package net.samongi.Inscription.Loot;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import net.samongi.Inscription.Glyphs.Glyph;
import net.samongi.Inscription.Glyphs.Generator.GlyphGenerator;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

public class LootManager
{
  // Chances for entities to drop the glyphs
  private Map<EntityType, Double> entity_drop_chances = new HashMap<>();
  private Map<EntityType, GlyphGenerator> entity_generators = new HashMap<>();
  // Chances for blocks to drop the glyphs and the generators
  private Map<Material, Double> block_drop_chances = new HashMap<>();
  private Map<Material, GlyphGenerator> block_generators = new HashMap<>();
  
  // Mapping of all the GlyphGenerators (type name to generator)
  private Map<String, GlyphGenerator> glyph_generators = new HashMap<>();
  
  public void registerGenerator(GlyphGenerator generator){this.glyph_generators.put(generator.getTypeName(), generator);}
  public GlyphGenerator getGenerator(String type_name){return this.glyph_generators.get(type_name);}
  public void registerGeneratorToEntity(EntityType type, GlyphGenerator generator, double chance)
  {
    this.entity_drop_chances.put(type, chance);
    this.entity_generators.put(type, generator);
  }
  public void registerGeneratorToMaterial(Material type, GlyphGenerator generator, double chance)
  {
    this.block_drop_chances.put(type, chance);
    this.block_generators.put(type, generator);
  }
  
  /**Parses all configuration files that represent GlyphGenerators
   * and will add them all to the LootManager
   * 
   * @param dir The directory of the config files that are generators
   */
  public void parseGenerators(File dir)
  {
    File[] files = dir.listFiles();
    for(File f : files)
    {
      if(!f.exists()) continue;
      if(f.isDirectory()) continue;
      List<GlyphGenerator> generators = GlyphGenerator.parse(f);
      for(GlyphGenerator g : generators) this.registerGenerator(g);
    }
  }
  
  public void onEntityDeath(EntityDeathEvent event)
  { 
    EntityType type = event.getEntityType();
    if(!entity_drop_chances.containsKey(type)) return;
    if(!entity_generators.containsKey(type)) return;
    double type_chance = entity_drop_chances.get(type);
    
    // Checking to see if the mob will drop the glyph
    Random rand = new Random();
    if(rand.nextDouble() > type_chance) return;
    
    GlyphGenerator generator = entity_generators.get(type);
    Location loc = event.getEntity().getLocation();
    
    this.dropGlyph(generator, loc);
  }
  
  public void onBlockBreak(BlockBreakEvent event)
  {
    Material type = event.getBlock().getType();
    if(!block_drop_chances.containsKey(type)) return;
    if(!block_generators.containsKey(type)) return;
    double type_chance = block_drop_chances.get(type);
    
    // Checking to see if the mob will drop the glyph
    Random rand = new Random();
    if(rand.nextDouble() > type_chance) return;
    
    GlyphGenerator generator = block_generators.get(type);
    Location loc = event.getBlock().getLocation();
    
    this.dropGlyph(generator, loc);
  }
  
  private void dropGlyph(GlyphGenerator generator, Location loc)
  {
    Glyph glyph = generator.getGlyph();
    ItemStack item = glyph.getItemStack();
    loc.getWorld().dropItem(loc, item);
  }
}
