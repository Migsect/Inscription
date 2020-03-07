package net.samongi.Inscription.Waypoints;

import net.samongi.Inscription.Altars;
import net.samongi.Inscription.Attributes.Types.BlockBonusAttributeType;
import net.samongi.Inscription.Attributes.Types.WaypointAttributeType;
import net.samongi.Inscription.Inscription;
import net.samongi.Inscription.Player.CacheData;
import net.samongi.Inscription.Player.PlayerData;
import net.samongi.Inscription.TypeClasses.BiomeClass;
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
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;

import javax.annotation.Nonnull;
import java.util.*;

public class Waypoint {

    //--------------------------------------------------------------------------------------------------------------------//
    private final Location m_location;

    //--------------------------------------------------------------------------------------------------------------------//
    public Waypoint(@Nonnull Location location) {
        m_location = location;
    }

    //--------------------------------------------------------------------------------------------------------------------//

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
        return distance + (sameWorld ? 0 : Inscription.getBaseInterworldDistance());
    }

    public boolean isSameWorld(@Nonnull Location other) {
        return m_location.getWorld().equals(other.getWorld());
    }

    private @Nonnull Set<BiomeClass> getBiomeClasses() {
        Biome biome = getBlock().getBiome();
        Inscription.logger.finest("Biome: " + biome);
        List<BiomeClass> biomes = Inscription.getInstance().getTypeClassManager().getBiomeClasses();
        Inscription.logger.finest("BiomeClass Amount: " + biomes.size());
        Set<BiomeClass> validBiomes = new HashSet<>(BiomeClass.getContaining(biome, biomes));
        validBiomes.remove( Inscription.getInstance().getTypeClassManager().getBiomeClass("GLOBAL"));
        return validBiomes;
    }

    private boolean isValidAltar() {
        Altar waypointAltar = Altars.getWaypointAltar();
        return waypointAltar.checkPattern(getBlock());
    }

    private double getFailureChance(int distance, int safeDistance) {
        double exponent = Math.max((distance / (double) safeDistance) - 1, 0);
        double exponentBase = Inscription.getWaypointFailureExponentBase();

        double failureChance = 1 - (1 / Math.pow(exponentBase, exponent));

        double minFailureChance = Inscription.getMinimumWaypointFailureChance();
        double maxFailureChance = Inscription.getMaximumWaypointFailureChance();

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
        Block block = world.getHighestBlockAt((int) x, (int) z);

        return new Location(m_location.getWorld(), block.getX() + 0.5, block.getY() + 1, block.getZ() + 0.5);
    }

    private @Nonnull Location getTeleportLocation() {
        return new Location(m_location.getWorld(), m_location.getX() + 0.5, m_location.getY(), m_location.getZ() + 0.5);
    }
    private void teleport(Player player, Location fromLocation, int safeDistance) {
        int distance = getDistance(fromLocation);
        double failureChance = getFailureChance(distance, safeDistance);

        Random random = new Random();
        double randomRoll = random.nextDouble();
        if (failureChance > randomRoll) {
            Location failureLocation = getFailureLocation(distance).clone();
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
        int baseDistance = Inscription.getBaseWaypointDistance();

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
        Inscription.logger.finest(fromBiome + " -> " + toBiome + " Global: " + globalDistance+ " From: " + fromDistance + " To: " + toDistance+ " FromTo: " + fromToDistance);

        return globalDistance + fromDistance + toDistance + fromToDistance + baseDistance;
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
        String distanceString = ChatColor.GRAY + "Distance: " + ChatColor.AQUA + distance + "m " ;
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

    //--------------------------------------------------------------------------------------------------------------------//
    public @Nonnull ItemStack getItemIcon(@Nonnull Location fromLocation, int safeDistance) {

        ItemStack bannerItem = isValidAltar() ? getBannerItem() : new ItemStack(Material.BARRIER);

        ItemMeta bannerItemMeta = bannerItem.getItemMeta();
        bannerItemMeta.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS);
        bannerItemMeta.setDisplayName(getIconDisplayName());

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(getDistanceString(fromLocation, safeDistance));
        lore.add(getFailureChanceString(getDistance(fromLocation), safeDistance));
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
        for (Location waypointLocation : playerData.getWaypointsSorted(m_location)) {
            Waypoint waypoint = new Waypoint(waypointLocation);
            int safeDistance = getSafeDistance(player, waypointLocation.getBlock());

            ItemStack menuIcon = waypoint.getItemIcon(m_location, safeDistance);
            boolean validWaypoint = waypoint.isValidAltar();

            menu.setItem(slot, menuIcon);
            if (validWaypoint) {
                menu.addLeftClickAction(slot, () -> {
                    player.closeInventory();
                    waypoint.teleport(player, getTeleportLocation(), safeDistance);
                }, false);
            }
            menu.addLeftClickAction(slot, () -> {
                playerData.removeWaypoint(waypointLocation);
                player.sendMessage(ChatColor.YELLOW + "Waypoint removed");
                getInventoryMenu(player).openMenu();
            }, true);
            slot++;
        }
        return menu;
    }
    //--------------------------------------------------------------------------------------------------------------------//
}
