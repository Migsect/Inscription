package net.samongi.Inscription.Experience;

import java.io.*;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.samongi.Inscription.Inscription;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.annotation.Nonnull;

/**
 * Keeps tabs on blocks that have been placed.
 */
public class BlockTracker implements Serializable, Listener {

    private static final long serialVersionUID = -2972844610158182014L;
    private static final int DEFAULT_SAVE_DENSITY = 8;

    private static class ChunkPosition {

        public final World world;
        public final Integer x;
        public final Integer z;

        public ChunkPosition(World world, Integer x, Integer z) {
            this.world = world;
            this.x = x;
            this.z = z;
        }

        public ChunkPosition(Chunk chunk) {
            this.world = chunk.getWorld();
            this.x = chunk.getX();
            this.z = chunk.getZ();
        }

        @Override public int hashCode() {
            return 11 * world.hashCode() + 31 * (x.hashCode() + 97 * z.hashCode());
        }

        public boolean equals(ChunkPosition other) {
            return other.world.equals(world) && other.x.equals(x) && other.z.equals(z);
        }
        @Override public boolean equals(Object other) {
            if (other instanceof ChunkPosition) {
                return this.equals((ChunkPosition) other);
            }
            return false;
        }

        @Override public String toString() {
            return String.format("%s %d %d", world.getName(), x, z);
        }
    }

    private static class ChunkRegionPosition {

        public final World world;
        public final Integer x;
        public final Integer z;

        public ChunkRegionPosition(World world, Integer x, Integer z) {
            this.world = world;
            this.x = x;
            this.z = z;
        }

        public ChunkRegionPosition(Chunk chunk, int density) {
            this.world = chunk.getWorld();
            this.x = chunk.getX() / density;
            this.z = chunk.getZ() / density;
        }

        @Override public int hashCode() {
            return 11 * world.hashCode() + 31 * (x.hashCode() + 97 * z.hashCode());
        }

        public boolean equals(ChunkRegionPosition other) {
            return other.world.equals(world) && other.x.equals(x) && other.z.equals(z);
        }
        @Override public boolean equals(Object other) {
            if (other instanceof ChunkRegionPosition) {
                return this.equals((ChunkRegionPosition) other);
            }
            return false;
        }
    }

    //------------------------------------------------------------------------------------------------//
    private Set<Location> m_placedLocations = new HashSet<>(); //  A list of locations to track
    private transient Set<Material> m_trackedMaterials = new HashSet<>(); // A set of materials to track

    private final File m_saveFolder;
    private final int m_saveDensity;
    // Chunk regions are groups of chunks that reduce the number of files that need to be generated
    // when saving the placed locations.
    private HashMap<ChunkRegionPosition, Set<Location>> m_chunksRegions = new HashMap<>();
    // Used to track how many chunks are loaded in this region.
    // This is used to determine if we should unload a chunk region from memory.
    private HashMap<ChunkRegionPosition, Integer> m_loadedCounts = new HashMap<>();
    private Set<ChunkPosition> m_loadedChunks = new HashSet<>();

    //------------------------------------------------------------------------------------------------//
    public BlockTracker(File saveFolder, int saveDensity) {
        m_saveFolder = saveFolder;
        m_saveDensity = saveDensity;
        ensureSaveFolder();
    }
    public BlockTracker(File saveFolder) {
        m_saveFolder = saveFolder;
        m_saveDensity = DEFAULT_SAVE_DENSITY;
        ensureSaveFolder();
    }

