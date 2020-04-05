package net.samongi.Inscription.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;

import net.samongi.Inscription.Experience.ExperienceMap;
import net.samongi.Inscription.Inscription;
import net.samongi.Inscription.Waypoints.Waypoint;
import net.samongi.SamongiLib.Configuration.ConfigFile;
import net.samongi.SamongiLib.Exceptions.InvalidConfigurationException;

import net.samongi.SamongiLib.Tuple.Tuple;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.craftbukkit.libs.jline.internal.Nullable;
import org.bukkit.entity.Player;

import javax.annotation.Nonnull;

public class PlayerData {

    //--------------------------------------------------------------------------------------------------------------------//
    private Map<String, CacheData> m_cachedData = null;
    private GlyphInventory m_glyphs = null;
    private List<Location> m_waypoints = new ArrayList<>();
    private Set<Tuple> m_exploredChunks = new HashSet<>();

    /**
     * Tracks the amount of excess experience the player has.
     */
    private ExperienceMap m_experience = new ExperienceMap();

    private final UUID player_UUID;
    private String player_name = "NO NAME SET";

    //--------------------------------------------------------------------------------------------------------------------//
    /**
     * Constructs a PlayerData object based off
     * the player object passed in.
     *
     * @param player_UUID The player to make data for.
     */
    public PlayerData(UUID player_UUID) {
        this.player_UUID = player_UUID;
        this.player_name = Bukkit.getPlayer(player_UUID).getName();
        this.m_glyphs = new GlyphInventory(Bukkit.getPlayer(player_UUID));
    }

    public PlayerData(ConfigFile file) throws InvalidConfigurationException {
        ConfigurationSection section = file.getConfig();

        /* Grabbing the UUID */
        String uuid = section.getString("uuid");
        if (uuid == null) {
            throw new InvalidConfigurationException("UUID was not defined.");
        }
        try {
            this.player_UUID = UUID.fromString(uuid);
        }
        catch (IllegalArgumentException e) {
            throw new InvalidConfigurationException("UUID is not a valid UUID.");
        }

        ConfigurationSection storedExperienceSection = section.getConfigurationSection("stored_experience");
        if (storedExperienceSection != null) {
            deserializeExperienceSection(storedExperienceSection);
        }

        ConfigurationSection waypointSection = section.getConfigurationSection("waypoints");
        if (waypointSection != null) {
            deserailizeWaypointLocations(waypointSection);
        }

        ConfigurationSection exploredChunksSection = section.getConfigurationSection("explored-chunks");
        if (exploredChunksSection != null) {
            deserailizeExploredChunks(exploredChunksSection);
        }


        /* Grabbing the player's name */
        this.player_name = Bukkit.getPlayer(player_UUID).getName();

        /* Setting up the glyph Inventory */
        ConfigurationSection glyphsSection = section.getConfigurationSection("glyphs");
        this.m_glyphs = new GlyphInventory(Bukkit.getPlayer(player_UUID), glyphsSection);

    }
    /**
     * Returns the UUID of the player for this data
     *
     * @return UUID of the player
     */
    public UUID getPlayerUUID() {
        return this.player_UUID;
    }

    /**
     * Sets the player's name in this data
     * This should only be used when the player doesn't have a name set yet.
     *
     * @param name The name to set
     */
    public void setPlayerName(String name) {
        this.player_name = name;
    }

    /**
     * Getst the name of the player for this data
     *
     * @return Player name of this data's player
     */
    public String getPlayerName() {
        return this.player_name;
    }

    // ---------------------------------------------------------------------------------------------------------------//

    /**
     * Returns the player's Glyph Inventory
     * The glyph inventory will be unique to this player.
     * It will have an accessible inventory object, but it should not be
     * expected that this inventory is always the same and is bound to be deleted.
     *
     * @return
     */
    public GlyphInventory getGlyphInventory() {
        return this.m_glyphs;
    }

    public @Nullable InscriptionRootMenu getRootMenu(Player viewer) {
        return new InscriptionRootMenu(viewer, this);
    }

