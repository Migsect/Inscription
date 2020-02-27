package net.samongi.Inscription.Attributes.Types;

import java.util.*;

import net.md_5.bungee.api.ChatColor;
import net.samongi.Inscription.Attributes.GeneralAttributeParser;
import net.samongi.Inscription.Inscription;
import net.samongi.Inscription.Glyphs.Glyph;
import net.samongi.Inscription.Attributes.Attribute;
import net.samongi.Inscription.Attributes.AttributeType;
import net.samongi.Inscription.Attributes.AttributeTypeConstructor;
import net.samongi.Inscription.Player.CacheData;
import net.samongi.Inscription.Player.PlayerData;
import net.samongi.Inscription.TypeClasses.MaterialClass;

import net.samongi.SamongiLib.Items.ItemUtil;
import net.samongi.SamongiLib.Items.MaskedBlockData;
import net.samongi.SamongiLib.Vector.SamIntVector;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.material.Leaves;
import org.bukkit.material.MaterialData;
import org.bukkit.material.Sapling;
import org.bukkit.material.Tree;
import org.bukkit.material.Wood;
import org.bukkit.material.WoodenStep;

import javax.annotation.Nonnull;

public class ChainBreakAttributeType extends AttributeType {

    //----------------------------------------------------------------------------------------------------------------//

    private static final String TYPE_IDENTIFIER = "CHAIN_BREAK";

    //----------------------------------------------------------------------------------------------------------------//
    private int minBlocks;
    private int maxBlocks;

    private MaterialClass blockMaterials = MaterialClass.getGlobal("any items");
    private MaterialClass toolMaterials = MaterialClass.getGlobal("any items");

    //----------------------------------------------------------------------------------------------------------------//
    protected ChainBreakAttributeType(GeneralAttributeParser parser) {
        super(parser);
    }

    //----------------------------------------------------------------------------------------------------------------//

    /* *** SETTERS *** */
    public void setMin(int value) {
        this.minBlocks = value;
    }
    public void setMax(int value) {
        this.maxBlocks = value;
    }

    /* *** GETTERS *** */
    public int getMin() {
        return this.minBlocks;
    }
    public int getMax() {
        return this.maxBlocks;
    }
    public int getAmount(Glyph glyph) {
        int glyph_level = glyph.getLevel_LEGACY();
        int rarity_level = glyph.getRarity().getRank();

        double rarity_multiplier = 1 + this.m_rarityMultiplier * rarity_level;
        double baseAmount = this.minBlocks + (this.maxBlocks - this.minBlocks) * (glyph_level - 1) / (Glyph.MAX_LEVEL - 1);
        return (int) Math.floor(rarity_multiplier * baseAmount);
    }

    public String getAmountString(Glyph glyph) {
        return String.format("%d", this.getAmount(glyph));
    }
    public String getMinAmountString(Glyph glyph) {
        return String.format("%d", (int) (this.getMin() * calculateRarityMultiplier(glyph)));
    }
    public String getMaxAmountString(Glyph glyph) {

        return String.format("%d", (int) (this.getMax() * calculateRarityMultiplier(glyph)));
    }
    public String getDisplayString(Glyph glyph, String prefix, String suffix) {
        String chanceString = prefix + getAmountString(glyph) + suffix;
        String minChanceString = prefix + getMinAmountString(glyph) + suffix;
        String maxChanceString = prefix + getMaxAmountString(glyph) + suffix;

        return org.bukkit.ChatColor.BLUE + chanceString + org.bukkit.ChatColor.DARK_GRAY + "[" + minChanceString + "," + maxChanceString + "]";
    }