    private void ensureSaveFolder() {
        if (!m_saveFolder.exists()) {
            m_saveFolder.mkdir();
        }
    }
    //------------------------------------------------------------------------------------------------//
    private File getRegionFile(@Nonnull ChunkRegionPosition position) {
        String name = String.format("%s.%d.%d.json", position.world.getName(), position.x, position.z);
        File file = new File(m_saveFolder, name);
        return file;
    }
    public boolean loadChunkRegion(@Nonnull ChunkRegionPosition regionPosition) {
        File saveFile = getRegionFile(regionPosition);
        Set<Location> locations = new HashSet<>();

        if (!saveFile.isFile()) {
            m_chunksRegions.put(regionPosition, locations);
            return false;
        }

        JSONArray locationList = null;

        JSONParser jsonParser = new JSONParser();
        try (FileReader reader = new FileReader(saveFile)) {

            Object object = jsonParser.parse(reader);
            if (!(object instanceof JSONArray)) {
                return false;
            }
            locationList = (JSONArray) object;

        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        }
        catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        catch (ParseException e) {
            e.printStackTrace();
            return false;
        }

        for (Object element : locationList) {
            if (!(element instanceof JSONObject)) {
                continue;
            }
            JSONObject object = (JSONObject) element;
            Long x = (Long) object.get("x");
            Long y = (Long) object.get("y");
            Long z = (Long) object.get("z");
            String w = (String) object.get("w");

            World world = Bukkit.getWorld(w);

            Location location = new Location(world, x, y, z);
            locations.add(location);
        }

        m_chunksRegions.put(regionPosition, locations);
        return true;
    }
    public boolean loadChunk(@Nonnull Chunk chunk) {
        ChunkPosition chunkPosition = new ChunkPosition(chunk);
        if (m_loadedChunks.contains(chunkPosition)) {
            return false;
        }
        m_loadedChunks.add(chunkPosition);

        ChunkRegionPosition regionPosition = new ChunkRegionPosition(chunk, m_saveDensity);
        // We are going to increment the loaded counts.
        m_loadedCounts.put(regionPosition, m_loadedCounts.getOrDefault(regionPosition, 0) + 1);

        if (m_chunksRegions.containsKey(regionPosition)) {
            return false;
        }
        return loadChunkRegion(regionPosition);
    }

    public void loadWorldChunks(World world) {
        for (Chunk chunk : world.getLoadedChunks()) {
            loadChunk(chunk);
        }
    }

