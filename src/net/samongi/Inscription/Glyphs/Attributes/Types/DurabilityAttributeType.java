package net.samongi.Inscription.Glyphs.Attributes.Types;

import java.util.HashMap;
import java.util.Random;

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
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.ItemStack;

public class DurabilityAttributeType extends ChanceAttributeType
{

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
          double currentValue = data.get();
          /* Multiplicative bonus */
          double newValue = currentValue + (1 - currentValue) * chance;
          data.set(newValue > 1 ? 1 : newValue);
        }
        else
        {
          for (Material t : tool_materials.getMaterials())
          {
            double currentValue = data.get();
            /* Multiplicative bonus */
            double newValue = currentValue + (1 - currentValue) * chance;
            data.setTool(t, newValue > 1 ? 1 : newValue);
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
      String type = section.getString("type");
      if (type == null || !type.toUpperCase().equals(TYPE_IDENTIFIER)) return null;

      String name = section.getString("name");
      if (name == null) return null;

      String descriptor = section.getString("descriptor");
      if (descriptor == null) return null;

      double minChance = section.getDouble("min-chance");
      double maxChance = section.getDouble("max-chance");
      if (minChance > maxChance)
      {
        Inscription.logger.warning(section.getName() + " : min chance is bigger than max chance");
        return null;
      }
      double rarityMult = section.getDouble("rarity-multiplier");
      String targetMaterials = section.getString("target-materials");

      DurabilityAttributeType attributeType = new DurabilityAttributeType(name, descriptor);
      attributeType.setMin(minChance);
      attributeType.setMax(maxChance);
      attributeType.setRarityMultiplier(rarityMult);

      attributeType.base_experience = AttributeType.getIntMap(section.getConfigurationSection("base-experience"));
      attributeType.level_experience = AttributeType.getIntMap(section.getConfigurationSection("level-experience"));

      /* Setting all the targeting if there is any */
      if (targetMaterials != null)
      {
        MaterialClass m_class = Inscription.getInstance().getTypeClassManager().getMaterialClass(targetMaterials);
        attributeType.tool_materials = m_class;
      }

      return attributeType;
    }

    @Override
    public Listener getListener()
    {
      return new Listener()
      {

        @EventHandler
        public void onItemDamaged(PlayerItemDamageEvent event)
        {
          Player player = event.getPlayer();
          PlayerData playerData = Inscription.getInstance().getPlayerManager().getData(player);
          CacheData cacheData = playerData.getData(DurabilityAttributeType.TYPE_IDENTIFIER);
          if (!(cacheData instanceof DurabilityAttributeType.Data)) return;
          DurabilityAttributeType.Data data = (DurabilityAttributeType.Data) cacheData;

          ItemStack tool = event.getItem();
          if (tool == null)
          {
            return;
          }

          Material toolMaterial = tool.getType();

          /*
           * Calculating the chance for there not to be any durability, this has
           * to be between 0 and 1. The chances are multiplicative and as such
           * makes it hard to reach 100% chance.
           */
          double noDurabilityChance = data.get();
          noDurabilityChance += (1 - noDurabilityChance) * data.getTool(toolMaterial);

          Inscription.logger.finest("[Item Damage Event] No Durability Chance: " + noDurabilityChance);

          /*
           * Canceling the event if its larger than the random number, note that
           * this will prevent an AMOUNT of durability loss. In the future we
           * may want to have a chance
           * for each durability.
           */
          Random random = new Random();
          if (random.nextDouble() < noDurabilityChance)
          {
            event.setCancelled(true);
          }
        }
      };
    }
  }
}
