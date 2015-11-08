package net.samongi.Inscription.Glyphs.Generator;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.bukkit.configuration.ConfigurationSection;

import net.samongi.Inscription.Glyphs.Glyph;
import net.samongi.Inscription.Glyphs.GlyphElement;
import net.samongi.Inscription.Glyphs.GlyphRarity;
import net.samongi.Inscription.Glyphs.Attributes.Attribute;
import net.samongi.Inscription.Glyphs.Attributes.AttributeHandler;
import net.samongi.Inscription.Glyphs.Attributes.AttributeType;
import net.samongi.SamongiLib.Configuration.ConfigFile;

public class GlyphGenerator
{
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
      // Making sure the handler has an instance of the parser.
      AttributeHandler.getInstance().registerParser(attribute_generator);
    }
    
    return glyph;
  }
  
  public void setMinLevel(int level){this.min_level = level;}
  public void setMaxLevel(int level){this.max_level = level;}
  public void addAttributeCount(int count, int weight){this.attribute_count_weights.put(count, weight);}
  public void addAttributeGenerator(AttributeType generator, int weight){this.attribute_weights.put(generator, weight);}
  public void addRarity(GlyphRarity rarity, int weight){this.rarity_weights.put(rarity, weight);}
  public void addElement(GlyphElement element, int weight){this.element_weights.put(element, weight);}
  
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
    
  }
}
