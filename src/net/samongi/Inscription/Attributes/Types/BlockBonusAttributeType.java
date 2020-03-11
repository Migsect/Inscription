package net.samongi.Inscription.Attributes.Types;

import java.util.Collection;
import java.util.HashMap;
import java.util.Random;

import net.md_5.bungee.api.ChatColor;
import net.samongi.Inscription.Attributes.GeneralAttributeParser;
import net.samongi.Inscription.Experience.BlockTracker;
import net.samongi.Inscription.Inscription;
import net.samongi.Inscription.Attributes.Attribute;
import net.samongi.Inscription.Attributes.AttributeType;
import net.samongi.Inscription.Attributes.AttributeTypeConstructor;
import net.samongi.Inscription.Attributes.Base.ChanceAttributeType;
import net.samongi.Inscription.Player.CacheData;
import net.samongi.Inscription.Player.PlayerData;
import net.samongi.Inscription.TypeClass.TypeClasses.BlockClass;
import net.samongi.Inscription.TypeClass.TypeClasses.MaterialClass;

import net.samongi.SamongiLib.Items.MaskedBlockData;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

public class BlockBonusAttributeType extends ChanceAttributeType {

    //----------------------------------------------------------------------------------------------------------------//
    private static final String TYPE_IDENTIFIER = "BLOCK_BONUS";

    //----------------------------------------------------------------------------------------------------------------//
    private BlockClass m_blockMaterials;
    private MaterialClass m_toolMaterials;

    //----------------------------------------------------------------------------------------------------------------//
    protected BlockBonusAttributeType(GeneralAttributeParser parser) {
        super(parser);
    }

    //----------------------------------------------------------------------------------------------------------------//
    @Override public Attribute generate() {
        return new Attribute(this) {

            @Override public void cache(PlayerData data) {
                CacheData cached_data = data.getData(BlockBonusAttributeType.TYPE_IDENTIFIER);
                if (cached_data == null) {
                    cached_data = new BlockBonusAttributeType.Data();
                }
                if (!(cached_data instanceof BlockBonusAttributeType.Data)) {
                    return;
                }

                Inscription.logger.finer("  Caching attribute for " + m_typeDescription);
                Inscription.logger.finer("    'block_materials' is global?: " + m_blockMaterials.isGlobal());
                Inscription.logger.finer("    'tool_materials' is global?: " + m_toolMaterials.isGlobal());

                BlockBonusAttributeType.Data bonusData = (BlockBonusAttributeType.Data) cached_data;
                double chance = getChance(this.getGlyph());
                if (m_blockMaterials.isGlobal() && m_toolMaterials.isGlobal()) {
                    double c = bonusData.get();
                    bonusData.set(c + chance);

                    Inscription.logger.finer("  +C Added '" + chance + "' bonus");
                } else if (m_blockMaterials.isGlobal()) {
                    for (Material toolMaterial : m_toolMaterials.getMaterials()) {
                        double c = bonusData.getTool(toolMaterial);
                        bonusData.setTool(toolMaterial, c + chance);

                        Inscription.logger
                            .finer("  +C Added '" + chance + "' bonus to '" + toolMaterial.toString() + "'");
                    }
                } else if (m_toolMaterials.isGlobal()) {
                    for (MaskedBlockData blockData : m_blockMaterials.getBlockDatas()) {
                        double c = bonusData.getBlock(blockData.getBlockData());
                        bonusData.setBlock(blockData.getBlockData(), c + chance);

                        Inscription.logger
                            .finer("  +C Added '" + chance + "' bonus to '" + blockData.getBlockData().getAsString(true) + "'");
                    }
                } else {
                    for (Material toolMaterial : m_toolMaterials.getMaterials())
                        for (MaskedBlockData blockData : m_blockMaterials.getBlockDatas()) {
                            double c = bonusData.getToolBlock(toolMaterial, blockData.getBlockData());
                            bonusData.setToolBlock(toolMaterial, blockData.getBlockData(), c + chance);

                            Inscription.logger.finer(
                                "  +C Added '" + chance + "' bonus to '" + toolMaterial.toString() + "|" + blockData.getBlockData()
                                    .getAsString(true) + "'");
                        }
                }
                Inscription.logger.finer("  Finished caching for " + m_typeDescription);
                data.setData(bonusData); // setting the data again.
            }

            @Override public String getLoreLine() {
                String chanceString = getDisplayString(this.getGlyph(), "+", "%");
                String tool_class = m_toolMaterials.getName();
                String block_class = m_blockMaterials.getName();

                String info_line =
                    chanceString + ChatColor.YELLOW + " chance for extra drop on " + ChatColor.BLUE + block_class
                        + ChatColor.YELLOW + " using " + ChatColor.BLUE + tool_class;
                return "" + ChatColor.YELLOW + ChatColor.ITALIC + this.getType().getNameDescriptor() + " - "
                    + ChatColor.RESET + info_line;
            }
        };
    }

    public static class Data implements CacheData {

        private final static MaskedBlockData.Mask[] BLOCKDATA_MASKS = new MaskedBlockData.Mask[]{
            MaskedBlockData.Mask.MATERIAL, MaskedBlockData.Mask.AGEABLE};

        /* Data members of the data */
        private double global = 0.0;
        private HashMap<MaskedBlockData, Double> block_bonus = new HashMap<>();
        private HashMap<Material, Double> tool_bonus = new HashMap<>();
        private HashMap<Material, HashMap<MaskedBlockData, Double>> tool_block_bonus = new HashMap<>();

