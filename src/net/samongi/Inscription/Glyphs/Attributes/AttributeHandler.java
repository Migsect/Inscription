package net.samongi.Inscription.Glyphs.Attributes;

import java.util.HashMap;
import java.util.Map;

import net.samongi.Inscription.Glyphs.Glyph;

public class AttributeHandler
{
  private static AttributeHandler instance = null;
  
  /**Returns the instance of the Attribute handler
   * This is used to parse as well as register parsers for 
   * 
   * @return
   */
  public static AttributeHandler getInstance()
  {
    if(AttributeHandler.instance == null) instance = new AttributeHandler();
    return AttributeHandler.instance;
  }
  
  private Map<String, AttributeType> parsers = new HashMap<>();
  
  /**Registers a parser to a specific attribute name
   * 
   * @param attribute The attribute's type name
   * @param parser The parser
   */
  public void registerParser(AttributeType parser){this.parsers.put(parser.getName(), parser);}
  
  public Attribute parse(String line, Glyph glyph)
  {
    for(String k : this.parsers.keySet())
    {
      AttributeType p = this.parsers.get(k);
      Attribute result = p.parse(line);
      if(result == null) continue;
      return result;
    }
    return null;
  }
  
}
