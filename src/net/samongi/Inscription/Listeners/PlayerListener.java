package net.samongi.Inscription.Listeners;

import net.samongi.Inscription.Inscription;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener
{
  @EventHandler
  public void onPlayerJoin(PlayerJoinEvent event)
  {
    Inscription.getInstance().getPlayerManager().onPlayerJoin(event);
  }
  
  @EventHandler
  public void onPlayerQuit(PlayerQuitEvent event)
  {
    Inscription.getInstance().getPlayerManager().onPlayerQuit(event); 
  }
  
}
