package net.samongi.Inscription.Listeners;

import net.samongi.Inscription.Inscription;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

public class BlockListener implements Listener
{
  
  @EventHandler
  public void onBlockBreak(BlockBreakEvent event)
  {
    if(event.isCancelled()) return;
    Inscription.getInstance().getLootHandler().onBlockBreak(event);
    Inscription.getInstance().getExperienceManager().onBlockBreak(event);
  }
}
