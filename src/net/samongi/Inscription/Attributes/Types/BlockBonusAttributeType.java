package net.samongi.Inscription.Attributes.Types;

import java.util.*;

import net.md_5.bungee.api.ChatColor;
import net.samongi.Inscription.Attributes.Base.NumericalAttributeType;
import net.samongi.Inscription.Attributes.GeneralAttributeParser;
import net.samongi.Inscription.Conditions.Condition;
import net.samongi.Inscription.Conditions.Helpers.BlockConditionHelper;
import net.samongi.Inscription.Conditions.Helpers.PlayerConditionHelper;
import net.samongi.Inscription.Experience.BlockTracker;
import net.samongi.Inscription.Glyphs.Glyph;
import net.samongi.Inscription.Inscription;
import net.samongi.Inscription.Attributes.Attribute;
import net.samongi.Inscription.Attributes.AttributeType;
import net.samongi.Inscription.Attributes.AttributeTypeFactory;
import net.samongi.Inscription.Player.CacheData;
import net.samongi.Inscription.Player.CacheTypes.CompositeCacheData;
import net.samongi.Inscription.Player.CacheTypes.NumericCacheData;
import net.samongi.Inscription.Player.PlayerData;
import net.samongi.Inscription.TypeClass.TypeClasses.BlockClass;
import net.samongi.Inscription.TypeClass.TypeClasses.MaterialClass;

import net.samongi.SamongiLib.Items.MaskedBlockData;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nonnull;

public class BlockBonusAttributeType extends NumericalAttributeType {

    //----------------------------------------------------------------------------------------------------------------//
    private static final String TYPE_IDENTIFIER = "BLOCK_BONUS";

    //----------------------------------------------------------------------------------------------------------------//
    private Set<Condition> m_conditions = new HashSet<>();

    //----------------------------------------------------------------------------------------------------------------//
    protected BlockBonusAttributeType(@Nonnull ConfigurationSection section) throws InvalidConfigurationException {
        super(section);

        ConfigurationSection conditionSection = section.getConfigurationSection("conditions");
        if (conditionSection != null) {
            m_conditions = Inscription.getInstance().getAttributeManager().parseConditions(conditionSection);
        }
//
//        String targetToolsString = section.getString("target-materials");
//        if (targetToolsString == null) {
//            throw new InvalidConfigurationException("'target-materials' is not defined");
//        }
//
//        String targetBlocksString = section.getString("target-blocks");
//        if (targetBlocksString == null) {
//            throw new InvalidConfigurationException("'target-blocks' is not defined");
//        }
//
//        m_targetTools = MaterialClass.handler.getTypeClass(targetToolsString);
//        if (m_targetTools == null) {
//            throw new InvalidConfigurationException("'" + targetToolsString + "' is not a valid material class.");
//        }
//
//        m_targetBlocks = BlockClass.handler.getTypeClass(targetBlocksString);
//        if (m_targetBlocks == null) {
//            throw new InvalidConfigurationException("'" + targetBlocksString + "' is not a valid block class.");
//        }

    }

    //----------------------------------------------------------------------------------------------------------------//
    @Override public Attribute generate() {
        return new Attribute(this) {

            @Override public void cache(PlayerData playerData) {
                Data castedData = CacheData.getData(Data.class, TYPE_IDENTIFIER, playerData, Data::new);
                Inscription.logger.finer("  Caching attribute for " + m_displayName);
                for (Condition condition : m_conditions) {
                    Inscription.logger.finer("    Condition " + condition.toString());
                }

                double amount = getNumber(getGlyph());
                NumericalAttributeType.ReduceType reduceType = getReduceType();
                NumericCacheData numericCacheData = castedData.getCacheData(reduceType, () -> new NumericData(reduceType, reduceType.getInitialAggregator()));

                Inscription.logger.finer("    +C '" + amount + "' reducer '" + reduceType + "'");
                numericCacheData.add(m_conditions, amount);

                Inscription.logger.finer("  Finished caching for " + m_displayName);
                playerData.setData(castedData);
            }

            @Override public String getLoreLine() {
                Glyph glyph = getGlyph();
                String multiplierString = getDisplayString(glyph, 100, isPositive(glyph) ? "+" : "-", "%");

                String infoLine = multiplierString + ChatColor.YELLOW + " chance for an extra drop" + Condition.concatConditionDisplays(m_conditions);

                return getDisplayLineId() + infoLine;
            }
        };
    }

    //----------------------------------------------------------------------------------------------------------------//
    public static class Data extends CompositeCacheData<ReduceType, NumericCacheData> {

        //----------------------------------------------------------------------------------------------------------------//
        @Override public String getType() {
            return TYPE_IDENTIFIER;

        }
        @Override public String getData() {
            return "";
        }

        public double calculateAggregate(Player player, Block block)
        {
            Set<Condition> conditionGroups = PlayerConditionHelper.getConditionsForPlayer(player);
            conditionGroups.addAll(BlockConditionHelper.getConditionsForTargetBlock(block));
            return calculateConditionAggregate(conditionGroups, this);
        }
    }

