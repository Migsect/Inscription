package net.samongi.Inscription.Attributes.Types;

import java.util.*;

import net.md_5.bungee.api.ChatColor;
import net.samongi.Inscription.Attributes.Base.AmountAttributeType;
import net.samongi.Inscription.Attributes.GeneralAttributeParser;
import net.samongi.Inscription.Inscription;
import net.samongi.Inscription.Glyphs.Glyph;
import net.samongi.Inscription.Attributes.Attribute;
import net.samongi.Inscription.Attributes.AttributeType;
import net.samongi.Inscription.Attributes.AttributeTypeFactory;
import net.samongi.Inscription.Player.CacheData;
import net.samongi.Inscription.Player.PlayerData;
import net.samongi.Inscription.TypeClass.TypeClasses.BlockClass;
import net.samongi.Inscription.TypeClass.TypeClasses.MaterialClass;

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
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;

import javax.annotation.Nonnull;

public class ChainBreakAttributeType extends AmountAttributeType {

    //----------------------------------------------------------------------------------------------------------------//
    private static final String TYPE_IDENTIFIER = "CHAIN_BREAK";

    //----------------------------------------------------------------------------------------------------------------//
    private int minBlocks;
    private int maxBlocks;

    private BlockClass m_targetBlocks = null;
    private MaterialClass m_targetTools = null;

    //----------------------------------------------------------------------------------------------------------------//
    protected ChainBreakAttributeType(@Nonnull ConfigurationSection section) throws InvalidConfigurationException {
        super(section);

        String targetToolsString = section.getString("target-materials");
        if (targetToolsString == null) {
            throw new InvalidConfigurationException("'target-materials' is not defined");
        }

        String targetBlocksString = section.getString("target-blocks");
        if (targetBlocksString == null) {
            throw new InvalidConfigurationException("'target-blocks' is not defined");
        }

        m_targetTools = MaterialClass.handler.getTypeClass(targetToolsString);
        if (m_targetTools == null) {
            throw new InvalidConfigurationException("'" + targetToolsString + "' is not a valid material class.");
        }

        m_targetBlocks = BlockClass.handler.getTypeClass(targetBlocksString);
        if (m_targetBlocks == null) {
            throw new InvalidConfigurationException("'" + targetBlocksString + "' is not a valid block class.");
        }
    }

    //----------------------------------------------------------------------------------------------------------------//
    @Override public Attribute generate() {
        return new Attribute(this) {

            @Override public void cache(PlayerData playerData) {
                CacheData cachedData = playerData.getData(TYPE_IDENTIFIER);
                if (cachedData == null) {
                    cachedData = new Data();
                }
                if (!(cachedData instanceof Data)) {
                    return;
                }

                Inscription.logger.finer("  Caching attribute for " + m_displayName);
                Inscription.logger.finer("    'blockMaterials' is global?: " + m_targetBlocks.isGlobal());
                Inscription.logger.finer("    'toolMaterials' is global?: " + m_targetTools.isGlobal());

                Data bonusData = (Data) cachedData;

                int amount = getAmount(getGlyph());
                if (m_targetBlocks.isGlobal() && m_targetTools.isGlobal()) {
                    int a = bonusData.get();
                    bonusData.set(a + amount);

                    Inscription.logger.finer("  +C Added '" + amount + "' bonus");
                } else if (m_targetBlocks.isGlobal()) {
                    for (Material tool : m_targetTools.getMaterials()) {
                        int a = bonusData.getTool(tool);
                        bonusData.setTool(tool, a + amount);

                        Inscription.logger.finer("  +C Added '" + amount + "' bonus to '" + tool.toString() + "'");
                    }
                } else if (m_targetTools.isGlobal()) {
                    for (MaskedBlockData blockData : m_targetBlocks.getBlockDatas()) {
                        int a = bonusData.getBlock(blockData.getBlockData());
                        bonusData.setBlock(blockData.getBlockData(), a + amount);

                        Inscription.logger.finer("  +C Added '" + amount + "' bonus to '" + blockData.getBlockData().getAsString(true) + "'");
                    }
                } else {
                    for (Material type : m_targetTools.getMaterials()) {
                        for (MaskedBlockData blockData : m_targetBlocks.getBlockDatas()) {
                            int a = bonusData.getToolBlock(type, blockData.getBlockData());
                            bonusData.setToolBlock(type, blockData.getBlockData(), a + amount);

                            Inscription.logger
                                .finer("  +C Added '" + amount + "' bonus to '" + type.toString() + "|" + blockData.getBlockData().getAsString(true) + "'");
                        }
                    }
                }

                Inscription.logger.finer("  Finished caching for " + m_displayName);
                playerData.setData(bonusData);

            }

            @Override public String getLoreLine() {
                String amountString = ((ChainBreakAttributeType) this.getType()).getDisplayString(this.getGlyph(), "+", "");
                String toolClass = m_targetTools.getName();
                String blockClass = m_targetBlocks.getName();

                String infoLine =
                    amountString + ChatColor.YELLOW + " chain breaking for " + ChatColor.BLUE + blockClass + ChatColor.YELLOW + " using " + ChatColor.BLUE
                        + toolClass;
                return "" + ChatColor.YELLOW + ChatColor.ITALIC + this.getType().getDisplayName() + " - " + ChatColor.RESET + infoLine;
            }

        };
    }

    //----------------------------------------------------------------------------------------------------------------//
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

    //----------------------------------------------------------------------------------------------------------------//
    public static class Factory extends AttributeTypeFactory {
        //----------------------------------------------------------------------------------------------------------------//
        @Nonnull @Override public String getAttributeTypeId() {
            return TYPE_IDENTIFIER;
        }

        //----------------------------------------------------------------------------------------------------------------//
        @Nonnull @Override public AttributeType construct(@Nonnull ConfigurationSection section) throws InvalidConfigurationException {

            return new ChainBreakAttributeType(section);
        }

        //----------------------------------------------------------------------------------------------------------------//
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

        //----------------------------------------------------------------------------------------------------------------//
    }

}
