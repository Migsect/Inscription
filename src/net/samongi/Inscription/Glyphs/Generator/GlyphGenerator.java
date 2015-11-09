package net.samongi.Inscription.Glyphs.Generator;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.bukkit.configuration.ConfigurationSection;

import net.samongi.Inscription.Inscription;
import net.samongi.Inscription.Glyphs.Glyph;
import net.samongi.Inscription.Glyphs.GlyphElement;
import net.samongi.Inscription.Glyphs.GlyphRarity;
import net.samongi.Inscription.Glyphs.Attributes.Attribute;
import net.samongi.Inscription.Glyphs.Attributes.AttributeManager;
import net.samongi.Inscription.Glyphs.Attributes.AttributeType;
import net.samongi.SamongiLib.Configuration.ConfigFile;

public class GlyphGenerator
{
  public static void log(String message){Inscription.log("[GlyphGenerator] " + message);}
  public static void logDebug(String message){if(Inscription.debug()) GlyphGenerator.log(Inscription.debug_tag + message);}
  public static boolean debug(){return Inscription.debug();}
  
  private String type_name = "DEFAULT";
  
  private WeightMap<AttributeType> attribute_weights = new WeightMap<>();
  private WeightMap<Integer> attribute_count_weights = new WeightMap<>();
  private WeightMap<GlyphRarity> rarity_weights = new WeightMap<>();
  private WeightMap<GlyphElement> element_weights = new WeightMap<>();
  
  private int min_level = 1;
  private int max_level = 100;
  
  /**Constructs a GlyphGenerator with the following type name.
   * 
   * @param type_name This will be upcased and underscored spaced.
   */
  public GlyphGenerator(String type_name)
  {
    this.type_name = type_name.toUpperCase().replace(" ", "_");
  }
  
  
  public String getTypeName(){return this.type_name;}
  
  public Glyph getGlyph(){return this.getGlyph(min_level, max_level);}
  public Glyph getGlyph(int min_level, int max_level)
  {
    Random rand = new Random();
    
    // Creating the new parser
    Glyph glyph = new Glyph();
    glyph.setElement(this.element_weights.getRandom());
    glyph.setRarity(this.rarity_weights.getRandom());
    glyph.setLevel(min_level + rand.nextInt(max_level - min_level + 1));
    
    int attribute_count = this.attribute_count_weights.getRandom();
    Set<String> current_attributes = new HashSet<>();
    for(int i = 0 ; i < attribute_count ; i++)
    {
      AttributeType attribute_generator = this.attribute_weights.getRandom();
      Attribute attribute = attribute_generator.generate();
      // Making sure there aren't duplicates of attributes.
      if(current_attributes.contains(attribute_generator.getName()))
      {
        i--;
        if(i < 0) break;
        else continue;
      }
      // Adding the attribute to the list of attributes that have already been selected
      current_attributes.add(attribute_generator.getName());
      // Adding the attribute to the glyph.
      glyph.addAttribute(attribute);
    }
    
    return glyph;
  }
  
  public void setMinLevel(int level){this.min_level = level;}
  public void setMaxLevel(int level){this.max_level = level;}
  public void addAttributeCount(int count, int weight)
  {
    if(weight <= 0)
    {
      GlyphGenerator.logDebug("AttrbiuteCount '" + count + "' attempted to be registered with weight " + weight);
      return;
    }
    this.attribute_count_weights.put(count, weight);
  }
  public void addAttributeType(AttributeType type, int weight)
  {
    if(weight <= 0)
    {
      GlyphGenerator.logDebug("AttributeType '" + type.getName() + "' attempted to be registered with weight " + weight);
      return;
    }
    this.attribute_weights.put(type, weight);
  }
  public void addRarity(GlyphRarity rarity, int weight)
  {
    if(weight <= 0)
    {
      GlyphGenerator.logDebug("Rarity '" + rarity + "' attempted to be registered with weight " + weight);
      return;
    }
    this.rarity_weights.put(rarity, weight);
  }
  public void addElement(GlyphElement element, int weight)
  {
    if(weight <= 0)
    {
      GlyphGenerator.logDebug("GlyphElement '" + element + "' attempted to be registered with weight " + weight);
      return;
    }
    this.element_weights.put(element, weight);
  }
  
