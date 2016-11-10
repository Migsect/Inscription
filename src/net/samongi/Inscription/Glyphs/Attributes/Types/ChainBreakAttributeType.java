package net.samongi.Inscription.Glyphs.Attributes.Types;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

import net.md_5.bungee.api.ChatColor;
import net.samongi.Inscription.Inscription;
import net.samongi.Inscription.Glyphs.Glyph;
import net.samongi.Inscription.Glyphs.Attributes.Attribute;
import net.samongi.Inscription.Glyphs.Attributes.AttributeType;
import net.samongi.Inscription.Glyphs.Attributes.AttributeTypeConstructor;
import net.samongi.Inscription.Player.CacheData;
import net.samongi.Inscription.Player.PlayerData;
import net.samongi.Inscription.TypeClasses.MaterialClass;
import net.samongi.SamongiLib.Exceptions.InvalidConfigurationException;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerItemBreakEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.Leaves;
import org.bukkit.material.MaterialData;
import org.bukkit.material.Sapling;
import org.bukkit.material.Tree;
import org.bukkit.material.Wood;
import org.bukkit.material.WoodenStep;

public class ChainBreakAttributeType extends AttributeType
{

  /* *** Static Members *** */
  private static final long serialVersionUID = -2998698851054375672L;
  private static final String TYPE_IDENTIFIER = "CHAIN_BREAK";

  /* *** Class Members *** */
  private int minBlocks;
  private int maxBlocks;

  private MaterialClass blockMaterials = MaterialClass.getGlobal("any items");
  private MaterialClass toolMaterials = MaterialClass.getGlobal("any items");

  public ChainBreakAttributeType(String typeName, String description)
  {
    super(typeName, description);
  }

  /* *** SETTERS *** */
  public void setMin(int value)
  {
    this.minBlocks = value;
  }
  public void setMax(int value)
  {
    this.maxBlocks = value;
  }

  /* *** GETTERS *** */
  public int getMin()
  {
    return this.minBlocks;
  }
  public int getMax()
  {
    return this.maxBlocks;
  }
  public int getAmount(Glyph glyph)
  {
    int glyph_level = glyph.getLevel();
    int rarity_level = glyph.getRarity().getRank();

    double rarity_multiplier = 1 + this.rarityMultiplier * rarity_level;
    double baseAmount = this.minBlocks + (this.maxBlocks - this.minBlocks) * (glyph_level - 1)
        / (Glyph.MAX_LEVEL - 1);
    return (int) Math.floor(rarity_multiplier * baseAmount);
  }

  public String getAmountString(Glyph glyph)
  {
    return String.format("%d", this.getAmount(glyph));
  }

