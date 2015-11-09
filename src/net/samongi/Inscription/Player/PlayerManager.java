package net.samongi.Inscription.Player;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.samongi.Inscription.Inscription;

import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerManager
{
  public static void log(String message){Inscription.log("[PlayerManager] " + message);}
  public static void logDebug(String message){if(Inscription.debug()) PlayerManager.log(Inscription.debug_tag + message);}
  public static boolean debug(){return Inscription.debug();}
  
  private Map<UUID, PlayerData> players = new HashMap<>();
  private final File data_location;
  
  public PlayerManager(File data_location)
  {
    this.data_location = data_location;
  }
  
  public void loadPlayer(Player player){this.loadPlayer(player.getUniqueId());}
  public void loadPlayer(UUID player_UUID)
  {
    PlayerData data = PlayerData.load(data_location, player_UUID);
    if(data == null) return;
    this.players.put(player_UUID, data);
  }
  public void unloadPlayer(Player player){this.unloadPlayer(player.getUniqueId());}
  public void unloadPlayer(UUID player_UUID)
  {
    PlayerData data = this.getData(player_UUID);
    if(data == null) return;
    PlayerData.save(data, data_location);
    this.players.remove(player_UUID);
  }
  
  public PlayerData getData(Player player){return this.getData(player.getUniqueId());}
  public PlayerData getData(UUID player_UUID){return this.players.get(player_UUID);}
  
  public void onPlayerJoin(PlayerJoinEvent event)
  {
    Player player = event.getPlayer();
    this.loadPlayer(player);;
  }
  public void onPlayerQuit(PlayerQuitEvent event)
  {
    Player player = event.getPlayer();
    this.unloadPlayer(player);
  }
}