    // ---------------------------------------------------------------------------------------------------------------//

    private Map<String, CacheData> getCachedData() {
        if (this.m_cachedData == null) {
            this.m_cachedData = new HashMap<>();
        }
        return this.m_cachedData;
    }

    /**
     * Will call all the "clear()" methods on each entry of the cached data
     * This will not completely reset the data but call the implementation that
     * the data
     * species and as such is different from resetting the cached data
     */
    public void clearCachedData() {
        for (CacheData d : this.getCachedData().values())
            d.clear();
    }

    /**
     * Will completely clear all the cached data that is saved and will
     * remove any entries entirely. (Resets the hashmap of data to be empty)
     */
    public void resetCachedData() {
        this.m_cachedData = new HashMap<>();
    }

    public void setData(CacheData data) {
        this.getCachedData().put(data.getType().toUpperCase(), data);
    }

    public CacheData getData(String type) {
        return this.getCachedData().get(type.toUpperCase());
    }

    public boolean hasData(String type) {
        return this.getCachedData().containsKey(type.toUpperCase());
    }

    // ---------------------------------------------------------------------------------------------------------------//

    public void setExperience(String type, int amount) {
        m_experience.set(type, amount);
    }

    public void addExperience(String type, int amount) {
        m_experience.addInplace(type, amount);
    }

    public void addExperience(ExperienceMap experience)
    {
        m_experience.addInplace(experience);
    }

    @Deprecated public int getExperience_LEGACY(String type) {

        return m_experience.get(type);
    }

    @Deprecated public Map<String, Integer> getExperience_LEGACY() {
        return m_experience.get();
    }
    public @Nonnull ExperienceMap getExperience() {
        return m_experience.clone();
    }

    // ---------------------------------------------------------------------------------------------------------------//

    public List<Location> getWaypoints() {
        return m_waypoints;
    }

    public List<Location> getWaypointsSorted(Location fromLocation) {
        List<Location> locations = new ArrayList<>(m_waypoints);
        locations.sort((Location a, Location b) -> {
            Waypoint waypointA = new Waypoint(a);
            Waypoint waypointB = new Waypoint(b);
            return waypointA.getDistance(fromLocation) - waypointB.getDistance(fromLocation);
        });
        return locations;
    }

    /**
     * Adds a waypoint to the player's list of waypoints. If the player already has 27 waypoints, then this will pop
     * the oldest waypoint that the player has.
     *
     * @param location The location to add as a waypoint.
     */
    public boolean addWaypoint(Location location) {
        if (m_waypoints.size() >= 27) {
            return false;
        }
        if (m_waypoints.contains(location)) {
            return false;
        }
        m_waypoints.add(location);
        return true;
    }

    public boolean removeWaypoint(Location location) {
        if (!m_waypoints.contains(location)) {
            return false;
        }
        m_waypoints.remove(location);
        return true;
    }

    public void addExploredChunk(Chunk chunk) {
        Tuple chunkCoords = new Tuple(chunk.getWorld().getUID(), chunk.getX(), chunk.getZ());
        m_exploredChunks.add(chunkCoords);
    }
    public boolean isChunkExplored(Chunk chunk) {
        Tuple chunkCoords = new Tuple(chunk.getWorld().getUID(), chunk.getX(), chunk.getZ());
        return m_exploredChunks.contains(chunkCoords);
    }
    //--------------------------------------------------------------------------------------------------------------------//
    /**
     * Will attempt to load the specified player data file
     * If it fails to load the file for any reason it will
     * return null
     *
     * @param dir The directory location to load the file from.
     * @return A player data file, null if it could not load
     */
    public static PlayerData load(File dir, UUID player_UUID) {
        File file = new File(dir, player_UUID.toString() + ".yml");
        if (!file.exists() || file.isDirectory()) {
            Inscription.logger.fine("Data not found or is directory for file: " + file.getAbsolutePath());
            Inscription.logger.fine("  Returning new profile object for player: '" + player_UUID + "'");
            return new PlayerData(player_UUID);
        }
        ConfigFile configFile = new ConfigFile(file);
        PlayerData playerData = null;
        try {
            playerData = new PlayerData(configFile);
        }
        catch (InvalidConfigurationException e) {
            return new PlayerData(player_UUID);
        }

        return playerData;
    }

