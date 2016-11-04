package net.samongi.Inscription.Player;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.samongi.Inscription.Inscription;

import org.bukkit.Bukkit;

public class PlayerData implements Serializable
{

  private static final long serialVersionUID = 3049177777841203611L;

  private transient Map<String, CacheData> cached_data = null;
  private GlyphInventory glyphs = null;
  /**
   * This is no longer used for its original purpose
   * It will be repurposed as data for a player.
   */
  private HashMap<String, Integer> experience = new HashMap<>();

  private final UUID player_UUID;
  private String player_name = "NO NAME SET";

  /**
   * Constructs a PlayerData object based off
   * the player object passed in.
   * 
   * @param player
   *          The player to make data for.
   */
  public PlayerData(UUID player_UUID)
  {
    this.player_UUID = player_UUID;
    this.player_name = Bukkit.getPlayer(player_UUID).getName();
    this.glyphs = new GlyphInventory(player_UUID);
  }

  /**
   * Returns the UUID of the player for this data
   * 
   * @return UUID of the player
   */
  public UUID getPlayerUUID()
  {
    return this.player_UUID;
  }

  /**
   * Sets the player's name in this data
   * This should only be used when the player doesn't have a name set yet.
   * 
   * @param name
   *          The name to set
   */
  public void setPlayerName(String name)
  {
    this.player_name = name;
  }

  /**
   * Getst the name of the player for this data
   * 
   * @return Player name of this data's player
   */
  public String getPlayerName()
  {
    return this.player_name;
  }

  /**
   * Returns the player's Glyph Inventory
   * The glyph inventory will be unique to this player.
   * It will have an accessible inventory object, but it should not be
   * expected that this inventory is always the same and is bound to be deleted.
   * 
   * @return
   */
  public GlyphInventory getGlyphInventory()
  {
    return this.glyphs;
  }

  private Map<String, CacheData> getCachedData()
  {
    if (this.cached_data == null) this.cached_data = new HashMap<>();
    return this.cached_data;
  }

  /**
   * Will call all the "clear()" methods on each entry of the cached data
   * This will not completely reset the data but call the implementation that
   * the data
   * species and as such is different from resetting the cached data
   */
  public void clearCachedData()
  {
    for (CacheData d : this.getCachedData().values())
      d.clear();
  }

  /**
   * Will completely clear all the cached data that is saved and will
   * remove any entries entirely. (Resets the hashmap of data to be empty)
   */
  public void resetCachedData()
  {
    this.cached_data = new HashMap<>();
  }

  public void setData(CacheData data)
  {
    this.getCachedData().put(data.getType().toUpperCase(), data);
  }

  public CacheData getData(String type)
  {
    return this.getCachedData().get(type.toUpperCase());
  }

  public boolean hasData(String type)
  {
    return this.getCachedData().containsKey(type.toUpperCase());
  }

  public void setExperience(String type, int amount)
  {
    this.experience.put(type, amount);
  }

  public void addExperience(String type, int amount)
  {
    if (!this.experience.containsKey(type)) this.experience.put(type, 0);
    this.experience.put(type, this.experience.get(type) + amount);
  }

  public int getExperience(String type)
  {
    return this.experience.get(type);
  }

  public Map<String, Integer> getExperience()
  {
    return this.experience;
  }

  /**
   * Will attempt to load the specified player data file
   * If it fails to load the file for any reason it will
   * return null
   * 
   * @param dir
   *          The directory location to load the file from.
   * @return A player data file, null if it could not load
   */
  public static PlayerData load(File dir, UUID player_UUID)
  {
    File file = new File(dir, player_UUID.toString() + ".dat");
    if (!file.exists() || file.isDirectory())
    {
      Inscription.logger.fine("Data not found or is directory for file: " + file.getAbsolutePath());
      Inscription.logger.fine("  Returning new profile object for player: '" + player_UUID + "'");
      return new PlayerData(player_UUID);
    }
    PlayerData ret = null;
    try
    {
      FileInputStream file_in = new FileInputStream(file);
      ObjectInputStream obj_in = new ObjectInputStream(file_in);

      Object o = obj_in.readObject();
      ret = (PlayerData) o;

      obj_in.close();
      file_in.close();
    }
    catch (IOException e)
    {
      e.printStackTrace();
      return new PlayerData(player_UUID);
    }
    catch (ClassNotFoundException e)
    {
      e.printStackTrace();
      return new PlayerData(player_UUID);
    }
    return ret;
  }
  /**
   * Will attempt to save the specified player data file
   * If it fails to save the file for any reason it will
   * return false. Otherwise it will return true.
   * 
   * @param data
   *          The playerdata to save
   * @param dir
   *          The directory to save to
   * @return False if the save failed.
   */
  public static boolean save(PlayerData data, File dir)
  {
    if (!dir.isDirectory()) return false;
    File file = new File(dir, data.getPlayerUUID().toString() + ".dat");
    try
    {
      if (!file.exists())
      {
        Inscription.logger.fine("File does not yet exist, making file: " + file.getAbsolutePath());
        file.createNewFile();
      }
      FileOutputStream file_out = new FileOutputStream(file);
      ObjectOutputStream obj_out = new ObjectOutputStream(file_out);

      obj_out.writeObject(data);

      obj_out.close();
      file_out.close();
    }
    catch (IOException e)
    {
      e.printStackTrace();
      return false;
    }
    return true;
  }
}
