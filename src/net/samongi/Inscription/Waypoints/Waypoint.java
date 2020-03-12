package net.samongi.Inscription.Waypoints;

import net.samongi.Inscription.Altars;
import net.samongi.Inscription.Attributes.Types.WaypointAttributeType;
import net.samongi.Inscription.Inscription;
import net.samongi.Inscription.Player.CacheData;
import net.samongi.Inscription.Player.PlayerData;
import net.samongi.Inscription.TypeClass.TypeClasses.BiomeClass;
import net.samongi.SamongiLib.Blocks.Altar;
import net.samongi.SamongiLib.Menu.InventoryMenu;
import net.samongi.SamongiLib.Vector.SamIntVector;
import org.bukkit.*;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Rotatable;
import org.bukkit.craftbukkit.libs.jline.internal.Nullable;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.stream.Collectors;

public class Waypoint {

    //--------------------------------------------------------------------------------------------------------------------//
    private final Location m_location;

    //--------------------------------------------------------------------------------------------------------------------//
    public Waypoint(@Nonnull Location location) {
        m_location = location;
    }

    //--------------------------------------------------------------------------------------------------------------------//
    private @Nonnull Location getLocation() {
        return m_location;
    }
    private @Nonnull Block getBlock() {
        return m_location.getBlock();
    }

    private @Nullable ItemStack getBannerItem() {
        Block block = getBlock();
        List<ItemStack> drops = new ArrayList<>(block.getDrops());
        if (drops.size() != 1) {
            return null;
        }

        ItemStack bannerItem = drops.get(0);
        bannerItem.setAmount(1);
        ItemMeta bannerItemMeta = bannerItem.getItemMeta();
        if (!(bannerItemMeta instanceof BannerMeta)) {
            return null;
        }

        return bannerItem;
    }

    private BlockFace getBannerRotation() {
        Block block = getBlock();
        BlockData blockData = block.getBlockData();

        if (!(blockData instanceof Rotatable)) {
            return null;
        }
        Rotatable rotatable = (Rotatable) blockData;
        return rotatable.getRotation();
    }

    public int getDistance(@Nonnull Location other) {
        SamIntVector fromVector = new SamIntVector(m_location);
        SamIntVector toVector = new SamIntVector(other);
        int distance = (int) fromVector.getDistance(toVector);

        boolean sameWorld = isSameWorld(other);
        return distance + (sameWorld ? 0 : Inscription.getInstance().getWaypointManager().getBaseInterworldDistance());
    }

    public boolean isSameWorld(@Nonnull Location other) {
        return m_location.getWorld().equals(other.getWorld());
    }

    private @Nonnull Set<BiomeClass> getBiomeClasses() {
        Biome biome = getBlock().getBiome();
        Inscription.logger.finest("Biome: " + biome);
        Set<BiomeClass> biomes = BiomeClass.handler.getTypeClasses();
        Inscription.logger.finest("BiomeClass Amount: " + biomes.size());
        Set<BiomeClass> validBiomes = new HashSet<>(BiomeClass.handler.getContaining(biome, biomes));
        validBiomes.remove(BiomeClass.handler.getTypeClass("GLOBAL"));
        return validBiomes;
    }

    private boolean isValidAltar() {
        Altar waypointAltar = Altars.getWaypointAltar();
        return waypointAltar.checkPattern(getBlock());
    }

    private double getFailureChance(int distance, int safeDistance) {
        double exponent = Math.max((distance / (double) safeDistance) - 1, 0);
        double exponentBase = Inscription.getInstance().getWaypointManager().getFailureExponentBase();

        double failureChance = 1 - (1 / Math.pow(exponentBase, exponent));

        double minFailureChance = Inscription.getInstance().getWaypointManager().getMinimumFailureChance();
        double maxFailureChance = Inscription.getInstance().getWaypointManager().getMaximumFailureChance();

        return Math.max(Math.min(failureChance, maxFailureChance), minFailureChance);
    }

