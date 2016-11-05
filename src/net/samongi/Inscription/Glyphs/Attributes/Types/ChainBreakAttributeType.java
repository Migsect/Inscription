package net.samongi.Inscription.Glyphs.Attributes.Types;

import java.util.HashMap;

import net.samongi.Inscription.Glyphs.Attributes.Attribute;
import net.samongi.Inscription.Glyphs.Attributes.AttributeType;
import net.samongi.Inscription.Glyphs.Attributes.AttributeTypeConstructor;
import net.samongi.Inscription.Player.CacheData;
import net.samongi.Inscription.Player.PlayerData;
import net.samongi.Inscription.TypeClasses.MaterialClass;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.Listener;

public class ChainBreakAttributeType extends AttributeType
{

  private static final long serialVersionUID = -2998698851054375672L;
  private static final String TYPE_IDENTIFIER = "CHAIN_BREAK";

  private int minBlocks;
  private int maxBlocks;

  private MaterialClass blockMaterials = MaterialClass.getGlobal("any items");
  private MaterialClass toolMaterials = MaterialClass.getGlobal("any items");

  public ChainBreakAttributeType(String typeName, String description)
  {
    super(typeName, description);
  }

  @Override
  public Attribute generate()
  {
    // TODO Auto-generated method stub
    return new Attribute(this)
    {

      @Override
      public void cache(PlayerData data)
      {
        // TODO Auto-generated method stub

      }

      @Override
      public String getLoreLine()
      {
        // TODO Auto-generated method stub
        return null;
      }

    };
  }

  public static class Data implements CacheData
  {

    /* Data members of the data */
    private int global = 0;
    private HashMap<Material, Integer> blockAmount = new HashMap<>();
    private HashMap<Material, Integer> toolAmount = new HashMap<>();
    private HashMap<Material, HashMap<Material, Integer>> toolBlockAmount = new HashMap<>();

    /* Setters */
    public void set(int amount)
    {
      this.global = amount;
    }
    public void setBlock(Material material, int amount)
    {
      this.blockAmount.put(material, amount);
    }
    public void setTool(Material material, int amount)
    {
      this.toolAmount.put(material, amount);
    }
    public void setToolBlock(Material tool, Material block, int amount)
    {
      if (!this.toolBlockAmount.containsKey(tool)) this.toolBlockAmount.put(tool, new HashMap<>());
      HashMap<Material, Integer> blockAmount = this.toolBlockAmount.get(tool);
      blockAmount.put(block, amount);
    }

    /* Getters */
    public int getToolBlock(Material tool, Material block)
    {
      if (!this.toolBlockAmount.containsKey(tool)) return 0;
      HashMap<Material, Integer> blockAmount = this.toolBlockAmount.get(tool);
      if (!blockAmount.containsKey(block)) return 0;
      return blockAmount.get(block);
    }

    @Override
    public void clear()
    {
      global = 0;
      blockAmount = new HashMap<>();
      toolAmount = new HashMap<>();
      toolBlockAmount = new HashMap<>();

    }

    @Override
    public String getType()
    {
      return ChainBreakAttributeType.TYPE_IDENTIFIER;
    }

    @Override
    public String getData()
    {
      // TODO Human readable data
      return "";
    }

  }

  public static class Constructor implements AttributeTypeConstructor
  {

    @Override
    public AttributeType construct(ConfigurationSection section)
    {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public Listener getListener()
    {
      // TODO Auto-generated method stub
      return null;
    }

  }

}
