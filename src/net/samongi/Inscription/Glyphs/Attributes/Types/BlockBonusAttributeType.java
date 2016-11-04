package net.samongi.Inscription.Glyphs.Attributes.Types;

import java.util.Collection;
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

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

public class BlockBonusAttributeType extends ChanceAttributeType
{

  /* *** static variables *** */
  private static final long serialVersionUID = 604221447378305403L;
  private static final String TYPE_IDENTIFIER = "BLOCK_BONUS";

  /* *** class members *** */
  /* Material classes */
  /* The blocks where the bonus occurs */
  private MaterialClass block_materials = MaterialClass.getGlobal("any items");
  /* The tools that the bonus occurs with */
  private MaterialClass tool_materials = MaterialClass.getGlobal("any items");

  private BlockBonusAttributeType(String type_name, String description)
  {
    super(type_name, description);
  }

  @Override
  public Attribute generate()
  {
    return new Attribute(this)
    {

      private static final long serialVersionUID = -1151134999201153827L;

      @Override
      public void cache(PlayerData data)
      {
        CacheData cached_data = data.getData(BlockBonusAttributeType.TYPE_IDENTIFIER);
        if (cached_data == null) cached_data = new BlockBonusAttributeType.Data();
        if (!(cached_data instanceof BlockBonusAttributeType.Data)) return;

        Inscription.logger.finer("  Caching attribute for " + name_description);
        Inscription.logger.finer("    'block_materials' is global?: " + block_materials.isGlobal());
        Inscription.logger.finer("    'tool_materials' is global?: " + tool_materials.isGlobal());

        BlockBonusAttributeType.Data bonus_data = (BlockBonusAttributeType.Data) cached_data;
        double chance = getChance(this.getGlyph());
        if (block_materials.isGlobal() && tool_materials.isGlobal())
        {
          double c = bonus_data.get();
          Inscription.logger.finer("C- Added '" + chance + "' bonus");
          bonus_data.set(c + chance);
        }
        else if (block_materials.isGlobal())
        {
          for (Material t : tool_materials.getMaterials())
          {
            double c = bonus_data.getTool(t);
            Inscription.logger.finer("C- Added '" + chance + "' bonus to '" + t.toString() + "'");
            bonus_data.setTool(t, c + chance);
          }
        }
        else if (tool_materials.isGlobal())
        {
          for (Material b : block_materials.getMaterials())
          {
            double c = bonus_data.getBlock(b);
            Inscription.logger.finer("C- Added '" + chance + "' bonus to '" + b.toString() + "'");
            bonus_data.setBlock(b, c + chance);
          }
        }
        else
        {
          for (Material t : tool_materials.getMaterials())
            for (Material b : block_materials.getMaterials())
            {
              double c = bonus_data.getToolBlock(t, b);
              Inscription.logger.finer("C- Added '" + chance + "' bonus to '" + t.toString() + "|"
                  + b.toString() + "'");
              bonus_data.setToolBlock(t, b, c + chance);
            }
        }
        Inscription.logger.finer("  Finished caching for " + name_description);
        data.setData(bonus_data); // setting the data again.
      }

      @Override
      public String getLoreLine()
      {
        String chance_str = getChanceString(this.getGlyph());
        String tool_class = tool_materials.getName();
        String block_class = block_materials.getName();

        String info_line = ChatColor.BLUE + "+" + chance_str + "%" + ChatColor.YELLOW + " chance for extra drop on "
            + ChatColor.BLUE + block_class + ChatColor.YELLOW + " using " + ChatColor.BLUE + tool_class;
        return "" + ChatColor.YELLOW + ChatColor.ITALIC + this.getType().getNameDescriptor() + " - " + ChatColor.RESET
            + info_line;
      }
    };
  }

  public static class Data implements CacheData
  {

    /* Data members of the data */
    private double global = 0.0;
    private HashMap<Material, Double> block_bonus = new HashMap<>();
    private HashMap<Material, Double> tool_bonus = new HashMap<>();
    private HashMap<Material, HashMap<Material, Double>> tool_block_bonus = new HashMap<>();

    // Setters
    public void set(Double amount)
    {
      this.global = amount;
    }
    public void setBlock(Material mat, double amount)
    {
      this.block_bonus.put(mat, amount);
    }
    public void setTool(Material mat, double amount)
    {
      this.tool_bonus.put(mat, amount);
    }
    public void setToolBlock(Material tool, Material block, double amount)
    {
      if (!this.tool_block_bonus.containsKey(tool)) this.tool_block_bonus.put(tool, new HashMap<>());
      HashMap<Material, Double> block_bonus = this.tool_block_bonus.get(tool);
      block_bonus.put(block, amount);
    }

