package net.samongi.Inscription.Player;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.UUID;

import net.samongi.Inscription.Inscription;

public class PlayerData implements Serializable
{
  public static void log(String message){Inscription.log("[PlayerData] " + message);}
  public static void logDebug(String message){if(Inscription.debug()) PlayerManager.log("[DEBUG] " + message);}
  public static boolean debug(){return Inscription.debug();}
  
  private static final long serialVersionUID = 3049177777841203611L;
  
  private transient DamageData damage_data = new DamageData();
  private GlyphInventory glyphs = new GlyphInventory();
  private HashMap<String, Integer> experience = new HashMap<>();
  
  private final UUID player_UUID;
  private String player_name = "NO NAME SET";
  
  /**Constructs a PlayerData object based off
   * the player object passed in.
   * 
   * @param player The player to make data for.
   */
  public PlayerData(UUID player_UUID)
  {
    this.player_UUID = player_UUID;
  }
  
  /**Returns the UUID of the player for this data
   * 
   * @return UUID of the player
   */
  public UUID getPlayerUUID(){return this.player_UUID;}
  /**Sets the player's name in this data
   * This should only be used when the player doesn't have a name set yet.
   * 
   * @param name The name to set
   */
  public void setPlayerName(String name){this.player_name = name;}
  /**Getst the name of the player for this data
   * 
   * @return Player name of this data's player
   */
  public String getPlayerName(){return this.player_name;}
  
  /**Returns the player's Glyph Inventory
   * The glyph inventory will be unique to this player.
   * It will have an accessible inventory object, but it should not be
   * expected that this inventory is always the same and is bound to be deleted.
   * 
   * @return
   */
  public GlyphInventory getGlyphInventory(){return this.glyphs;}
  /**Returns the player data which determines their extra damage dealt to creatures.
   * This is cached data and will be recalculated whenever needed.
   * This is generally cleared whenever the glyph inventory is saved.
   * 
   * @return Player's current damage data.
   */
  public DamageData getDamageData()
  {
    if(damage_data == null) 
    {
      this.damage_data = new DamageData();
      this.glyphs.cacheGlyphs(this);
    }
    return this.damage_data;
  }
  
  public void setExperience(String type, Integer amount){this.experience.put(player_name, amount);}
  public void addExperience(String type, Integer amount){this.experience.put(type, this.experience.get(type));}
  public int getExperience(String type){return this.experience.get(type);}
  
  /**Will attempt to load the specified player data file
   * If it fails to load the file for any reason it will
   * return null
   * 
   * @param dir The directory location to load the file from.
   * @return A player data file, null if it could not load
   */
  public static PlayerData load(File dir, UUID player_UUID)
  {
    File file = new File(dir, player_UUID.toString() + ".dat");
    if(!file.exists() || file.isDirectory()) 
    {
      PlayerData.logDebug("Data not found or is directory for file: " + file.getAbsolutePath());
      PlayerData.logDebug("  Returning new profile object for player: '" + player_UUID + "'");
      return new PlayerData(player_UUID);
    }
    PlayerData ret = null;
    try
    {
      FileInputStream file_in = new FileInputStream(file);
      ObjectInputStream obj_in = new ObjectInputStream(file_in);
      
      Object o = obj_in.readObject();
      ret = (PlayerData)o;
      
      obj_in.close();
      file_in.close();
    }
    catch(IOException e)
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
  /**Will attempt to save the specified player data file
   * If it fails to save the file for any reason it will
   * return false. Otherwise it will return true.
   * 
   * @param data The playerdata to save
   * @param dir The directory to save to
   * @return False if the save failed.
   */
  public static boolean save(PlayerData data, File dir)
  {
    if(!dir.isDirectory()) return false;
    File file = new File(dir, data.getPlayerUUID().toString() + ".dat");
    try
    {
      if(!file.exists())
      {
        PlayerData.logDebug("File does not yet exist, making file: " + file.getAbsolutePath());
        file.createNewFile();
      }
      FileOutputStream file_out = new FileOutputStream(file);
      ObjectOutputStream obj_out = new ObjectOutputStream(file_out);
      
      obj_out.writeObject(data);

      obj_out.close();
      file_out.close();
    }
    catch(IOException e)
    {
      e.printStackTrace();
      return false;
    }
    return true;
  }
}