    @Override public Attribute generate() {
        return new Attribute(this) {

            @Override public void cache(PlayerData data) {
                CacheData cached_data = data.getData(TYPE_IDENTIFIER);
                if (cached_data == null) {
                    cached_data = new Data();
                }
                if (!(cached_data instanceof Data)) {
                    return;
                }

                Inscription.logger.finer("  Caching attribute for " + m_typeDescription);
                Inscription.logger.finer("    'blockMaterials' is global?: " + blockMaterials.isGlobal());
                Inscription.logger.finer("    'toolMaterials' is global?: " + toolMaterials.isGlobal());

                Data bonusData = (Data) cached_data;
                int amount = getAmount(this.getGlyph());
                if (blockMaterials.isGlobal() && toolMaterials.isGlobal()) {
                    int a = bonusData.get();
                    bonusData.set(a + amount);

                    Inscription.logger.finer("  +C Added '" + amount + "' bonus");
                } else if (blockMaterials.isGlobal()) {
                    for (Material tool : toolMaterials.getMaterials()) {
                        int a = bonusData.getTool(tool);
                        bonusData.setTool(tool, a + amount);

                        Inscription.logger.finer("  +C Added '" + amount + "' bonus to '" + tool.toString() + "'");
                    }
                } else if (toolMaterials.isGlobal()) {
                    for (BlockData blockData : blockMaterials.getBlockData()) {
                        int a = bonusData.getBlock(blockData);
                        bonusData.setBlock(blockData, a + amount);

                        Inscription.logger.finer("  +C Added '" + amount + "' bonus to '" + blockData.getAsString(true) + "'");
                    }
                } else {
                    for (Material type : toolMaterials.getMaterials())
                        for (BlockData blockData : blockMaterials.getBlockData()) {
                            int a = bonusData.getToolBlock(type, blockData);
                            bonusData.setToolBlock(type, blockData, a + amount);

                            Inscription.logger.finer("  +C Added '" + amount + "' bonus to '" + type.toString() + "|" + blockData.getAsString(true) + "'");
                        }
                }
                Inscription.logger.finer("  Finished caching for " + m_typeDescription);
                data.setData(bonusData); // setting the data again.

            }

            @Override public String getLoreLine() {
                String amountString = ((ChainBreakAttributeType) this.getType()).getDisplayString(this.getGlyph(), "+", "");
                String toolClass = toolMaterials.getName();
                String blockClass = blockMaterials.getName();

                String infoLine =
                    amountString + ChatColor.YELLOW + " chain breaking for " + ChatColor.BLUE + blockClass + ChatColor.YELLOW + " using " + ChatColor.BLUE
                        + toolClass;
                return "" + ChatColor.YELLOW + ChatColor.ITALIC + this.getType().getNameDescriptor() + " - " + ChatColor.RESET + infoLine;
            }

        };
    }

    public static class Data implements CacheData {

        private final static MaskedBlockData.Mask[] BLOCKDATA_MASKS = new MaskedBlockData.Mask[]{MaskedBlockData.Mask.MATERIAL, MaskedBlockData.Mask.AGEABLE};

        /* Data members of the data */
        private int global = 0;

        private HashMap<MaskedBlockData, Integer> blockAmount = new HashMap<>();
        private HashMap<Material, Integer> toolAmount = new HashMap<>();
        private HashMap<Material, HashMap<MaskedBlockData, Integer>> toolBlockAmount = new HashMap<>();

        /* Setters */
        public void set(int amount) {
            this.global = amount;
        }
        public void setBlock(BlockData blockData, int amount) {
            MaskedBlockData maskedBlockData = new MaskedBlockData(blockData, BLOCKDATA_MASKS);
            this.blockAmount.put(maskedBlockData, amount);
        }
        public void setTool(Material material, int amount) {
            this.toolAmount.put(material, amount);
        }
        public void setToolBlock(Material tool, BlockData blockData, int amount) {
            if (!this.toolBlockAmount.containsKey(tool)) {
                this.toolBlockAmount.put(tool, new HashMap<>());
            }

            MaskedBlockData maskedBlockData = new MaskedBlockData(blockData, BLOCKDATA_MASKS);
            HashMap<MaskedBlockData, Integer> blockAmount = this.toolBlockAmount.get(tool);
            blockAmount.put(maskedBlockData, amount);
        }

        /* Getters */
        public int get() {
            return this.global;
        }
        public int getBlock(BlockData blockData) {
            MaskedBlockData maskedBlockData = new MaskedBlockData(blockData, BLOCKDATA_MASKS);
            return this.blockAmount.getOrDefault(maskedBlockData, 0);
        }
        public int getTool(Material material) {
            return this.toolAmount.getOrDefault(material, 0);
        }
        public int getToolBlock(Material tool, BlockData blockData) {
            if (!this.toolBlockAmount.containsKey(tool)) {
                return 0;
            }
            HashMap<MaskedBlockData, Integer> blockAmount = this.toolBlockAmount.get(tool);

            MaskedBlockData maskedBlockData = new MaskedBlockData(blockData, BLOCKDATA_MASKS);
            return blockAmount.getOrDefault(maskedBlockData, 0);
        }