    // Getters
    public double get()
    {
      return this.global;
    }
    public double getBlock(Material mat)
    {
      if (!this.block_bonus.containsKey(mat)) return 0;
      return this.block_bonus.get(mat);
    }
    public double getTool(Material mat)
    {
      if (!this.tool_bonus.containsKey(mat)) return 0;
      return this.tool_bonus.get(mat);
    }
    public double getToolBlock(Material tool, Material block)
    {
      if (!this.tool_block_bonus.containsKey(tool)) return 0;
      HashMap<Material, Double> block_bonus = this.tool_block_bonus.get(tool);
      if (!block_bonus.containsKey(block)) return 0;
      return block_bonus.get(block);
    }

    @Override
    public void clear()
    {
      this.global = 0.0;
      this.tool_bonus = new HashMap<>();
      this.block_bonus = new HashMap<>();
      this.tool_block_bonus = new HashMap<>();

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
  } // endef
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

      double min_chance = section.getDouble("min-chance");
      double max_chance = section.getDouble("max-chance");
      if (min_chance > max_chance)
      {
        Inscription.logger.finer(section.getName() + " : min chance is bigger than max chance");
        return null;
      }
      double rarity_mult = section.getDouble("rarity-multiplier");

      String target_blocks = section.getString("target-blocks");
      String target_materials = section.getString("target-materials");

      BlockBonusAttributeType attribute_type = new BlockBonusAttributeType(name, descriptor);
      attribute_type.min_chance = min_chance;
      attribute_type.max_chance = max_chance;
      attribute_type.rarity_mult = rarity_mult;

      attribute_type.base_experience = AttributeType.getIntMap(section.getConfigurationSection("base-experience"));
      attribute_type.level_experience = AttributeType.getIntMap(section.getConfigurationSection("level-experience"));

      // Setting all the targeting if there is any
      if (target_materials != null)
      {
        MaterialClass m_class = Inscription.getInstance().getTypeClassManager().getMaterialClass(target_materials);
        attribute_type.tool_materials = m_class;
      }
      if (target_blocks != null)
      {
        MaterialClass m_class = Inscription.getInstance().getTypeClassManager().getMaterialClass(target_blocks);
        attribute_type.block_materials = m_class;
      }

      return attribute_type;
    }
    @Override
    public Listener getListener()
    {
      return new Listener()
      {

        @EventHandler
        public void onBlockBreak(BlockBreakEvent event)
        {
          Player player = event.getPlayer();
          PlayerData player_data = Inscription.getInstance().getPlayerManager().getData(player);
          CacheData data = player_data.getData(BlockBonusAttributeType.TYPE_IDENTIFIER);
          if (!(data instanceof BlockBonusAttributeType.Data)) return;
          BlockBonusAttributeType.Data bonus_data = (BlockBonusAttributeType.Data) data;

          Block block = event.getBlock();
          ItemStack tool = player.getInventory().getItemInMainHand();
          if (tool == null) tool = new ItemStack(Material.AIR);

          Material block_material = block.getType();
          Material tool_material = tool.getType();

          Collection<ItemStack> dropables = block.getDrops(tool);

          double block_bonus = 0;
          block_bonus += bonus_data.get();
          block_bonus += bonus_data.getTool(tool_material);
          block_bonus += bonus_data.getBlock(block_material);
          block_bonus += bonus_data.getToolBlock(tool_material, block_material);

          Inscription.logger.finest("[Break Event] Bonus Chance: " + block_bonus);

          Location loc = block.getLocation();

          int free_drops = 0;
          // we need to remove the extra 100%'s and give the reward for those
          while (block_bonus > 1.0)
          {
            block_bonus -= 1.0; // subtracting 100% for a free items
            free_drops += 1;
          }
          Random rand = new Random();
          for (ItemStack i : dropables)
          {
            int drops = free_drops;
            ItemStack drop = i.clone();
            if (drop.getAmount() > 1) drop.setAmount(1);
            if (rand.nextDouble() < block_bonus) drops++;
            for (int c = 0; c < drops; c++)
              loc.getWorld().dropItem(loc, drop);
          }
        }
      };
    }
  } // Endef Constructor
}
