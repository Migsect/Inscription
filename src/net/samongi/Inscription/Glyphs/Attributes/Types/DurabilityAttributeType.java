package net.samongi.Inscription.Glyphs.Attributes.Types;

import java.util.HashMap;

import net.md_5.bungee.api.ChatColor;
import net.samongi.Inscription.Inscription;
import net.samongi.Inscription.Glyphs.Attributes.Attribute;
import net.samongi.Inscription.Glyphs.Attributes.AttributeType;
import net.samongi.Inscription.Glyphs.Attributes.AttributeTypeConstructor;
import net.samongi.Inscription.Glyphs.Attributes.Base.ChanceAttributeType;
import net.samongi.Inscription.Player.CacheData;
import net.samongi.Inscription.Player.PlayerData;
import net.samongi.Inscription.TypeClasses.MaterialClass;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.Listener;

public class DurabilityAttributeType extends ChanceAttributeType
{

  /* Debug Methods */
  private static void log(String message)
  {
    Inscription.log("[DurabilityAttributeType] " + message);
  }
  @SuppressWarnings("unused")
  private static void logDebug(String message)
  {
    if (Inscription.debug()) DurabilityAttributeType.log(Inscription.debug_tag + message);
  }
  @SuppressWarnings("unused")
  private static boolean debug()
  {
    return Inscription.debug();
  }

  /* *** static variables *** */
  private static final long serialVersionUID = -8182691382483264948L;
  private static final String TYPE_IDENTIFIER = "DURABILITY";

  /* *** class members *** */
  private MaterialClass tool_materials = MaterialClass.getGlobal("any items");

  private DurabilityAttributeType(String type_name, String description)
  {
    super(type_name, description);
  }
  @Override
  public Attribute generate()
  {
    return new Attribute(this)
    {

      private static final long serialVersionUID = -6354912159590917251L;

      @Override
      public void cache(PlayerData playerData)
      {
        CacheData cached_data = playerData.getData(DurabilityAttributeType.TYPE_IDENTIFIER);
        if (cached_data == null) cached_data = new DurabilityAttributeType.Data();
        if (!(cached_data instanceof DurabilityAttributeType.Data)) return;

        DurabilityAttributeType.Data data = (DurabilityAttributeType.Data) cached_data;
        double chance = getChance(this.getGlyph());
        if (tool_materials.isGlobal())
        {
          double c = data.get();
          data.set(c + chance);
        }
        else
        {
          for (Material t : tool_materials.getMaterials())
          {
            double c = data.getTool(t);
            data.setTool(t, c + chance);
          }
        }
        playerData.setData(data);
      }

      @Override
      public String getLoreLine()
      {
        String chance_str = getChanceString(this.getGlyph());
        String tool_class = tool_materials.getName();

        String info_line = ChatColor.BLUE + "+" + chance_str + "%" + ChatColor.YELLOW +
            " chance to not use durability using " + ChatColor.BLUE + tool_class;
        return "" + ChatColor.YELLOW + ChatColor.ITALIC + this.getType().getNameDescriptor() + " - " + ChatColor.RESET
            + info_line;
      }

    };
  }

  public static class Data implements CacheData
  {

    /* Data members of the the data */
    private double global = 0.0;
    private HashMap<Material, Double> tool_chance = new HashMap<>();

    /* *** Setters *** */
    public void set(Double amount)
    {
      this.global = amount;
    }
    public void setTool(Material mat, double amount)
    {
      this.tool_chance.put(mat, amount);
    }

    /* *** Getters *** */
    public double get()
    {
      return this.global;
    }
    public double getTool(Material mat)
    {
      if (!this.tool_chance.containsKey(mat)) return 0;
      return this.tool_chance.get(mat);
    }

    @Override
    public void clear()
    {
      this.global = 0.0;
      this.tool_chance = new HashMap<>();
    }

    @Override
    public String getType()
    {
      return TYPE_IDENTIFIER;
    }

    @Override
    public String getData()
    {
      // TODO This returns the data as a string
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
      return new Listener()
      {

      };
    }

  }
}
