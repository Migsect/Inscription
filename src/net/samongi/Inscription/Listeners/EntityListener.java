package net.samongi.Inscription.Listeners;

import net.samongi.Inscription.Inscription;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;

public class EntityListener implements Listener
{

  @EventHandler
  public void onEntityDeath(EntityDeathEvent event)
  {
    Inscription.getInstance().getLootHandler().onEntityDeath(event);
    Inscription.getInstance().getExperienceManager().onEntityDeath(event);
  }
  
  @EventHandler
  public void onEntityDamageByEntity(EntityDamageByEntityEvent event)
  {
    if(event.isCancelled()) return;
    Inscription.getInstance().getExperienceManager().onEntityDamageEntity(event);
  }
}