        @Override public void clear() {
            global = 0;
            blockAmount = new HashMap<>();
            toolAmount = new HashMap<>();
            toolBlockAmount = new HashMap<>();

        }

        @Override public String getType() {
            return ChainBreakAttributeType.TYPE_IDENTIFIER;
        }

        @Override public String getData() {
            // TODO Human readable data
            return "";
        }

    }

    public static class Constructor extends AttributeTypeConstructor {

        @Override public AttributeType construct(ConfigurationSection section) {

            GeneralAttributeParser parser = new GeneralAttributeParser(section, TYPE_IDENTIFIER);
            if (!parser.checkType()) {
                return null;
            }
            if (!parser.loadInfo()) {
                return null;
            }

            ChainBreakAttributeType attributeType = new ChainBreakAttributeType(parser);

            int minBlocks = section.getInt("min-blocks");
            int maxBlocks = section.getInt("max-blocks");
            if (minBlocks > maxBlocks) {
                Inscription.logger.warning(section.getName() + " : min blocks is bigger than max blocks");
                return null;
            }

            attributeType.setMin(minBlocks);
            attributeType.setMax(maxBlocks);

            String targetMaterials = section.getString("target-materials");
            if (targetMaterials != null) {
                MaterialClass materialClass = Inscription.getInstance().getTypeClassManager().getMaterialClass(targetMaterials);
                if (materialClass == null) {
                    Inscription.logger.warning("[ChainBreakAttributeType] '" + targetMaterials + "' is not a valid damage class.");
                    return null;
                }
                attributeType.toolMaterials = materialClass;
            }

            String targetBlocks = section.getString("target-blocks");
            if (targetBlocks != null) {
                MaterialClass materialClass = Inscription.getInstance().getTypeClassManager().getMaterialClass(targetBlocks);
                if (materialClass == null) {
                    Inscription.logger.warning("[ChainBreakAttributeType] '" + targetBlocks + "' is not a valid damage class.");
                    return null;
                }
                attributeType.blockMaterials = materialClass;
            }

            return attributeType;
        }

