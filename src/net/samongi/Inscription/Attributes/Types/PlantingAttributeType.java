package net.samongi.Inscription.Attributes.Types;

import net.md_5.bungee.api.ChatColor;
import net.samongi.Inscription.Attributes.Attribute;
import net.samongi.Inscription.Attributes.AttributeType;
import net.samongi.Inscription.Attributes.AttributeTypeFactory;
import net.samongi.Inscription.Attributes.Base.AmountAttributeType;
import net.samongi.Inscription.Attributes.Base.NumericalAttributeType;
import net.samongi.Inscription.Conditions.Condition;
import net.samongi.Inscription.Conditions.Helpers.PlayerConditionHelper;
import net.samongi.Inscription.Glyphs.Glyph;
import net.samongi.Inscription.Inscription;
import net.samongi.Inscription.Player.CacheData;
import net.samongi.Inscription.Player.CacheTypes.CompositeCacheData;
import net.samongi.Inscription.Player.CacheTypes.NumericCacheData;
import net.samongi.Inscription.Player.PlayerData;
import net.samongi.SamongiLib.Blocks.BlockUtil;
import net.samongi.SamongiLib.Vector.SamIntVector;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import javax.annotation.Nonnull;
import java.util.*;

public class PlantingAttributeType extends AmountAttributeType {

    //----------------------------------------------------------------------------------------------------------------//
    private static final String TYPE_IDENTIFIER = "PLANTING";

    //----------------------------------------------------------------------------------------------------------------//
    Set<Condition> m_conditions = new HashSet<>();