    /**
     * Will attempt to save the specified player data file
     * If it fails to save the file for any reason it will
     * return false. Otherwise it will return true.
     *
     * @param data The playerdata to save
     * @param dir  The directory to save to
     * @return False if the save failed.
     */
    public static boolean save(PlayerData data, File dir) {
        if (!dir.isDirectory()) {
            return false;
        }
        File file = new File(dir, data.getPlayerUUID().toString() + ".yml");

        try {
            if (!file.exists()) {
                Inscription.logger.fine("File does not yet exist, making file: " + file.getAbsolutePath());
                file.createNewFile();
            }
        }
        catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        /* Saving to the config file */
        ConfigFile configFile = new ConfigFile(file);
        ConfigurationSection section = configFile.getConfig();
        section.set("uuid", data.player_UUID.toString());
        section.set("glyphs", data.m_glyphs.getAsConfigurationSection());
        section.set("stored_experience", data.serializeExperienceSection());
        section.set("waypoints", data.serailizeWaypointLocations());
        section.set("explored-chunks", data.serailizeExploredChunks());

        configFile.saveConfig();

        return true;
    }

    //--------------------------------------------------------------------------------------------------------------------//
    public ConfigurationSection serializeExperienceSection() {
        ConfigurationSection section = new YamlConfiguration();
        for (String key : m_experience.experienceTypes()) {
            Integer value = m_experience.get(key);
            section.set(key, value);
        }
        return section;
    }

    public void deserializeExperienceSection(ConfigurationSection section) {

        for (String key : section.getKeys(false)) {
            int value = section.getInt(key);
            m_experience.set(key, value);
        }
    }
    //--------------------------------------------------------------------------------------------------------------------//
    public ConfigurationSection serailizeWaypointLocations() {
        ConfigurationSection section = new YamlConfiguration();
        int index = 0;
        for (Location location : m_waypoints) {
            section.set("" + index, location.serialize());
            index++;
        }
        return section;
    }

    public void deserailizeWaypointLocations(ConfigurationSection section) {
        m_waypoints.clear();
        for (String key : section.getKeys(false)) {
            Map<String, Object> locationMap = section.getConfigurationSection(key).getValues(false);
            Location location = Location.deserialize(locationMap);
            m_waypoints.add(location);
        }
    }
    //--------------------------------------------------------------------------------------------------------------------//
    public ConfigurationSection serailizeExploredChunks() {
        ConfigurationSection section = new YamlConfiguration();
        int index = 0;
        for (Tuple chunkCoordinate : m_exploredChunks) {
            UUID worldId = chunkCoordinate.get(0);
            int x = chunkCoordinate.get(1);
            int z = chunkCoordinate.get(2);

            ConfigurationSection subSection = new YamlConfiguration();
            subSection.set("world", worldId.toString());
            subSection.set("x", x);
            subSection.set("z", z);

            section.set("" + index, subSection);
            index++;
        }
        return section;
    }
    public void deserailizeExploredChunks(ConfigurationSection section) {
        m_exploredChunks.clear();
        for (String key : section.getKeys(false)) {

            ConfigurationSection subSection = section.getConfigurationSection(key);

            if (!subSection.contains("world") || !subSection.contains("x") || !subSection.contains("z")) {
                Inscription.logger.warning("Player " + player_UUID + " had invalid explored coord at " + key);
                continue;
            }
            String worldString = subSection.getString("world");
            int x = subSection.getInt("x");
            int z = subSection.getInt("z");
            UUID worldID = UUID.fromString(worldString);

            Tuple chunkCoordinate = new Tuple(worldID, x, z);
            m_exploredChunks.add(chunkCoordinate);

        }
    }
    //--------------------------------------------------------------------------------------------------------------------//
}