        @Override public Listener getListener() {
            return new Listener() {

                private Set<Location> usedLocations = new HashSet<>();

                private boolean isSimilarData(@Nonnull Block blockA, @Nonnull Block blockB) {
                    BlockData blockDataA = blockA.getBlockData();
                    BlockData blockDataB = blockB.getBlockData();

                    if (blockDataA.getMaterial() != blockDataB.getMaterial()) {
                        return false;
                    }
                    // Checking to see if the blocks hold the same age (for crops mostly)
                    if (blockDataA instanceof Ageable && blockDataB instanceof Ageable) {
                        Ageable ageableDataA = (Ageable) blockDataA;
                        Ageable ageableDataB = (Ageable) blockDataB;
                        if (ageableDataA.getAge() != ageableDataB.getAge()) {
                            return false;
                        }
                    }
                    return true;
                }
                @SuppressWarnings("deprecation") private boolean isSimilarData(MaterialData block1, MaterialData block2) {
                    if (block1 instanceof Wood && block2 instanceof Wood) {
                        /* Log Checking */
                        if (block1 instanceof Tree && block2 instanceof Tree) {
                            return ((Tree) block1).getSpecies().equals(((Tree) block1).getSpecies());
                        } else if (block1 instanceof Sapling && block2 instanceof Sapling) {
                            return ((Sapling) block1).getSpecies().equals(((Tree) block1).getSpecies());
                        } else if (block1 instanceof Leaves && block2 instanceof Leaves) {
                            return ((Leaves) block1).getSpecies().equals(((Tree) block1).getSpecies());
                        } else if (block1 instanceof WoodenStep && block2 instanceof WoodenStep) {
                            return ((WoodenStep) block1).getSpecies().equals(((Tree) block1).getSpecies());
                        }
                    } else if (block1.getItemType().equals(Material.STONE) && block2.getItemType().equals(Material.STONE)) {
                        return block1.getData() == block2.getData();
                    }
                    return block1.getItemType().equals(block2.getItemType());
                }

                @EventHandler public void onBlockBreak(BlockBreakEvent event) {
                    /*Making sure we don't respond to self made events (determined by the block location)*/
                    if (usedLocations.contains(event.getBlock().getLocation()) || event.isCancelled()) {
                        return;
                    }

                    Player player = event.getPlayer();

                    /* If the player is shifting, cancel the ability */
                    if (player.isSneaking()) {
                        return;
                    }

                    PlayerData playerData = Inscription.getInstance().getPlayerManager().getData(player);
                    CacheData cacheData = playerData.getData(ChainBreakAttributeType.TYPE_IDENTIFIER);
                    if (!(cacheData instanceof ChainBreakAttributeType.Data)) {
                        return;
                    }
                    ChainBreakAttributeType.Data data = (ChainBreakAttributeType.Data) cacheData;

                    Block block = event.getBlock();
                    ItemStack tool = player.getInventory().getItemInMainHand();
                    if (tool == null) {
                        tool = new ItemStack(Material.AIR);
                    }

                    BlockData blockData = block.getBlockData();
                    Material toolMaterial = tool.getType();

                    int totalBlocks = data.get() + data.getTool(toolMaterial) + data.getBlock(blockData) + data.getToolBlock(toolMaterial, blockData);

                    Inscription.logger
                        .finest("[Break Event] Chain Amount: " + totalBlocks + " (" + blockData.getMaterial() + "/" + blockData.getAsString(true) + ")");

                    /* No need to check for materials if this is less-than-equal to 0 */
                    if (totalBlocks <= 0) {
                        return;
                    }


                    /* Setting up the search's data structures */
                    Set<Location> markedLocations = new LinkedHashSet<>();
                    Queue<Block> blockQueue = new LinkedList<>();

                    /* Seeding the queue */
                    blockQueue.add(block);

                    /* Loop while we have a queue or we haven't out grown our quota */
                    while (markedLocations.size() <= totalBlocks && blockQueue.size() > 0) {
                        Inscription.logger.finest("Marked Size:" + markedLocations.size() + "/" + totalBlocks + ", Queue Size:" + blockQueue.size());
                        Block target = blockQueue.poll();
                        if (target == null) {
                            break;
                        }

                        // We are going to use a predefined list of vectors. Note that these vectors will prioritize the closer.
                        // vectors first before the further surrounding vectors.
                        // NOTE: We may want to shuffle the vectors here, or at least shuffle the subgroups to allow for more
                        // interesting breaks (and less predictable ones.)
                        List<SamIntVector> vectors = SamIntVector.getSurroundingVectorsSemiScrambled();
                        for (SamIntVector vector : vectors) {
                            Block relative = target.getRelative(vector.X(), vector.Y(), vector.Z());

                            // Checking if the block is chainable
                            if (!isSimilarData(relative, target) || markedLocations.contains(relative.getLocation())) {
                                continue;
                            }

                            // Adding to the data structures
                            blockQueue.add(relative);
                            markedLocations.add(relative.getLocation());

                            // If we reach the point where the blocks we have marked is at our total blocks, we will break out.
                            if (markedLocations.size() >= totalBlocks) {
                                break;
                            }
                        }
                        if (markedLocations.size() >= totalBlocks) {
                            break;
                        }
                    }

                    /* Breaking all the marked blocks */
                    for (Location location : markedLocations) {
                        Block target = location.getBlock();

                        // We want to allow other plugins to stop the event if needed.
                        BlockBreakEvent blockBreakEvent = new BlockBreakEvent(target, player);
                        usedLocations.add(location);
                        Bukkit.getPluginManager().callEvent(blockBreakEvent);

                        if (blockBreakEvent.isCancelled()) {
                            continue;
                        }

                        /* NOTE May not drop the normal items based on fortune */
                        target.breakNaturally(tool);

                        // Remove the location from the set to allow it to be triggered again
                        usedLocations.remove(location);

                        ItemUtil.damageItem(player, tool);

                        if (tool.getItemMeta() instanceof Damageable) {
                            Damageable damageableMeta = (Damageable) tool.getItemMeta();
                            if (damageableMeta.getDamage() >= tool.getType().getMaxDurability()) {
                                player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));

                                // Making the sound the broken item.
                                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1F, 1F);

                                break;
                            }

                        }

                    }
                }
            };
        }
    }

}