  @Override
  public Attribute generate()
  {
    // TODO Auto-generated method stub
    return new Attribute(this)
    {

      private static final long serialVersionUID = 762158852113035202L;

      @Override
      public void cache(PlayerData data)
      {
        CacheData cached_data = data.getData(TYPE_IDENTIFIER);
        if (cached_data == null) cached_data = new Data();
        if (!(cached_data instanceof Data)) return;

        Inscription.logger.finer("  Caching attribute for " + typeDescription);
        Inscription.logger.finer("    'blockMaterials' is global?: " + blockMaterials.isGlobal());
        Inscription.logger.finer("    'toolMaterials' is global?: " + toolMaterials.isGlobal());

        Data bonus_data = (Data) cached_data;
        int amount = getAmount(this.getGlyph());
        if (blockMaterials.isGlobal() && toolMaterials.isGlobal())
        {
          int a = bonus_data.get();
          bonus_data.set(a + amount);

          Inscription.logger.finer("C- Added '" + amount + "' bonus");
        }
        else if (blockMaterials.isGlobal())
        {
          for (Material t : toolMaterials.getMaterials())
          {
            int a = bonus_data.getTool(t);
            bonus_data.setTool(t, a + amount);

            Inscription.logger.finer("C- Added '" + amount + "' bonus to '" + t.toString() + "'");
          }
        }
        else if (toolMaterials.isGlobal())
        {
          for (Material b : blockMaterials.getMaterials())
          {
            int a = bonus_data.getBlock(b);
            bonus_data.setBlock(b, a + amount);

            Inscription.logger.finer("C- Added '" + amount + "' bonus to '" + b.toString() + "'");
          }
        }
        else
        {
          for (Material t : toolMaterials.getMaterials())
            for (Material b : blockMaterials.getMaterials())
            {
              int a = bonus_data.getToolBlock(t, b);
              bonus_data.setToolBlock(t, b, a + amount);

              Inscription.logger.finer("C- Added '" + amount + "' bonus to '" + t.toString() + "|"
                  + b.toString() + "'");
            }
        }
        Inscription.logger.finer("  Finished caching for " + typeDescription);
        data.setData(bonus_data); // setting the data again.

      }

      @Override
      public String getLoreLine()
      {
        String amount = getAmountString(this.getGlyph());
        String toolClass = toolMaterials.getName();
        String blockClass = blockMaterials.getName();

        String infoLine = ChatColor.BLUE + "+" + amount + ChatColor.YELLOW + " chain breaking for "
            + ChatColor.BLUE + blockClass + ChatColor.YELLOW + " using " + ChatColor.BLUE + toolClass;
        return "" + ChatColor.YELLOW + ChatColor.ITALIC + this.getType().getNameDescriptor() + " - " + ChatColor.RESET
            + infoLine;
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
    public int get()
    {
      return this.global;
    }
    public int getBlock(Material material)
    {
      if (!this.blockAmount.containsKey(material)) return 0;
      return this.blockAmount.get(material);
    }
    public int getTool(Material material)
    {
      if (!this.toolAmount.containsKey(material)) return 0;
      return this.toolAmount.get(material);
    }
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
    public AttributeType construct(ConfigurationSection section) throws InvalidConfigurationException
    {
      String type = section.getString("type");
      if (type == null || !type.toUpperCase().equals(TYPE_IDENTIFIER)) return null;

      String name = section.getString("name");
      if (name == null) return null;

      String descriptor = section.getString("descriptor");
      if (descriptor == null) return null;

      int minBlocks = section.getInt("min-blocks");
      int maxBlocks = section.getInt("max-blocks");
      if (minBlocks > maxBlocks)
      {
        Inscription.logger.warning(section.getName() + " : min blocks is bigger than max blocks");
        return null;
      }
      double rarityMultiplier = section.getDouble("rarity-multiplier");

      String target_blocks = section.getString("target-blocks");
      String target_materials = section.getString("target-materials");

      ChainBreakAttributeType attributeType = new ChainBreakAttributeType(name, descriptor);
      attributeType.setMin(minBlocks);
      attributeType.setMax(maxBlocks);
      attributeType.setRarityMultiplier(rarityMultiplier);

      attributeType.baseExperience = AttributeType.getIntMap(section.getConfigurationSection("base-experience"));
      attributeType.levelExperience = AttributeType.getIntMap(section.getConfigurationSection("level-experience"));

      // Setting all the targeting if there is any
      if (target_materials != null)
      {
        MaterialClass m_class = Inscription.getInstance().getTypeClassManager().getMaterialClass(target_materials);
        if (m_class == null)
        {
          throw new InvalidConfigurationException("Material class was undefined:" + target_materials);
        }
        attributeType.toolMaterials = m_class;
      }
      if (target_blocks != null)
      {
        MaterialClass m_class = Inscription.getInstance().getTypeClassManager().getMaterialClass(target_blocks);
        if (m_class == null)
        {
          throw new InvalidConfigurationException("Material class was undefined:" + target_blocks);
        }
        attributeType.blockMaterials = m_class;
      }

      return attributeType;
    }

    @Override
    public Listener getListener()
    {
      return new Listener()
      {

        private Set<Location> usedLocations = new HashSet<>();

        @SuppressWarnings("deprecation")
        private boolean isSimilarData(MaterialData block1, MaterialData block2)
        {
          if (block1 instanceof Wood && block2 instanceof Wood)
          {
            /* Log Checking */
            if (block1 instanceof Tree && block2 instanceof Tree)
            {
              return ((Tree) block1).getSpecies().equals(((Tree) block1).getSpecies());
            }
            else if (block1 instanceof Sapling && block2 instanceof Sapling)
            {
              return ((Sapling) block1).getSpecies().equals(((Tree) block1).getSpecies());
            }
            else if (block1 instanceof Leaves && block2 instanceof Leaves)
            {
              return ((Leaves) block1).getSpecies().equals(((Tree) block1).getSpecies());
            }
            else if (block1 instanceof WoodenStep && block2 instanceof WoodenStep)
            {
              return ((WoodenStep) block1).getSpecies().equals(((Tree) block1).getSpecies());
            }
          }
          else if (block1.getItemType().equals(Material.STONE) && block2.getItemType().equals(Material.STONE))
          {
            return block1.getData() == block2.getData();
          }
          return block1.getItemType().equals(block2.getItemType());
        }

        @EventHandler
        public void onBlockBreak(BlockBreakEvent event)
        {
          /*
           * Making sure we don't respond to self made events (determined by the
           * block location)
           */
          if (usedLocations.contains(event.getBlock().getLocation())) return;

          Player player = event.getPlayer();

          /* If the player is shifting, cancel the ability */
          if (player.isSneaking()) return;

          PlayerData playerData = Inscription.getInstance().getPlayerManager().getData(player);
          CacheData cacheData = playerData.getData(ChainBreakAttributeType.TYPE_IDENTIFIER);
          if (!(cacheData instanceof ChainBreakAttributeType.Data)) return;
          ChainBreakAttributeType.Data data = (ChainBreakAttributeType.Data) cacheData;

          Block block = event.getBlock();
          ItemStack tool = player.getInventory().getItemInMainHand();
          if (tool == null) tool = new ItemStack(Material.AIR);

          Material blockMaterial = block.getType();
          Material toolMaterial = tool.getType();

          int totalBlocks = data.get();
          totalBlocks += data.getTool(toolMaterial)
              + data.getBlock(blockMaterial)
              + data.getToolBlock(toolMaterial, blockMaterial);

          Inscription.logger.finest("[Break Event] Chain Amount: " + totalBlocks);

          /* No need to check for materials if this is less-than-equal to 0 */
          if (totalBlocks <= 0) return;

          /* Setting up the search's data structures */
          Set<Location> markedLocations = new LinkedHashSet<>();
          Queue<Block> blockQueue = new LinkedList<>();

          /* Seeding the queue */
          blockQueue.add(block);

          /* Loop while we have a queue or we haven't out grown our quota */
          while (markedLocations.size() <= totalBlocks && blockQueue.size() > 0)
          {
            Inscription.logger.finest("Marked Size:" + markedLocations.size() + ", Queue Size:" + blockQueue.size());
            Block target = blockQueue.poll();
            if (target == null) break;

            /* Looping through all the relative blocks */
            for (int x = -1; x <= 1; x++)
              for (int y = -1; y <= 1; y++)
                for (int z = -1; z <= 1; z++)
                {
                  if (x == 0 && y == 0 && z == 0) continue;
                  Block relative = target.getRelative(x, y, z);

                  /* Checking if the block is chainable */
                  if (!isSimilarData(relative.getState().getData(), target.getState().getData()) || markedLocations.contains(relative.getLocation()))
                  {
                    continue;
                  }

                  /* Adding to the data structures */
                  blockQueue.add(relative);
                  markedLocations.add(relative.getLocation());
                  if (markedLocations.size() >= totalBlocks)
                  {
                    break;
                  }
                }
          }

          /* Breaking all the marked blocks */
          for (Location location : markedLocations)
          {
            Block target = location.getBlock();

            /* Breaking the block */
            BlockBreakEvent blockBreakEvent = new BlockBreakEvent(target, player);
            usedLocations.add(location);
            Bukkit.getPluginManager().callEvent(blockBreakEvent);
            if (blockBreakEvent.isCancelled())
            {
              continue;
            }
            /* NOTE May not drop the normal items based on fortune */
            target.breakNaturally(tool);
            /*
             * Remove the location from the set to allow it to be triggered
             * again
             */
            usedLocations.remove(location);

            /* Damaging the tool */
            PlayerItemDamageEvent itemDamageEvent = new PlayerItemDamageEvent(player, tool, 1);
            Bukkit.getPluginManager().callEvent(itemDamageEvent);
            if (!blockBreakEvent.isCancelled())
            {
              tool.setDurability((short) (tool.getDurability() + 1));
              /* Breaking the tool */
              if (tool.getDurability() > tool.getType().getMaxDurability())
              {
                /* Sending an event out that the item broke */
                PlayerItemBreakEvent itemBreakEvent = new PlayerItemBreakEvent(player, tool);
                Bukkit.getPluginManager().callEvent(itemBreakEvent);
                tool.setDurability((short) 0);

                /* Clearing the item slot */
                player.getInventory().clear(player.getInventory().getHeldItemSlot());
                player.playSound(
                    player.getLocation(),
                    Sound.ENTITY_ITEM_BREAK, 1F, 1F);

                /* Breaking the entire loop since we can't break blocks anymore */
                break;
              }
            }
          }
        }
      };
    }
  }

}
