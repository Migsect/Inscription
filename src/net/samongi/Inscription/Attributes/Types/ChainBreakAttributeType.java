package net.samongi.Inscription.Attributes.Types;

import java.util.*;

import net.md_5.bungee.api.ChatColor;
import net.samongi.Inscription.Attributes.Base.AmountAttributeType;
import net.samongi.Inscription.Attributes.Base.NumericalAttributeType;
import net.samongi.Inscription.Attributes.GeneralAttributeParser;
import net.samongi.Inscription.Conditions.Condition;
import net.samongi.Inscription.Conditions.Helpers.BlockConditionHelper;
import net.samongi.Inscription.Conditions.Helpers.PlayerConditionHelper;
import net.samongi.Inscription.Conditions.Helpers.TargetEntityConditionHelper;
import net.samongi.Inscription.Inscription;
import net.samongi.Inscription.Glyphs.Glyph;
import net.samongi.Inscription.Attributes.Attribute;
import net.samongi.Inscription.Attributes.AttributeType;
import net.samongi.Inscription.Attributes.AttributeTypeFactory;
import net.samongi.Inscription.Player.CacheData;
import net.samongi.Inscription.Player.CacheTypes.CompositeCacheData;
import net.samongi.Inscription.Player.CacheTypes.NumericCacheData;
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
import org.bukkit.entity.Entity;
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
    Set<Condition> m_conditions = new HashSet<>();

    //----------------------------------------------------------------------------------------------------------------//
    protected ChainBreakAttributeType(@Nonnull ConfigurationSection section) throws InvalidConfigurationException {
        super(section);

        ConfigurationSection conditionSection = section.getConfigurationSection("conditions");
        if (conditionSection != null) {
            m_conditions = Inscription.getInstance().getAttributeManager().parseConditions(conditionSection);
        }
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
                String multiplierString = getDisplayString(glyph, isPositive(glyph) ? "+" : "-", "");

                String infoLine = multiplierString + ChatColor.YELLOW + " chain breaking" + Condition.concatConditionDisplays(m_conditions);

                return getDisplayLineId() + infoLine;
            }

        };
    }

    public static class Data extends CompositeCacheData<ReduceType, NumericCacheData> {

        //----------------------------------------------------------------------------------------------------------------//
        @Override public String getType() {
            return TYPE_IDENTIFIER;

        }
        @Override public String getData() {
            return "";
        }

        public double calculateAggregate(Player player, Block block) {
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

                    //                    BlockData blockData = block.getBlockData();
                    //                    Material toolMaterial = tool.getType();

                    int totalBlocks = (int) Math.floor(data.calculateAggregate(player,
                        block)); //data.get() + data.getTool(toolMaterial) + data.getBlock(blockData) + data.getToolBlock(toolMaterial, blockData);

                    Inscription.logger.finest("[Break Event] Chain Amount: " + totalBlocks);

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


                        if (tool.getItemMeta() instanceof Damageable && tool.getType().getMaxDurability() > 0) {
                            ItemUtil.damageItem(player, tool);

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