    //----------------------------------------------------------------------------------------------------------------//
    protected PlantingAttributeType(@Nonnull ConfigurationSection section) throws InvalidConfigurationException {
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

                String infoLine = multiplierString + ChatColor.YELLOW + " area planting" + Condition.concatConditionDisplays(m_conditions);

                return getDisplayLineId() + infoLine;
            }

        };
    }

    //----------------------------------------------------------------------------------------------------------------//
    public static class Data extends CompositeCacheData<ReduceType, NumericCacheData> {

        @Override public String getType() {
            return TYPE_IDENTIFIER;

        }
        @Override public String getData() {
            return "";
        }

        public double calculateAggregate(Player player) {
            Set<Condition> conditionGroups = PlayerConditionHelper.getConditionsForPlayer(player);
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

            return new PlantingAttributeType(section);
        }

        //----------------------------------------------------------------------------------------------------------------//
        @Override public Listener getListener() {
            return new Listener() {

                private Set<Location> usedLocations = new HashSet<>();

                @EventHandler void onBlockPlaceEvent(BlockPlaceEvent event) {
                    Block block = event.getBlock();
                    // TODO Maybe handle blockFaces in a special way to be able to handle odd crops like cocoa beans
                    // BlockFace blockFace = BlockUtil.calculateRelativeFace(block, event.getBlockAgainst());

                    if (usedLocations.contains(block.getLocation()) || event.isCancelled()) {
                        return;
                    }

                    Player player = event.getPlayer();

                    /* If the player is shifting, cancel the ability */
                    if (player.isSneaking()) {
                        return;
                    }

                    PlayerData playerData = Inscription.getInstance().getPlayerManager().getData(player);
                    assert playerData != null;
                    CacheData cacheData = playerData.getData(TYPE_IDENTIFIER);
                    if (!(cacheData instanceof Data)) {
                        return;
                    }
                    Data data = (Data) cacheData;

                    int totalBlocks = (int) Math.floor(data.calculateAggregate(player));

                    /* No need to check for materials if this is not positive */
                    if (totalBlocks <= 0) {
                        return;
                    }

                    Material placeMaterial = event.getBlock().getType();
                    Material itemMaterial = event.getItemInHand().getType();

                    int totalItems = 0;
                    PlayerInventory playerInventory = player.getInventory();
                    for (int slot = 0; slot < playerInventory.getSize(); slot++) {
                        ItemStack item = playerInventory.getItem(slot);
                        if (item != null && item.getType() == itemMaterial) {
                            totalItems += item.getAmount();
                        }
                    }

                    // Subtracting one item because it'll be used for the normal block place.
                    int placeLimit = Math.min(totalItems - 1, totalBlocks);

                    Inscription.logger.finest("[Place Block Event] Planting Amount: " + placeLimit);
                    if (placeLimit <= 0) {
                        return;
                    }

                    Set<Location> markedLocations = new LinkedHashSet<>();
                    Queue<Block> blockQueue = new LinkedList<>();

                    /* Seeding the queue */
                    Block farmlandBlock = block.getRelative(BlockFace.DOWN);
                    // We aren't adding the original block to marked locations because it's already placed as of this event.
                    blockQueue.add(farmlandBlock);
                    Material farmlandType = farmlandBlock.getType();

                    while (blockQueue.size() > 0) {
                        Block target = blockQueue.poll();
                        if (target == null) {
                            break;
                        }

                        List<SamIntVector> vectors = SamIntVector.getSurroundingVectorsSemiScrambled();
                        for (SamIntVector vector : vectors) {
                            Block relative = target.getRelative(vector.X(), vector.Y(), vector.Z());

                            // Checking if the block is plantable (matches the original block
                            boolean plantable = relative.getType() == farmlandType && relative.getRelative(BlockFace.UP).isEmpty();
                            if (!plantable || markedLocations.contains(relative.getLocation())) {
                                continue;
                            }

                            // Adding to the data structures
                            blockQueue.add(relative);
                            markedLocations.add(relative.getLocation());

                            // If we reach the point where the blocks we have marked is at our total blocks, we will break out.
                            if (markedLocations.size() >= placeLimit) {
                                break;
                            }
                        }

                        if (markedLocations.size() >= placeLimit) {
                            break;
                        }
                    }

                    // We need to prioritize the item in hand.
                    int amountPlaced = markedLocations.size();
                    ItemStack seed = event.getItemInHand();
                    EquipmentSlot seedSlot = event.getHand();

                    for (Location location : markedLocations) {
                        // This is handled normally by minecraft.
                        if (location.equals(farmlandBlock.getLocation())) {
                            Inscription.logger.finest("location.equals(farmlandBlock.getLocation())");
                            continue;
                        }

                        Block underTarget = location.getBlock();
                        Block target = underTarget.getRelative(BlockFace.UP);
                        BlockState replacedState = target.getState();
                        target.setType(placeMaterial);

                        usedLocations.add(target.getLocation());
                        BlockPlaceEvent newPlaceEvent = new BlockPlaceEvent(target, replacedState, target, event.getItemInHand(), player, event.canBuild(),
                            event.getHand());
                        Bukkit.getPluginManager().callEvent(newPlaceEvent);
                        usedLocations.remove(target.getLocation());

                        if (!newPlaceEvent.canBuild() || event.isCancelled()) {
                            replacedState.update();
                            // If the block isn't going to be placed, then we need not charge the player the item
                            amountPlaced--;
                            continue;
                        }


                    }

                    // Leaving one item at the least so Minecraft can handle it.
                    if (amountPlaced >= (seed.getAmount() - 1)) {
                        seed.setAmount(1);
                        amountPlaced -= (seed.getAmount() - 1);
                    } else {
                        seed.setAmount((seed.getAmount() - amountPlaced));
                        amountPlaced = 0;
                    }

                    if (seedSlot == EquipmentSlot.HAND) {
                        playerInventory.setItemInMainHand(seed);
                    } else if (seedSlot == EquipmentSlot.OFF_HAND) {
                        playerInventory.setItemInOffHand(seed);
                    }

                    // Handling the overflow from the main hand.
                    for (int slot = 0; slot < playerInventory.getSize() && amountPlaced > 0; slot++) {
                        ItemStack item = playerInventory.getItem(slot);
                        if (item == null || item.getType() != itemMaterial) {
                            continue;
                        }

                        if (item.getAmount() > amountPlaced) {
                            item.setAmount(item.getAmount() - amountPlaced);
                            amountPlaced = 0;
                        } else {
                            amountPlaced -= item.getAmount();
                            playerInventory.setItem(slot, new ItemStack(Material.AIR));
                        }
                    }
                }
            };
        }

        //----------------------------------------------------------------------------------------------------------------//
    }
    //----------------------------------------------------------------------------------------------------------------//
}
