package net.samongi.Inscription.Experience;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.samongi.Inscription.Inscription;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;

import javax.annotation.Nonnull;

/**
 * Keeps tabs on blocks that have been placed.
 */
public class BlockTracker implements Serializable {

    private static final long serialVersionUID = -2972844610158182014L;

    //------------------------------------------------------------------------------------------------//
    private Set<Location> m_placedLocations = new HashSet<>(); //  A list of locations to track
    private Set<Material> m_trackedMaterials = new HashSet<>(); // A set of materials to track

    //------------------------------------------------------------------------------------------------//
    public boolean isPlaced(@Nonnull Block block) {
        return this.isPlaced(block.getLocation());
    }
    public boolean isPlaced(@Nonnull Location location) {
        return m_placedLocations.contains(location);
    }

    //------------------------------------------------------------------------------------------------//
    public void addPlaced(@Nonnull Block block)
    {
        this.addPlaced(block.getLocation());
    }
    public void addPlaced(@Nonnull Location location) {
        this.m_placedLocations.add(location);
    }

    //------------------------------------------------------------------------------------------------//
    public void removePlaced(@Nonnull Block block) {
        this.removePlaced(block.getLocation());
    }
    public void removePlaced(@Nonnull Location location) {
        this.m_placedLocations.remove(location);
    }

    //------------------------------------------------------------------------------------------------//
    /**
     * Resets all locations that are being tracked as placed.
     * This simply empties the hashset. (makes a new one and replaced the old set)
     */
    public void clearPlaced() {
        this.m_placedLocations = new HashSet<>();
    }
    /**
     * Will cleanup all placed locations, making sure they still contain a material that is being tracked.
     * This will obviously remove any locations that have materials that are no longer being tracked.
     */
    public void cleanPlaced()
    {
        Set<Location> to_remove = new HashSet<>();
        for (Location location : this.m_placedLocations) {
            Material m = location.getBlock().getType();
            if (!this.m_trackedMaterials.contains(m)) to_remove.add(location);
        }
        this.m_placedLocations.removeAll(to_remove);
    }

    //------------------------------------------------------------------------------------------------//
    public boolean isTracked(@Nonnull Material mat) {
        return this.m_trackedMaterials.contains(mat);
    }
    public void addTracked(@Nonnull Material mat) {
        this.m_trackedMaterials.add(mat);
    }
    public void removeTracked(@Nonnull Material mat) {
        this.m_trackedMaterials.remove(mat);
    }
    public void clearTracked() {
        this.m_trackedMaterials = new HashSet<>();
    }

    //------------------------------------------------------------------------------------------------//
    public void onBlockBreak(BlockBreakEvent event)
    {
        if (event.isCancelled()) {
            return;
        }
        Location location = event.getBlock().getLocation();
        Material material = event.getBlock().getType();

        if (!this.isTracked(material) || !this.isPlaced(location)) {
            return;
        }
        this.removePlaced(location);
    }
    public void onBlockPlace(BlockPlaceEvent event)
    {
        if (event.isCancelled()) {

        }
        Location location = event.getBlock().getLocation();
        Material material = event.getBlock().getType();

        if (!this.isTracked(material)) {
            return;
        }
        this.addPlaced(location);
    }

    //------------------------------------------------------------------------------------------------//
    public void onBlockPistonExtend(BlockPistonExtendEvent event)
    {
        if (event.isCancelled()) {
            return;
        }

        List<Block> movedBlocks = event.getBlocks();
        BlockFace direction = event.getDirection();

        for (Block block : movedBlocks) {
            Material material = block.getType();
            Location location = block.getLocation();
            if (!this.isTracked(material) || !this.isPlaced(location)) {
                continue;
            }

            int deltaX = direction.getModX();
            int deltaY = direction.getModY();
            int deltaZ = direction.getModZ();
            Location deltaLocation = new Location(
                location.getWorld(),
                location.getBlockX() + deltaX,
                location.getBlockY() + deltaY,
                location.getBlockZ() + deltaZ
            );

            // Now to remove the old location and add the new location
            this.removePlaced(location);
            this.addPlaced(deltaLocation);
        }

    }
    public void onBlockPistonRetract(BlockPistonRetractEvent event)
    {
        if (event.isCancelled()) {
            return;
        }

        List<Block> moved_blocks = event.getBlocks();
        BlockFace direction = event.getDirection();

        for (Block block : moved_blocks) {
            Material material = block.getType();
            Location location = block.getLocation();
            if (!this.isTracked(material) || !this.isPlaced(location)) {
                continue;
            }

            int deltaX = direction.getModX();
            int deltaY = direction.getModY();
            int deltaZ = direction.getModZ();
            Location deltaLocation = new Location(
                location.getWorld(),
                location.getBlockX() + deltaX,
                location.getBlockY() + deltaY,
                location.getBlockZ() + deltaZ
            );

            // Now to remove the old location and add the new location
            this.removePlaced(location);
            this.addPlaced(deltaLocation);
        }
    }
    //------------------------------------------------------------------------------------------------//

    public static BlockTracker load(File file)
    {
        if (!file.exists() || file.isDirectory()) {
            return null;
        }
        BlockTracker readObject = null;
        try {
            FileInputStream fileIn = new FileInputStream(file);
            ObjectInputStream objectIn = new ObjectInputStream(fileIn);

            Object o = objectIn.readObject();
            readObject = (BlockTracker) o;

            objectIn.close();
            fileIn.close();
        }
        catch (IOException e) {
            return null;
        }
        catch (ClassNotFoundException e) {
            return null;
        }

        return readObject;
    }
    public static boolean save(BlockTracker tracker, File file)
    {
        try {
            if (!file.exists()) file.createNewFile();
            FileOutputStream fileOut = new FileOutputStream(file);
            ObjectOutputStream objectOut = new ObjectOutputStream(fileOut);

            objectOut.writeObject(tracker);

            objectOut.close();
            fileOut.close();
        }
        catch (IOException e) {
            return false;
        }
        return true;
    }
    //------------------------------------------------------------------------------------------------//
}
