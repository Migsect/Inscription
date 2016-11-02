package net.samongi.Inscription.Loot;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import net.samongi.Inscription.Inscription;
import net.samongi.Inscription.Glyphs.Glyph;
import net.samongi.Inscription.Glyphs.Generator.GlyphGenerator;
import net.samongi.SamongiLib.Configuration.ConfigFile;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
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

  public void registerGenerator(GlyphGenerator generator)
  {
    this.glyph_generators.put(generator.getTypeName(), generator);
  }
  /**
   * Retrieves the generator with the type name.
   * 
   * @param type_name
   *          The name of the generator to retrieve
   * @return The GlyphGenerator
   */
  public GlyphGenerator getGenerator(String type_name)
  {
    return this.glyph_generators.get(type_name);
  }

  /**
   * Retrieves a list of all the generators.
   * 
   * @return All the generators.
   */
  public List<GlyphGenerator> getGenerators()
  {
    return new ArrayList<GlyphGenerator>(this.glyph_generators.values());
  }

  /**
   * Registers the generator to the entity type.
   * 
   * @param type
   *          The type of the entity to register the generator to.
   * @param generator
   *          The generator to register.
   * @param chance
   *          The probability that the generator will be used.
   */
  public void registerGeneratorToEntity(EntityType type,
      GlyphGenerator generator, double chance)
  {
    this.entity_drop_chances.put(type, chance);
    this.entity_generators.put(type, generator);
  }

  /**
   * Registers the generator to the material type.
   * 
   * @param type
   *          The type of the mateiral (block) to register the generator to.
   * @param generator
   *          The generator to register.
   * @param chance
   *          The probability that the generator will be used.
   */
  public void registerGeneratorToMaterial(Material type,
      GlyphGenerator generator, double chance)
  {
    this.block_drop_chances.put(type, chance);
    this.block_generators.put(type, generator);
  }

  /**
   * Parses all configuration files that represent GlyphGenerators and will add
   * them all to the LootManager
   * 
   * @param dir
   *          The directory of the config files that are generators
   */
  public void parseGenerators(File dir)
  {
    if (!dir.exists()) return; // TODO error message
    if (!dir.isDirectory()) return; // TODO error message

    File[] files = dir.listFiles();
    for (File f : files)
    {
      if (!f.exists()) continue;
      if (f.isDirectory()) continue;
      List<GlyphGenerator> generators = GlyphGenerator.parse(f);
      for (GlyphGenerator g : generators)
        this.registerGenerator(g);
    }
  }

  public void parseDrops(File file)
  {
    if (!file.exists()) return; // TODO error message;
    if (file.isDirectory()) return; // TODO error message;

    ConfigFile config = new ConfigFile(file);
    ConfigurationSection root = config.getConfig().getConfigurationSection(
        "drops");
    if (root == null) return; // TODO error message

    ConfigurationSection entities = root.getConfigurationSection("entities");
    if (entities != null) // TODO error message?
    {
      Set<String> entity_keys = entities.getKeys(false);
      for (String k : entity_keys)
      {
        EntityType type = EntityType.valueOf(k);
        if (type == null) continue; // TODO error message
        ConfigurationSection section = entities.getConfigurationSection(k);

        String gen_string = section.getString("generator");
        if (gen_string == null) continue; // TODO error_message
        GlyphGenerator generator = this.getGenerator(gen_string);
        if (generator == null) continue; // TODO error_message;

        double rate = section.getDouble("rate");

        this.registerGeneratorToEntity(type, generator, rate);
      }
    }

    ConfigurationSection materials = root.getConfigurationSection("materials");
    if (materials != null) // TODO error message?
    {
      Set<String> material_keys = materials.getKeys(false);
      for (String k : material_keys)
      {
        Material type = Material.valueOf(k);
        if (type == null) continue; // TODO error message
        ConfigurationSection section = materials.getConfigurationSection(k);

        String gen_string = section.getString("generator");
        if (gen_string == null) continue; // TODO error_message
        GlyphGenerator generator = this.getGenerator(gen_string);
        if (generator == null) continue; // TODO error_message;

        double rate = section.getDouble("rate");

        this.registerGeneratorToMaterial(type, generator, rate);
      }
    }
  }

  public void onEntityDeath(EntityDeathEvent event)
  {
    EntityType type = event.getEntityType();
    if (!entity_drop_chances.containsKey(type)) return;
    if (!entity_generators.containsKey(type)) return;
    double type_chance = entity_drop_chances.get(type);

    // Checking to see if the mob will drop the glyph
    Random rand = new Random();
    if (rand.nextDouble() > type_chance) return;

    GlyphGenerator generator = entity_generators.get(type);
    Location loc = event.getEntity().getLocation();

    this.dropGlyph(generator, loc);
  }

  public void onBlockBreak(BlockBreakEvent event)
  {
    if (event.isCancelled()) return;
    Location location = event.getPlayer().getLocation();
    Material type = event.getBlock().getType();
    if (Inscription.getInstance().getExperienceManager().getTracker()
        .isTracked(type)
        && Inscription.getInstance().getExperienceManager().getTracker()
            .isPlaced(location)) return;

    if (!block_drop_chances.containsKey(type)) return;
    if (!block_generators.containsKey(type)) return;
    double type_chance = block_drop_chances.get(type);

    // Checking to see if the mob will drop the glyph
    Random rand = new Random();
    if (rand.nextDouble() > type_chance) return;

    GlyphGenerator generator = block_generators.get(type);

    this.dropGlyph(generator, location);
    Inscription.logDebug("A Glyph was dropped!");
  }

  private void dropGlyph(GlyphGenerator generator, Location loc)
  {
    Glyph glyph = generator.getGlyph();
    ItemStack item = glyph.getItemStack();
    loc.getWorld().dropItem(loc, item);
  }
}