    private @Nonnull Location getFailureLocation(int distance) {
        Random random = new Random();
        double radius = random.nextDouble() * distance;
        double x = random.nextDouble() * radius;
        double zUnsigned = Math.sqrt(x * x + radius * radius);
        double z = random.nextBoolean() ? zUnsigned : -zUnsigned;

        World world = m_location.getWorld();
        assert world != null;
        Block block = world.getHighestBlockAt((int) (x + m_location.getX()), (int) (z + m_location.getZ()));

        return new Location(m_location.getWorld(), block.getX() + 0.5, block.getY() + 1, block.getZ() + 0.5);
    }

    private @Nonnull Location getTeleportLocation() {
        return new Location(m_location.getWorld(), m_location.getX() + 0.5, m_location.getY(), m_location.getZ() + 0.5);
    }

    private void teleport(Player player, Waypoint fromWaypoint, int safeDistance) {
        int distance = getDistance(fromWaypoint.getTeleportLocation());
        double failureChance = getFailureChance(distance, safeDistance);

        Random random = new Random();
        double randomRoll = random.nextDouble();
        if (failureChance > randomRoll) {
            Location failureLocation = fromWaypoint.getFailureLocation(distance).clone();
            failureLocation.setDirection(new Vector(random.nextDouble(), 0.0, random.nextDouble()));
            player.teleport(failureLocation);
            player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, (float) 1.0, (float) 1.0);
            player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_HURT, (float) 1.0, (float) 1.0);
        } else {
            Location successLocation = getTeleportLocation();
            BlockFace bannerRotation = getBannerRotation();
            successLocation.setDirection(bannerRotation.getDirection());
            successLocation = successLocation.add(bannerRotation.getDirection().multiply(0.2));
            player.teleport(successLocation);
            player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, (float) 1.0, (float) 1.0);
        }
    }

    private int getSafeDistance(Player player, Block toBlock) {
        int baseDistance = Inscription.getInstance().getWaypointManager().getBaseDistance();

        PlayerData playerData = Inscription.getInstance().getPlayerManager().getData(player);
        CacheData data = playerData.getData(WaypointAttributeType.TYPE_IDENTIFIER);
        if (!(data instanceof WaypointAttributeType.Data)) {
            return baseDistance;
        }
        WaypointAttributeType.Data waypointData = (WaypointAttributeType.Data) data;

        Biome fromBiome = getBlock().getBiome();
        Biome toBiome = toBlock.getBiome();

        int globalDistance = waypointData.get();
        int fromDistance = waypointData.get(fromBiome, null);
        int toDistance = waypointData.get(null, toBiome);
        int fromToDistance = waypointData.get(fromBiome, toBiome);
        Inscription.logger.finest(
            fromBiome + " -> " + toBiome + " Global: " + globalDistance + " From: " + fromDistance + " To: " + toDistance + " FromTo: " + fromToDistance);

        return globalDistance + fromDistance + toDistance + fromToDistance + baseDistance;
    }

    private int getCost(Location toLocation) {
        int distance = getDistance(toLocation);
        int currencyPower = Inscription.getInstance().getWaypointManager().getCurrencyPower();
        return (int) Math.floor(distance / (double) currencyPower);
    }

    private boolean hasCost(Player player, Location toLocation) {
        Material currency = Inscription.getInstance().getWaypointManager().getCurrency();
        if (currency == null) {
            return true;
        }

        int currencyCost = getCost(toLocation);

        Inventory inventory = player.getInventory();
        return inventory.containsAtLeast(new ItemStack(currency), currencyCost);
    }

    private boolean payCost(Player player, Location toLocation) {
        int remainingCost = getCost(toLocation);
        Material currency = Inscription.getInstance().getWaypointManager().getCurrency();
        if (remainingCost == 0 || currency == null) {
            return true;
        }
        if (!hasCost(player, toLocation)) {
            return false;
        }

        Inventory inventory = player.getInventory();
        for (int index = 0; index < inventory.getSize(); index++) {
            ItemStack item = inventory.getItem(index);
            if (item == null || item.getType() != currency) {
                continue;
            }

            int itemAmount = item.getAmount();
            if (itemAmount > remainingCost) {
                item.setAmount(itemAmount - remainingCost);
                remainingCost = 0;
            } else {
                inventory.clear(index);
                remainingCost -= itemAmount;
            }

            if (remainingCost <= 0) {
                return true;
            }
        }
        return false;
    }

    //--------------------------------------------------------------------------------------------------------------------//
    private @Nonnull String getMenuDisplayName() {
        String displayName = net.md_5.bungee.api.ChatColor.DARK_PURPLE + "Waypoint ";
        ItemStack bannerItem = getBannerItem();
        if (bannerItem == null) {
            displayName += ChatColor.DARK_RED + "NO WAYPOINT BANNER";
        } else {
            ItemMeta bannerItemMeta = bannerItem.getItemMeta();
            String bannerItemDisplayName = bannerItemMeta.getDisplayName();
            displayName += ChatColor.LIGHT_PURPLE + (bannerItemDisplayName.isEmpty() ? "Unnamed Waypoint" : bannerItemDisplayName);
        }
        return displayName;
    }

    private @Nonnull String getIconDisplayName() {
        String displayName = "";

        boolean isValidAltar = isValidAltar();
        if (!isValidAltar) {
            displayName += ChatColor.DARK_RED + "[UNAVAILABLE] " + ChatColor.RED;
        } else {
            displayName += ChatColor.GREEN;
        }

        ItemStack bannerItem = getBannerItem();
        if (bannerItem == null) {
            displayName += "Banner Unavailable";
        } else {
            ItemMeta bannerItemMeta = bannerItem.getItemMeta();
            String bannerItemDisplayName = bannerItemMeta.getDisplayName();
            displayName += bannerItemDisplayName.isEmpty() ? "Unnamed Waypoint" : bannerItemDisplayName;
        }

        return displayName;
    }

    private @Nonnull String getDistanceString(@Nonnull Location other, int safeDistance) {
        int distance = getDistance(other);
        String distanceString = ChatColor.GRAY + "Distance: " + ChatColor.AQUA + distance + "m ";
        distanceString += ChatColor.GRAY + "(" + ChatColor.BLUE + safeDistance + "m" + ChatColor.GRAY + ")";
        if (!isSameWorld(other)) {
            distanceString += ChatColor.DARK_PURPLE + " [Interworld]";
        }
        return distanceString;
    }

    private @Nonnull String getBiomeString() {
        String biomeNames = "";
        Set<BiomeClass> biomeClasses = getBiomeClasses();

        int index = 0;
        for (BiomeClass biomeClass : biomeClasses) {
            biomeNames += ChatColor.AQUA + biomeClass.getName();
            if (index != biomeClasses.size() - 1) {
                biomeNames += ChatColor.GRAY + ", ";
            }
            index++;
        }
        return ChatColor.GRAY + "Biomes: " + ChatColor.AQUA + (biomeNames.isEmpty() ? "N/A" : biomeNames);
    }

    private @Nonnull String getCoordinateString() {
        String coordinateString = ChatColor.GRAY + "Coords:";
        coordinateString += "" + ChatColor.AQUA + " x:" + m_location.getBlockX();
        coordinateString += "" + ChatColor.AQUA + " y:" + m_location.getBlockY();
        coordinateString += "" + ChatColor.AQUA + " z:" + m_location.getBlockZ();
        return coordinateString;
    }

    private ChatColor getFailureChanceColor(double failureChance) {
        if (failureChance < 0.2) {
            return ChatColor.GREEN;
        } else if (failureChance < 0.4) {
            return ChatColor.YELLOW;
        } else if (failureChance < 0.6) {
            return ChatColor.GOLD;
        } else if (failureChance < 0.8) {
            return ChatColor.RED;
        } else {
            return ChatColor.DARK_RED;
        }
    }

    private String getFailureChanceString(int distance, int safeDistance) {
        double failureChance = getFailureChance(distance, safeDistance);
        String failurePercentString = String.format("%.1f%%", failureChance * 100);
        return ChatColor.GRAY + "Chance to Fail: " + getFailureChanceColor(failureChance) + failurePercentString;
    }

    private String getCostString(Player player, Location fromLocation) {
        Material currency = Inscription.getInstance().getWaypointManager().getCurrency();
        String currencyString = currency.toString().toLowerCase().replace('_', ' ');
        int cost = getCost(fromLocation);

        String costString = ChatColor.GRAY + "Cost: ";
        costString += hasCost(player, fromLocation) ? ChatColor.GREEN : ChatColor.DARK_RED;
        costString += cost + " " + currencyString + (cost > 1 ? "s" : "");
        return costString;
    }

    //--------------------------------------------------------------------------------------------------------------------//
    public @Nonnull ItemStack getItemIcon(@Nonnull Player player, @Nonnull Location fromLocation, int safeDistance) {

        ItemStack bannerItem = isValidAltar() ? getBannerItem() : new ItemStack(Material.BARRIER);

        ItemMeta bannerItemMeta = bannerItem.getItemMeta();
        bannerItemMeta.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS);
        bannerItemMeta.setDisplayName(getIconDisplayName());

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(getDistanceString(fromLocation, safeDistance));
        lore.add(getFailureChanceString(getDistance(fromLocation), safeDistance));
        lore.add(getCostString(player, fromLocation));
        lore.add("");
        lore.add(getCoordinateString());
        lore.add(getBiomeString());
        lore.add("");
        lore.add(ChatColor.YELLOW + "Shift-Click to Remove");

        bannerItemMeta.setLore(lore);
        bannerItem.setItemMeta((bannerItemMeta));

        return bannerItem;
    }

    //--------------------------------------------------------------------------------------------------------------------//
    public InventoryMenu getInventoryMenu(@Nonnull Player player) {
        InventoryMenu menu = new InventoryMenu(player, 3, getMenuDisplayName());

        PlayerData playerData = Inscription.getInstance().getPlayerManager().getData(player);
        int slot = 0;
        List<Waypoint> waypoints = playerData.getWaypoints().stream().map((Location location) -> new Waypoint(location)).collect(Collectors.toList());
        waypoints.sort((Waypoint a, Waypoint b) -> {
            int aSafeDistance = a.getSafeDistance(player, getBlock());
            int bSafeDistance = b.getSafeDistance(player, getBlock());
            int aDistance = a.getDistance(getLocation());
            int bDistance = b.getDistance(getLocation());
            double aFailChance = a.getFailureChance(aDistance, aSafeDistance);
            double bFailChance = b.getFailureChance(bDistance, bSafeDistance);
            if (aFailChance > bFailChance) {
                return 1;
            }
            if (aFailChance < bFailChance) {
                return -1;
            }
            return bDistance - aDistance;
        });

        for (Waypoint waypoint : waypoints) {
            int safeDistance = getSafeDistance(player, waypoint.getBlock());

            ItemStack menuIcon = waypoint.getItemIcon(player, m_location, safeDistance);
            boolean validWaypoint = waypoint.isValidAltar();

            menu.setItem(slot, menuIcon);
            if (validWaypoint) {
                menu.addLeftClickAction(slot, () -> {
                    boolean paid = waypoint.payCost(player, m_location);
                    if (!paid) {
                        return;
                    }

                    player.closeInventory();
                    waypoint.teleport(player, this, safeDistance);
                }, false);
            }
            menu.addLeftClickAction(slot, () -> {
                playerData.removeWaypoint(waypoint.getLocation());
                player.sendMessage(ChatColor.YELLOW + "Waypoint removed");
                getInventoryMenu(player).openMenu();
            }, true);
            slot++;
        }
        return menu;
    }
    //--------------------------------------------------------------------------------------------------------------------//
}