  public static List<GlyphGenerator> parse(File file)
  {
    ConfigFile config = new ConfigFile(file);
    
    List<GlyphGenerator> generators = new ArrayList<GlyphGenerator>();
    ConfigurationSection root = config.getConfig().getConfigurationSection("generators");
    for(String s : root.getKeys(false))
    {
      GlyphGenerator generator = GlyphGenerator.parse(root.getConfigurationSection(s));
      if(generator == null) continue;
      generators.add(generator);
    }
    
    return generators;
  }
  public static GlyphGenerator parse(ConfigurationSection section)
  {
    // Getting the generator's name
    String type_name = section.getString("type-name");
    if(type_name == null)
    {
      return null;
    }
    GlyphGenerator.logDebug("Found type_name to be: '" + type_name + "'");
    
    // Creating the generator
    GlyphGenerator generator = new GlyphGenerator(type_name);
    
    // min and max level setting of the generator
    int min_level = section.getInt("min-level", 1);
    int max_level = section.getInt("max-level", 100);
    generator.setMaxLevel(max_level);
    generator.setMinLevel(min_level);
    GlyphGenerator.logDebug("Found min_level to be: '" + min_level + "'");
    GlyphGenerator.logDebug("Found max_level to be: '" + max_level + "'");
    

    GlyphGenerator.logDebug("Parsing Elements:");
    ConfigurationSection elements = section.getConfigurationSection("elements");
    Set<String> element_keys = elements.getKeys(false);
    for(String k : element_keys)
    {
      GlyphElement e = GlyphElement.valueOf(k);
      if(e == null) continue; // TODO error message
      
      int weight = elements.getInt(k);
      generator.addElement(e, weight);

      GlyphGenerator.logDebug("  Found e to be: '" + e  + "'");
      GlyphGenerator.logDebug("  Found weight to be: '" + weight  + "'");
    }

    GlyphGenerator.logDebug("Parsing Rarities:");
    ConfigurationSection rarities = section.getConfigurationSection("rarities");
    Set<String> rarity_keys = rarities.getKeys(false);
    for(String k : rarity_keys)
    {
      GlyphRarity r = GlyphRarity.valueOf(k);
      if(r == null) continue; // TODO error message

      int weight = rarities.getInt(k);
      generator.addRarity(r, weight);
      
      GlyphGenerator.logDebug("  Found r to be: '" + r  + "'");
      GlyphGenerator.logDebug("  Found weight to be: '" + weight  + "'");
    }

    GlyphGenerator.logDebug("Parsing Attribute Counts:");
    ConfigurationSection attribute_counts = section.getConfigurationSection("attribute-counts");
    Set<String> attribute_count_keys = attribute_counts.getKeys(false);
    for(String k : attribute_count_keys)
    {
      int int_k = -1;
      try{int_k = Integer.parseInt(k);}catch(NumberFormatException e){}
      if(int_k <= 0) continue; // TODO error message
      
      int weight = attribute_counts.getInt(k);
      generator.addAttributeCount(int_k, weight);
      
      GlyphGenerator.logDebug("  Found int_k to be: '" + int_k  + "'");
      GlyphGenerator.logDebug("  Found weight to be: '" + weight  + "'");
    }
    
    AttributeManager attribute_manager = Inscription.getInstance().getAttributeManager();

    GlyphGenerator.logDebug("Parsing Attributes:");
    ConfigurationSection attributes = section.getConfigurationSection("attributes");
    Set<String> attribute_keys = attributes.getKeys(false);
    for(String k : attribute_keys)
    {
      AttributeType type = attribute_manager.getAttributeType(k);
      if(type == null)
      {
        GlyphGenerator.logDebug("Could not find type for section: '" + k + "'");
        continue;
      }
      
      int weight = attributes.getInt(k);
      generator.addAttributeType(type, weight);
      
      GlyphGenerator.logDebug("  Found type to be: '" + type.getName()  + "'");
      GlyphGenerator.logDebug("  Found weight to be: '" + weight  + "'");
    }
    
    
    return generator;
  }
}