    public boolean saveChunkRegion(@Nonnull ChunkRegionPosition regionPosition) {
        File saveFile = getRegionFile(regionPosition);
        Set<Location> locations = m_chunksRegions.get(regionPosition);

        // We are going to delete the locations file if it exists and if it does then we will delete it.
        if (locations.isEmpty() && saveFile.isFile()) {
            return saveFile.delete();
        }
        if (locations.isEmpty()) {
            return false;
        }

        JSONArray saveArray = new JSONArray();
        for (Location location : locations) {
            JSONObject locationObject = new JSONObject();
            locationObject.put("x", location.getBlockX());
            locationObject.put("y", location.getBlockY());
            locationObject.put("z", location.getBlockZ());
            locationObject.put("w", location.getWorld().getName());
            saveArray.add(locationObject);
        }
        FileWriter fileWriter = null;
        try {
            fileWriter = new FileWriter(saveFile);
            fileWriter.write(saveArray.toJSONString());
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        finally {

            try {
                if (fileWriter != null) {
                    fileWriter.flush();
                    fileWriter.close();
                }
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
        return true;
    }

    public void saveAllChunkRegions() {
        for (ChunkRegionPosition regionPosition : m_chunksRegions.keySet()) {
            saveChunkRegion(regionPosition);
        }
    }

    @SuppressWarnings("unchecked") public boolean unloadChunk(@Nonnull Chunk chunk) {
        ChunkPosition chunkPosition = new ChunkPosition(chunk);
        m_loadedChunks.remove(chunkPosition);

        ChunkRegionPosition regionPosition = new ChunkRegionPosition(chunk, m_saveDensity);
        if (!m_chunksRegions.containsKey(regionPosition)) {
            return false;
        }

        // We are going to decrement the loaded counts and if it's less than or 0, then we can save.
        m_loadedCounts.put(regionPosition, m_loadedCounts.get(regionPosition) - 1);
        if (m_loadedCounts.get(regionPosition) > 0) {
            return false;
        }

        boolean saveStatus = saveChunkRegion(regionPosition);
        m_loadedCounts.remove(regionPosition);
        return saveStatus;

    }

    //------------------------------------------------------------------------------------------------//
    public boolean isPlaced(@Nonnull Block block) {
        ChunkRegionPosition regionPosition = new ChunkRegionPosition(block.getChunk(), m_saveDensity);
        Set<Location> locations = m_chunksRegions.get(regionPosition);
        return locations.contains(block.getLocation());
    }
    public boolean isPlaced(@Nonnull Location location) {
        return isPlaced(location.getBlock());
    }

    //------------------------------------------------------------------------------------------------//
    public void addPlaced(@Nonnull Block block) {
        ChunkRegionPosition regionPosition = new ChunkRegionPosition(block.getChunk(), m_saveDensity);
        Set<Location> locations = m_chunksRegions.get(regionPosition);
        locations.add(block.getLocation());
    }
    public void addPlaced(@Nonnull Location location) {
        addPlaced(location.getBlock());
    }

    //------------------------------------------------------------------------------------------------//
    public void removePlaced(@Nonnull Block block) {
        ChunkRegionPosition regionPosition = new ChunkRegionPosition(block.getChunk(), m_saveDensity);
        Set<Location> locations = m_chunksRegions.get(regionPosition);
        locations.remove(block.getLocation());
    }
    public void removePlaced(@Nonnull Location location) {
        removePlaced(location.getBlock());
    }

    //------------------------------------------------------------------------------------------------//
    /**
     * Resets all locations that are being tracked as placed.
     * This simply empties the hashset. (makes a new one and replaced the old set)
     */
    @Deprecated public void clearPlaced() {
        this.m_placedLocations = new HashSet<>();
    }
    /**
     * Will cleanup all placed locations, making sure they still contain a material that is being tracked.
     * This will obviously remove any locations that have materials that are no longer being tracked.
     */
    @Deprecated public void cleanPlaced() {
        Set<Location> to_remove = new HashSet<>();
        for (Location location : this.m_placedLocations) {
            Material m = location.getBlock().getType();
            if (!this.m_trackedMaterials.contains(m))
                to_remove.add(location);
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
    @EventHandler public void onChunkLoad(ChunkLoadEvent event) {
        Chunk chunk = event.getChunk();
        loadChunk(chunk);
    }

    @EventHandler public void onChunkUnload(ChunkUnloadEvent event) {
        Chunk chunk = event.getChunk();
        unloadChunk(chunk);
    }

    //------------------------------------------------------------------------------------------------//
    @EventHandler(priority = EventPriority.HIGH) public void onBlockBreak(BlockBreakEvent event) {
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
    @EventHandler public void onBlockPlace(BlockPlaceEvent event) {
        if (event.isCancelled()) {
            return;
        }
        Location location = event.getBlock().getLocation();
        Material material = event.getBlock().getType();

        if (!this.isTracked(material)) {
            return;
        }
        this.addPlaced(location);
    }

    //------------------------------------------------------------------------------------------------//
    @EventHandler public void onBlockPistonExtend(BlockPistonExtendEvent event) {
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
            Location deltaLocation = new Location(location.getWorld(), location.getBlockX() + deltaX,
                location.getBlockY() + deltaY, location.getBlockZ() + deltaZ);

            // Now to remove the old location and add the new location
            this.removePlaced(location);
            this.addPlaced(deltaLocation);
        }

    }
    @EventHandler public void onBlockPistonRetract(BlockPistonRetractEvent event) {
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
            Location deltaLocation = new Location(location.getWorld(), location.getBlockX() + deltaX,
                location.getBlockY() + deltaY, location.getBlockZ() + deltaZ);

            // Now to remove the old location and add the new location
            this.removePlaced(location);
            this.addPlaced(deltaLocation);
        }
    }
    //------------------------------------------------------------------------------------------------//
    public void configureTracker(FileConfiguration config) {
        List<String> trackedMaterialStrings = config.getStringList("placement-tracking");
        Set<Material> trackedMaterials = new HashSet<>();
        for (String s : trackedMaterialStrings) {
            Material m = Material.getMaterial(s);
            if (m == null) {
                continue;
            }
            trackedMaterials.add(m);
        }

        clearTracked();
        for (Material material : trackedMaterials) {
            Inscription.logger.fine("Tracking: '" + material + "'");
            addTracked(material);
        }
        cleanPlaced();
    }
    //------------------------------------------------------------------------------------------------//

    @Deprecated public static BlockTracker load(File file) {
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

    @Deprecated public static boolean save(BlockTracker tracker, File file) {
        try {
            if (!file.exists())
                file.createNewFile();
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