    public static class NumericData extends NumericCacheData {

        NumericData(ReduceType reduceType, double dataGlobalInitial) {
            super(reduceType);
            set(dataGlobalInitial);
        }

        @Override public String getType() {
            return TYPE_IDENTIFIER;
        }
        @Override public String getData() {
            return null;
        }
    }

    //    public static class Data implements CacheData {
//
//        private final static MaskedBlockData.Mask[] BLOCKDATA_MASKS = new MaskedBlockData.Mask[]{MaskedBlockData.Mask.MATERIAL, MaskedBlockData.Mask.AGEABLE};
//
//        /* Data members of the data */
//        private double global = 0.0;
//        private HashMap<MaskedBlockData, Double> block_bonus = new HashMap<>();
//        private HashMap<Material, Double> tool_bonus = new HashMap<>();
//        private HashMap<Material, HashMap<MaskedBlockData, Double>> tool_block_bonus = new HashMap<>();
//
//        // Setters
//        public void set(Double amount) {
//            this.global = amount;
//        }
//        public void setBlock(BlockData blockData, double amount) {
//            MaskedBlockData maskedBlockData = new MaskedBlockData(blockData, BLOCKDATA_MASKS);
//            this.block_bonus.put(maskedBlockData, amount);
//        }
//        public void setTool(Material mat, double amount) {
//            this.tool_bonus.put(mat, amount);
//        }
//        public void setToolBlock(Material toolMaterial, BlockData blockData, double amount) {
//            MaskedBlockData maskedBlockData = new MaskedBlockData(blockData, BLOCKDATA_MASKS);
//            if (!this.tool_block_bonus.containsKey(toolMaterial)) {
//                this.tool_block_bonus.put(toolMaterial, new HashMap<>());
//            }
//            HashMap<MaskedBlockData, Double> block_bonus = this.tool_block_bonus.get(toolMaterial);
//            block_bonus.put(maskedBlockData, amount);
//        }
//
//        // Getters
//        public double get() {
//            return this.global;
//        }
//        public double getBlock(BlockData blockData) {
//            MaskedBlockData maskedBlockData = new MaskedBlockData(blockData, BLOCKDATA_MASKS);
//            return this.block_bonus.getOrDefault(maskedBlockData, 0.0);
//        }
//        public double getTool(Material material) {
//            return this.tool_bonus.getOrDefault(material, 0.0);
//        }
//        public double getToolBlock(Material tool, BlockData blockData) {
//            if (!this.tool_block_bonus.containsKey(tool)) {
//                return 0;
//            }
//            HashMap<MaskedBlockData, Double> block_bonus = this.tool_block_bonus.get(tool);
//
//            MaskedBlockData maskedBlockData = new MaskedBlockData(blockData, BLOCKDATA_MASKS);
//            return block_bonus.getOrDefault(maskedBlockData, 0.0);
//        }
//
//        @Override public void clear() {
//            this.global = 0.0;
//            this.tool_bonus = new HashMap<>();
//            this.block_bonus = new HashMap<>();
//            this.tool_block_bonus = new HashMap<>();
//
//        }
//        @Override public String getType() {
//            return TYPE_IDENTIFIER;
//        }
//        @Override public String getData() {
//            // TODO This returns the data as a string
//            return "";
//        }
//    } // endef
    //----------------------------------------------------------------------------------------------------------------//
    public static class Factory extends AttributeTypeFactory {

        @Override public @Nonnull String getAttributeTypeId() {
            return TYPE_IDENTIFIER;
        }

        @Nonnull @Override public AttributeType construct(@Nonnull ConfigurationSection section)
            throws InvalidConfigurationException {
            return new BlockBonusAttributeType(section);

        }
        @Override public Listener getListener() {
            return new Listener() {

                @EventHandler public void onBlockBreak(BlockBreakEvent event) {
                    Player player = event.getPlayer();
                    PlayerData player_data = Inscription.getInstance().getPlayerManager().getData(player);
                    CacheData data = player_data.getData(BlockBonusAttributeType.TYPE_IDENTIFIER);
                    if (!(data instanceof BlockBonusAttributeType.Data)) {
                        return;
                    }
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

//                    BlockData blockData = block.getBlockData();
//                    Material toolMaterial = tool.getType();

                    Collection<ItemStack> dropables = block.getDrops(tool);

                    double block_bonus = bonus_data.calculateAggregate(player, block);
//                    block_bonus += bonus_data.getTool(toolMaterial);
//                    block_bonus += bonus_data.getBlock(blockData);
//                    block_bonus += bonus_data.getToolBlock(toolMaterial, blockData);

                    Inscription.logger.finest("[Break Event] Bonus Chance: " + block_bonus);

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

    //----------------------------------------------------------------------------------------------------------------//
}