        // Setters
        public void set(Double amount) {
            this.global = amount;
        }
        public void setBlock(BlockData blockData, double amount) {
            MaskedBlockData maskedBlockData = new MaskedBlockData(blockData, BLOCKDATA_MASKS);
            this.block_bonus.put(maskedBlockData, amount);
        }
        public void setTool(Material mat, double amount) {
            this.tool_bonus.put(mat, amount);
        }
        public void setToolBlock(Material toolMaterial, BlockData blockData, double amount) {
            MaskedBlockData maskedBlockData = new MaskedBlockData(blockData, BLOCKDATA_MASKS);
            if (!this.tool_block_bonus.containsKey(toolMaterial)) {
                this.tool_block_bonus.put(toolMaterial, new HashMap<>());
            }
            HashMap<MaskedBlockData, Double> block_bonus = this.tool_block_bonus.get(toolMaterial);
            block_bonus.put(maskedBlockData, amount);
        }

        // Getters
        public double get() {
            return this.global;
        }
        public double getBlock(BlockData blockData) {
            MaskedBlockData maskedBlockData = new MaskedBlockData(blockData, BLOCKDATA_MASKS);
            return this.block_bonus.getOrDefault(maskedBlockData, 0.0);
        }
        public double getTool(Material material) {
            return this.tool_bonus.getOrDefault(material, 0.0);
        }
        public double getToolBlock(Material tool, BlockData blockData) {
            if (!this.tool_block_bonus.containsKey(tool)) {
                return 0;
            }
            HashMap<MaskedBlockData, Double> block_bonus = this.tool_block_bonus.get(tool);

            MaskedBlockData maskedBlockData = new MaskedBlockData(blockData, BLOCKDATA_MASKS);
            return block_bonus.getOrDefault(maskedBlockData, 0.0);
        }

        @Override public void clear() {
            this.global = 0.0;
            this.tool_bonus = new HashMap<>();
            this.block_bonus = new HashMap<>();
            this.tool_block_bonus = new HashMap<>();

        }
        @Override public String getType() {
            return TYPE_IDENTIFIER;
        }
        @Override public String getData() {
            // TODO This returns the data as a string
            return "";
        }
    } // endef

    public static class Constructor extends AttributeTypeConstructor {

        @Override public AttributeType construct(ConfigurationSection section) {

            GeneralAttributeParser parser = new GeneralAttributeParser(section, TYPE_IDENTIFIER);
            if (!parser.checkType()) {
                return null;
            }
            if (!parser.loadInfo()) {
                return null;
            }

            BlockBonusAttributeType attributeType = new BlockBonusAttributeType(parser);

            double minChance = section.getDouble("min-chance");
            double maxChance = section.getDouble("max-chance");
            if (minChance > maxChance) {
                Inscription.logger.warning(section.getName() + " : min chance is bigger than max chance");
                return null;
            }

            attributeType.setMin(minChance);
            attributeType.setMax(maxChance);

            String targetMaterials = section.getString("target-materials");
            if (targetMaterials != null) {
                MaterialClass m_class = MaterialClass.handler.getTypeClass(targetMaterials);
                if (m_class == null) {
                    Inscription.logger
                        .warning("[BlockBonusAttributeType] '" + targetMaterials + "' is not a valid material class.");
                    return null;
                }
                attributeType.m_toolMaterials = m_class;
            }
            String targetBlocks = section.getString("target-blocks");

            if (targetBlocks != null) {
                BlockClass m_class = BlockClass.handler.getTypeClass(targetBlocks);
                if (m_class == null) {
                    Inscription.logger
                        .warning("[BlockBonusAttributeType] '" + targetBlocks + "' is not a valid material class.");
                    return null;
                }
                attributeType.m_blockMaterials = m_class;
            }

            return attributeType;
        }
        @Override public Listener getListener() {
            return new Listener() {

                @EventHandler public void onBlockBreak(BlockBreakEvent event) {
                    Player player = event.getPlayer();
                    PlayerData player_data = Inscription.getInstance().getPlayerManager().getData(player);
                    CacheData data = player_data.getData(BlockBonusAttributeType.TYPE_IDENTIFIER);
                    if (!(data instanceof BlockBonusAttributeType.Data))
                        return;
                    BlockBonusAttributeType.Data bonus_data = (BlockBonusAttributeType.Data) data;

                    Block block = event.getBlock();
                    ItemStack tool = player.getInventory().getItemInMainHand();
                    if (tool == null) {
                        tool = new ItemStack(Material.AIR);
                    }

                    BlockTracker tracker = Inscription.getInstance().getBlockTracker();
                    if (tracker.isTracked(block.getType()) && tracker.isPlaced(block)) {
                        return;
                    }

                    BlockData blockData = block.getBlockData();
                    Material toolMaterial = tool.getType();

                    Collection<ItemStack> dropables = block.getDrops(tool);

                    double block_bonus = bonus_data.get();
                    block_bonus += bonus_data.getTool(toolMaterial);
                    block_bonus += bonus_data.getBlock(blockData);
                    block_bonus += bonus_data.getToolBlock(toolMaterial, blockData);

                    Inscription.logger
                        .finest("[Break Event] '" + blockData.getAsString(true) + "' Bonus Chance: " + block_bonus);

                    Location loc = block.getLocation();

                    int freeDrops = 0;
                    // we need to remove the extra 100%'s and give the reward for those
                    while (block_bonus > 1.0) {
                        block_bonus -= 1.0; // subtracting 100% for a free items
                        freeDrops += 1;
                    }
                    Random rand = new Random();
                    for (ItemStack item : dropables) {
                        int drops = freeDrops;
                        ItemStack drop = item.clone();
                        if (drop.getAmount() > 1) {
                            drop.setAmount(1);
                        }
                        if (rand.nextDouble() < block_bonus) {
                            drops++;
                        }
                        for (int c = 0; c < drops; c++)
                            loc.getWorld().dropItem(loc, drop);
                    }
                }
            };
        }
    } // Endef Constructor
}
